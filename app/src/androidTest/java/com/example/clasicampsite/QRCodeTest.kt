/*
 * Purpose: Tests that campsite QR codes contain a unique deep link to the campsite details.
 * User story:
 * US 02.02.01 As a campsite owner, I want the system to generate a unique QR code that links to the campsite details.
 *
 * Outstanding issues: These are instrumented tests because QR generation uses Android Bitmap.
 */

package com.example.clasicampsite

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.RGBLuminanceSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the campsite QR code helper on an Android device or emulator. */
@RunWith(AndroidJUnit4::class)
class QRCodeTest {

    /** US 02.02.01: a campsite ID produces a deep link to that campsite's details. */
    @Test
    fun campsiteUriContainsCampsiteId() {
        val uri = QRCode.campsiteUri("camp_123")

        assertEquals("clasicampsite://campsite/camp_123", uri)
    }

    /** US 02.02.01: different campsite IDs produce different QR destinations. */
    @Test
    fun differentCampsitesHaveUniqueQrDestinations() {
        val first = QRCode.campsiteUri("camp_123")
        val second = QRCode.campsiteUri("camp_456")

        assertNotEquals(first, second)
    }

    /** US 02.02.01: the generated QR bitmap can be decoded to its campsite details link. */
    @Test
    fun generatedQrCodeDecodesToCampsiteDetailsLink() {
        val expected = QRCode.campsiteUri("camp_123")
        val bitmap = QRCode.generate(expected, 512)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val decoded = QRCodeReader().decode(binaryBitmap)

        assertEquals(expected, decoded.text)
    }
}
