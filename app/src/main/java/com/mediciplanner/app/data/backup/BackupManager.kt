package com.mediciplanner.app.data.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.mediciplanner.app.data.local.MediciDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * BackupManager handles creating an exportable .zip archive containing:
 * 1. The Room SQLite database (medici.db)
 * 2. Captured prescription and medicine photos (medicine_images/)
 *
 * It uses MediaStore for modern scoped storage on Android 10+ (Q+) to save
 * directly to the user's Downloads folder, and supports restoring from a selected .zip.
 */
object BackupManager {

    private const val TAG = "BackupManager"
    const val BACKUP_DB_ENTRY_NAME = "medici.db"
    const val IMAGES_DIR_NAME = "medicine_images"

    /**
     * Creates a zip backup containing the database and images directory,
     * saving it to the user's Downloads folder.
     */
    fun backupData(context: Context): Result<Uri> {
        return try {
            // 1. Close the Room Database so SQLite flushes WAL/SHM to the main DB file
            MediciDatabase.closeDatabase()

            // 2. Identify database file
            var dbFile = context.getDatabasePath(MediciDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                val fallbackDb = context.getDatabasePath("medici_planner_db")
                if (fallbackDb.exists()) {
                    dbFile = fallbackDb
                }
            }

            // 3. Identify images folder
            val imagesDir = File(context.filesDir, IMAGES_DIR_NAME)

            // 4. Create a temporary zip file in cache
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "medici_backup_$timeStamp.zip"
            val tempZipFile = File(context.cacheDir, "temp_backup_$timeStamp.zip")

            FileOutputStream(tempZipFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // Add database file if present
                    if (dbFile.exists()) {
                        val dbEntry = ZipEntry(BACKUP_DB_ENTRY_NAME)
                        zos.putNextEntry(dbEntry)
                        FileInputStream(dbFile).use { fis ->
                            fis.copyTo(zos)
                        }
                        zos.closeEntry()
                    }

                    // Add images if directory exists
                    if (imagesDir.exists() && imagesDir.isDirectory) {
                        val images = imagesDir.listFiles()
                        if (images != null) {
                            for (image in images) {
                                if (image.isFile) {
                                    val entryPath = "$IMAGES_DIR_NAME/${image.name}"
                                    val imgEntry = ZipEntry(entryPath)
                                    zos.putNextEntry(imgEntry)
                                    FileInputStream(image).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                }
                            }
                        }
                    }
                }
            }

            // 5. Save zip file to Downloads folder using MediaStore on Android 10+
            val savedUri = saveZipToDownloads(context, tempZipFile, fileName)

            // Delete temporary cache file
            tempZipFile.delete()

            Result.success(savedUri)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup zip", e)
            Result.failure(e)
        }
    }

    /**
     * Saves a temporary zip file to the public Downloads folder.
     */
    private fun saveZipToDownloads(context: Context, sourceFile: File, fileName: String): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Failed to create MediaStore entry in Downloads")

            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw IOException("Failed to open output stream for MediaStore Uri")

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                return uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            // Android 9 and lower or testing fallback
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val destination = File(downloadsDir, fileName)
            sourceFile.copyTo(destination, overwrite = true)
            return Uri.fromFile(destination)
        }
    }

    /**
     * Restores data from the specified zip Uri:
     * 1. Closes Room database
     * 2. Extracts zip to temporary directory
     * 3. Replaces medici.db
     * 4. Replaces medicine_images folder
     */
    fun restoreData(context: Context, zipUri: Uri): Result<Unit> {
        return try {
            // 1. Close existing database
            MediciDatabase.closeDatabase()

            val tempExtractDir = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}")
            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
            }
            tempExtractDir.mkdirs()

            // 2. Unzip files to temporary directory
            val inputStream = context.contentResolver.openInputStream(zipUri)
                ?: throw IOException("Could not open input stream for selected backup file")

            unzip(inputStream, tempExtractDir)

            // 3. Locate the database file inside the extracted files
            val extractedDb = File(tempExtractDir, BACKUP_DB_ENTRY_NAME).takeIf { it.exists() }
                ?: File(tempExtractDir, "medici_planner_db").takeIf { it.exists() }
                ?: tempExtractDir.walkTopDown().firstOrNull { it.isFile && (it.extension == "db" || it.name.endsWith(".db")) }
                ?: throw IllegalStateException("No database file (medici.db) found in the backup archive")

            // 4. Replace target database file
            val targetDb = context.getDatabasePath(MediciDatabase.DATABASE_NAME)
            targetDb.parentFile?.mkdirs()

            // Clean up old sqlite auxiliary files
            File(targetDb.path + "-wal").delete()
            File(targetDb.path + "-shm").delete()
            File(targetDb.path + "-journal").delete()

            val oldDb = context.getDatabasePath("medici_planner_db")
            if (oldDb.exists()) {
                oldDb.delete()
                File(oldDb.path + "-wal").delete()
                File(oldDb.path + "-shm").delete()
            }

            extractedDb.copyTo(targetDb, overwrite = true)

            // 5. Replace medicine images
            val extractedImagesDir = File(tempExtractDir, IMAGES_DIR_NAME)
            val targetImagesDir = File(context.filesDir, IMAGES_DIR_NAME)
            if (!targetImagesDir.exists()) {
                targetImagesDir.mkdirs()
            }

            if (extractedImagesDir.exists() && extractedImagesDir.isDirectory) {
                extractedImagesDir.listFiles()?.forEach { image ->
                    if (image.isFile) {
                        val destFile = File(targetImagesDir, image.name)
                        image.copyTo(destFile, overwrite = true)
                    }
                }
            }

            // 6. Clean up temporary directory
            tempExtractDir.deleteRecursively()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            Result.failure(e)
        }
    }

    /**
     * Extracts a zip input stream safely with Zip Slip vulnerability check.
     */
    fun unzip(zipStream: InputStream, targetDir: File) {
        ZipInputStream(zipStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)

                // Prevent Zip Slip vulnerability
                val canonicalDestination = targetDir.canonicalPath
                val canonicalNewFile = newFile.canonicalPath
                if (!canonicalNewFile.startsWith(canonicalDestination + File.separator) && canonicalNewFile != canonicalDestination) {
                    throw SecurityException("Zip entry is outside of target directory: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /**
     * Restarts the application cleanly after data restoration.
     */
    fun restartApp(context: Context, killProcess: Boolean = true) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (intent != null) {
            context.startActivity(intent)
        }

        if (killProcess) {
            val isTest = try {
                Class.forName("org.robolectric.Robolectric") != null
            } catch (e: ClassNotFoundException) {
                false
            }
            if (!isTest) {
                Runtime.getRuntime().exit(0)
            }
        }
    }
}
