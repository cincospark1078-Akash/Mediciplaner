package com.mediciplanner.app.core.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

open class OcrHelper(private val context: Context) {

    open fun recognizeText(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    onSuccess(visionText.text)
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
                .addOnCompleteListener {
                    recognizer.close()
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
