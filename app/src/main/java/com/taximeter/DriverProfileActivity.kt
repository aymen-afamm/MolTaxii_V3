package com.taximeter.app

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.android.synthetic.main.activity_driver_profile.*

class DriverProfileActivity : AppCompatActivity() {

    private var driverName = ""
    private var driverAge = 0
    private var driverLicense = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_profile)

        driverName = intent.getStringExtra("DRIVER_NAME") ?: "N/A"
        driverAge = intent.getIntExtra("DRIVER_AGE", 0)
        driverLicense = intent.getStringExtra("DRIVER_LICENSE") ?: "N/A"

        setupUI()
        generateQRCode()

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupUI() {
        tvDriverName.text = driverName
        tvDriverAge.text = "$driverAge ${getString(R.string.years_old)}"
        tvDriverLicense.text = driverLicense
        val initial = if (driverName.isNotEmpty()) driverName[0].toString() else "?"
        tvDriverInitial.text = initial
    }

    private fun generateQRCode() {
        try {
            val qrContent = """
                TAXI DRIVER INFO
                Name: $driverName
                Age: $driverAge
                License: $driverLicense
                Contact: +212 XXX-XXXXXX
            """.trimIndent()

            val size = 512
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, size, size)

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            ivQRCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
