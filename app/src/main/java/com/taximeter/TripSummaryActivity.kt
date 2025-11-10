package com.taximeter.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_trip_summary.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class TripSummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_summary)

        val distance = intent.getDoubleExtra("DISTANCE", 0.0)
        val duration = intent.getDoubleExtra("DURATION", 0.0)
        val fare = intent.getDoubleExtra("FARE", 0.0)
        val baseFare = intent.getDoubleExtra("BASE_FARE", 0.0)
        val distanceFare = intent.getDoubleExtra("DISTANCE_FARE", 0.0)
        val timeFare = intent.getDoubleExtra("TIME_FARE", 0.0)

        displayTripSummary(distance, duration, fare, baseFare, distanceFare, timeFare)

        btnClose.setOnClickListener {
            finish()
        }

        btnShareReceipt.setOnClickListener {
            shareReceipt(distance, duration, fare)
        }
    }

    private fun displayTripSummary(
        distance: Double,
        duration: Double,
        fare: Double,
        baseFare: Double,
        distanceFare: Double,
        timeFare: Double
    ) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        tvTripDate.text = currentDate
        tvTripDistance.text = String.format("%.2f km", distance)
        tvTripDuration.text = "${duration.toInt()} min"
        tvBaseFare.text = String.format("%.2f DH", baseFare)
        tvDistanceFare.text = String.format("%.2f DH", distanceFare)
        tvTimeFare.text = String.format("%.2f DH", timeFare)
        tvTotalFare.text = String.format("%.2f DH", fare)
    }

    private fun shareReceipt(distance: Double, duration: Double, fare: Double) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val receiptText = """
            🚕 REÇU DE TAXI
            
            Date: $currentDate
            Distance: ${String.format("%.2f", distance)} km
            Durée: ${duration.toInt()} min
            Tarif Total: ${String.format("%.2f", fare)} DH
            
            Merci d'avoir choisi notre service!
        """.trimIndent()

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, receiptText)
            type = "text/plain"
        }
        startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share_receipt)))
    }
}