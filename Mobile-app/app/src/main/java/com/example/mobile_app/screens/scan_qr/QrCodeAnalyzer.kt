package com.example.mobile_app.screens.scan_qr

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

// This class acts as the bridge between CameraX and ML Kit
class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE) // We only care about QR Codes
            .build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Convert CameraX image to ML Kit InputImage
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Check if we found any barcodes
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrEmpty()) {
                            // We found a code! Send it back to the UI/ViewModel
                            onQrCodeScanned(rawValue)
                        }
                    }
                }
                .addOnFailureListener {
                    // Handle failure if needed
                }
                .addOnCompleteListener {
                    // CRITICAL: We must close the image proxy to receive the next frame
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}