package com.taximeter.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.taximeter.MainActivity
import com.taximeter.app.R
import com.taximeter.TripSummaryActivity
import kotlinx.android.synthetic.main.fragment_meter.*
import kotlin.math.roundToInt

class MeterFragment : Fragment() {

    private lateinit var mainActivity: MainActivity
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (mainActivity.isRideStarted && !mainActivity.isPaused) {
                updateUI()
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_meter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity
        setupClickListeners()
        updateUI()
    }

    private fun setupClickListeners() {
        btnStart.setOnClickListener {
            if (!mainActivity.isRideStarted) {
                startRide()
            } else if (mainActivity.isPaused) {
                resumeRide()
            } else {
                pauseRide()
            }
        }

        btnStop.setOnClickListener {
            if (mainActivity.isRideStarted) {
                stopRide()
            }
        }

        btnReset.setOnClickListener {
            resetMeter()
        }
    }

    private fun startRide() {
        mainActivity.isRideStarted = true
        mainActivity.isPaused = false
        mainActivity.tripStartTime = System.currentTimeMillis()
        mainActivity.totalDistance = 0.0
        mainActivity.totalPausedDuration = 0L
        mainActivity.lastLocation = mainActivity.currentLocation

        btnStart.text = getString(R.string.pause)
        btnStop.isEnabled = true
        handler.post(updateRunnable)
        Toast.makeText(requireContext(), getString(R.string.ride_started), Toast.LENGTH_SHORT).show()
    }

    private fun pauseRide() {
        mainActivity.isPaused = true
        mainActivity.pausedTime = System.currentTimeMillis()
        btnStart.text = getString(R.string.resume)
        Toast.makeText(requireContext(), getString(R.string.ride_paused), Toast.LENGTH_SHORT).show()
    }

    private fun resumeRide() {
        mainActivity.isPaused = false
        mainActivity.totalPausedDuration += System.currentTimeMillis() - mainActivity.pausedTime
        btnStart.text = getString(R.string.pause)
        handler.post(updateRunnable)
        Toast.makeText(requireContext(), getString(R.string.ride_resumed), Toast.LENGTH_SHORT).show()
    }

    private fun stopRide() {
        mainActivity.isRideStarted = false
        mainActivity.isPaused = false
        handler.removeCallbacks(updateRunnable)

        val totalFare = mainActivity.calculateTotalFare()
        val distanceKm = mainActivity.totalDistance / 1000.0
        val durationMinutes = mainActivity.getElapsedMinutes()

        showTripSummary(distanceKm, durationMinutes, totalFare)
        btnStart.text = getString(R.string.start)
        btnStop.isEnabled = false
        Toast.makeText(requireContext(), getString(R.string.ride_completed), Toast.LENGTH_SHORT).show()
    }

    private fun resetMeter() {
        if (!mainActivity.isRideStarted) {
            mainActivity.totalDistance = 0.0
            mainActivity.tripStartTime = 0L
            mainActivity.pausedTime = 0L
            mainActivity.totalPausedDuration = 0L
            mainActivity.lastLocation = null
            updateUI()
            Toast.makeText(requireContext(), getString(R.string.meter_reset), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), getString(R.string.cannot_reset_during_ride), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        if (!isAdded) return

        val distanceKm = mainActivity.totalDistance / 1000.0
        tvDistance.text = String.format("%.2f", distanceKm)

        val totalSeconds = (mainActivity.getElapsedMinutes() * 60).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        tvTime.text = String.format("%02d:%02d", minutes, seconds)

        val fare = mainActivity.calculateTotalFare()
        tvFare.text = String.format("%.2f", fare)

        mainActivity.currentLocation?.let {
            if (it.hasSpeed()) {
                val speedKmh = it.speed * 3.6
                tvSpeed.text = String.format("%d km/h", speedKmh.roundToInt())
            } else {
                tvSpeed.text = "0 km/h"
            }
        } ?: run {
            tvSpeed.text = "0 km/h"
        }
    }

    private fun showTripSummary(distance: Double, duration: Double, fare: Double) {
        val intent = Intent(requireContext(), TripSummaryActivity::class.java)
        intent.putExtra("DISTANCE", distance)
        intent.putExtra("DURATION", duration)
        intent.putExtra("FARE", fare)
        intent.putExtra("BASE_FARE", mainActivity.BASE_FARE)
        intent.putExtra("DISTANCE_FARE", distance * mainActivity.FARE_PER_KM)
        intent.putExtra("TIME_FARE", duration * mainActivity.FARE_PER_MINUTE)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (mainActivity.isRideStarted && !mainActivity.isPaused) {
            handler.post(updateRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }
}
