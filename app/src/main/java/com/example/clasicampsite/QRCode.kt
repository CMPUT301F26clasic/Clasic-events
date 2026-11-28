/**
 * Purpose: Generates a QR code that contains a deep link to a campsite's details page.
 * The QR code allows users to open the campsite in the ClasiCampsite app (US 02.02.01).
 * Outstanding issues: The app must be installed and able to handle the campsite deep link
 * for the QR code to open the details page directly.
 */
package com.example.clasicampsite

import android.graphics.Bitmap
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/** QR code helpers for campsite detail links. */
object QRCode {
    /** Deep link handled by the app's campsite detail navigation destination. */
    fun campsiteUri(campsiteId: String): String =
        Uri.Builder()
            .scheme("clasicampsite")
            .authority("campsite")
            .appendPath(campsiteId)
            .build()
            .toString()

    /** Encodes [content] as a black-and-white QR bitmap. */
    fun generate(content: String, size: Int = 512): Bitmap {
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x,
                    y,
                    if (matrix.get(x, y)) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                )
            }
        }

        return bitmap
    }
}
