package com.mediciplanner.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mediciplanner.app.data.local.converter.Converters
import com.mediciplanner.app.data.local.converter.ListConverter
import com.mediciplanner.app.data.local.dao.FollowUpDao
import com.mediciplanner.app.data.local.dao.PrescriptionDao
import com.mediciplanner.app.data.local.dao.ReminderDao
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity

@Database(
    entities = [
        PrescriptionEntity::class,
        MedicineEntity::class,
        DoseScheduleEntity::class,
        ReminderEntity::class,
        FollowUpEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class, ListConverter::class)
abstract class MediciDatabase : RoomDatabase() {

    abstract fun prescriptionDao(): PrescriptionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun followUpDao(): FollowUpDao

    companion object {
        const val DATABASE_NAME = "medici.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN imagePath TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE prescriptions ADD COLUMN parentId TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE prescriptions SET parentId = id WHERE parentId = ''")
                db.execSQL("ALTER TABLE prescriptions ADD COLUMN versionNumber INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE prescriptions ADD COLUMN isLatest INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE follow_ups ADD COLUMN location TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE follow_ups ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE follow_ups ADD COLUMN reminderTime TEXT NOT NULL DEFAULT '09:00'")
                db.execSQL("ALTER TABLE follow_ups ADD COLUMN completedAt TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE prescriptions DROP COLUMN prescriptionImagePath")
                } catch (e: Exception) {
                    // Ignored if column did not exist
                }
                db.execSQL("ALTER TABLE prescriptions ADD COLUMN prescriptionImagePaths TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE follow_ups ADD COLUMN documentPaths TEXT NOT NULL DEFAULT '[]'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN lineageId TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE medicines SET lineageId = id WHERE lineageId = ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE dose_schedules ADD COLUMN foodRule TEXT NOT NULL DEFAULT 'AFTER'")
                db.execSQL(
                    """
                    UPDATE dose_schedules
                    SET foodRule = COALESCE(
                        foodRuleOverride,
                        (SELECT m.foodRule FROM medicines m WHERE m.id = dose_schedules.medicineId),
                        'AFTER'
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN stockCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN initialStock INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: MediciDatabase? = null

        fun getDatabase(context: Context): MediciDatabase {
            return INSTANCE ?: synchronized(this) {
                val oldDb = context.getDatabasePath("medici_planner_db")
                val targetDb = context.getDatabasePath(DATABASE_NAME)
                if (oldDb.exists() && !targetDb.exists()) {
                    try {
                        oldDb.copyTo(targetDb, overwrite = true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediciDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10
                )
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun closeDatabase() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                INSTANCE = null
            }
        }
    }
}
