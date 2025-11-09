package com.taximeter.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.taximeter.app.databinding.ActivityMainBinding
import com.taximeter.app.ui.profile.ProfileActivity
import pub.devrel.easypermissions.EasyPermissions
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var googleMap: GoogleMap
    private var currentLocation: Location? = null
    private var lastLocation: Location? = null
    private val routePoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null
    private var currentMarker: Marker? = null

    // Trip state
    private var isRideStarted = false
    private var isPaused = false

    // Trip data
    private var totalDistance = 0.0 // in meters
    private var tripStartTime = 0L
    private var pausedTime = 0L
    private var totalPausedDuration = 0L

    // Fare calculation constants (as per project requirements)
    private val BASE_FARE = 2.5 // DH - Tarif de base
    private val FARE_PER_KM = 1.5 // DH - Tarif par kilomètre
    private val FARE_PER_MINUTE = 0.5 // DH - Tarif par minute

    // UI update handler - updates every second
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRideStarted && !isPaused) {
                updateUI()
                handler.postDelayed(this, 1000) // Update every second
            }
        }
    }

    // Driver info
    private val driverName = "KARIMA"
    private val driverAge = 33
    private val driverLicense = "Permis A"

    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 1
        private const val NOTIFICATION_CHANNEL_ID = "taxi_meter_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup map


        // Setup location callback - this is where real-time tracking happens
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    android.util.Log.d("TaxiMeter", "Location update: ${location.latitude}, ${location.longitude}")
                    updateLocation(location)
                }
            }
        }

        setupClickListeners()
        setupBottomNavigation()
        checkLocationPermission()
        setupDriverProfile()
        updateButtonVisibility()

        // Initialize UI with base fare
        updateUI()
    }

    private fun setupClickListeners() {
        binding.btnStart.setOnClickListener {
            startRide()
        }

        binding.btnPause.setOnClickListener {
            if (!isPaused) {
                pauseRide()
            } else {
                resumeRide()
            }
        }

        binding.btnStop.setOnClickListener {
            stopRide()
        }

        binding.btnProfile.setOnClickListener {
            showDriverProfile()
        }

        binding.btnReset.setOnClickListener {
            resetMeter()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, getString(R.string.nav_home), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_history -> {
                    Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    showDriverProfile()
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, getString(R.string.coming_soon), Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun setupDriverProfile() {}

    private fun showDriverProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("DRIVER_NAME", driverName)
        intent.putExtra("DRIVER_AGE", driverAge)
        intent.putExtra("DRIVER_LICENSE", driverLicense)
        startActivity(intent)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                googleMap.isMyLocationEnabled = true
                getCurrentLocation()
            }
        }
    }

    private fun checkLocationPermission() {
        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (EasyPermissions.hasPermissions(this, *perms)) {
            startLocationUpdates()
        } else {
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.permission_location_rationale),
                PERMISSION_LOCATION_REQUEST_CODE,
                *perms
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 1000 // Update every 1 second (more frequent for testing)
            fastestInterval = 500 // Fastest update: 0.5 second
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f // Track even small movements (1 meter)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            android.util.Log.d("TaxiMeter", "Location updates started")
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    val latLng = LatLng(it.latitude, it.longitude)

                    // Add marker for driver position
                    currentMarker?.remove()
                    currentMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title("Position du chauffeur")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    )

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }

    /**
     * Update location in real-time and calculate distance
     * This is the core logic for the taxi meter
     */
    private fun updateLocation(location: Location) {
        currentLocation = location
        val latLng = LatLng(location.latitude, location.longitude)

        // Update marker position
        currentMarker?.remove()
        currentMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Position du chauffeur")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        )

        // Only track distance when ride is active and not paused
        if (isRideStarted && !isPaused) {
            // Calculate distance from last position
            lastLocation?.let { last ->
                val distance = last.distanceTo(location) // Distance in meters

                android.util.Log.d("TaxiMeter", "Distance calculated: $distance meters, Total: $totalDistance meters")

                // Only count significant movements (more than 5 meters to avoid GPS noise)
                if (distance > 5) {
                    totalDistance += distance
                    routePoints.add(latLng)
                    updatePolyline()

                    // Update UI immediately when distance changes
                    updateUI()

                    android.util.Log.d("TaxiMeter", "Distance added! New total: $totalDistance meters")
                }
            }

            // Update last location
            lastLocation = location

            // Animate camera to follow driver
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        } else {
            android.util.Log.d("TaxiMeter", "Ride not started or paused. isRideStarted: $isRideStarted, isPaused: $isPaused")
        }
    }

    private fun updatePolyline() {
        polyline?.remove()

        if (routePoints.size > 1) {
            val polylineOptions = PolylineOptions()
                .addAll(routePoints)
                .color(Color.parseColor("#FFD700")) // Yellow taxi color
                .width(10f)

            polyline = googleMap.addPolyline(polylineOptions)
        }
    }

    /**
     * Start the ride - Initialize tracking
     */
    private fun startRide() {
        isRideStarted = true
        isPaused = false
        tripStartTime = System.currentTimeMillis()
        totalDistance = 0.0
        totalPausedDuration = 0L
        routePoints.clear()
        polyline?.remove()

        // Set initial position
        lastLocation = currentLocation
        currentLocation?.let {
            routePoints.add(LatLng(it.latitude, it.longitude))
            android.util.Log.d("TaxiMeter", "Ride started at: ${it.latitude}, ${it.longitude}")
        }

        updateButtonVisibility()
        handler.post(updateRunnable)

        Toast.makeText(this, getString(R.string.ride_started), Toast.LENGTH_SHORT).show()
    }

    /**
     * Pause the ride - Stop tracking but keep data
     */
    private fun pauseRide() {
        isPaused = true
        pausedTime = System.currentTimeMillis()
        binding.btnPause.text = getString(R.string.resume)

        Toast.makeText(this, getString(R.string.ride_paused), Toast.LENGTH_SHORT).show()
    }

    /**
     * Resume the ride - Continue tracking
     */
    private fun resumeRide() {
        isPaused = false
        totalPausedDuration += System.currentTimeMillis() - pausedTime
        binding.btnPause.text = getString(R.string.pause)

        handler.post(updateRunnable)

        Toast.makeText(this, getString(R.string.ride_resumed), Toast.LENGTH_SHORT).show()
    }

    /**
     * Stop the ride - End tracking and show summary
     */
    private fun stopRide() {
        isRideStarted = false
        isPaused = false
        handler.removeCallbacks(updateRunnable)

        val totalFare = calculateTotalFare()
        val distanceKm = totalDistance / 1000.0
        val durationMinutes = getElapsedMinutes()

        // Send notification with trip details
        sendTripNotification(distanceKm, durationMinutes, totalFare)

        updateButtonVisibility()

        Toast.makeText(this, getString(R.string.ride_completed), Toast.LENGTH_SHORT).show()
    }

    /**
     * Reset the meter - Clear all data
     */
    private fun resetMeter() {
        if (!isRideStarted) {
            totalDistance = 0.0
            tripStartTime = 0L
            pausedTime = 0L
            totalPausedDuration = 0L
            routePoints.clear()
            polyline?.remove()
            lastLocation = null

            updateUI()

            Toast.makeText(this, getString(R.string.meter_reset), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.cannot_reset_during_ride), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Update button visibility based on ride state
     */
    private fun updateButtonVisibility() {
        if (isRideStarted) {
            // Ride is active - hide Start, show Pause and Stop
            binding.btnStart.visibility = View.GONE
            binding.btnReset.visibility = View.VISIBLE
            binding.btnStop.visibility = View.VISIBLE
            binding.btnStop.isEnabled = true  // ENABLE Stop button
            binding.btnReset.isEnabled = true // DISABLE Reset button
        } else {
            // Ride is not active - show Start, hide Pause and Stop
            binding.btnStart.visibility = View.VISIBLE
            binding.btnReset.visibility = View.VISIBLE
            binding.btnStop.visibility = View.GONE
            binding.btnStop.isEnabled = true
            binding.btnReset.isEnabled = true
        }
    }

    /**
     * Update UI with current trip data
     * Called every second when ride is active
     */
    private fun updateUI() {
        // Distance in kilometers (2 decimal places)
        val distanceKm = totalDistance / 1000.0
        binding.tvDistance.text = String.format("%.2f", distanceKm)

        // Time in MM:SS format
        val elapsedMinutes = getElapsedMinutes()
        val totalSeconds = (elapsedMinutes * 60).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.tvTime.text = String.format("%02d:%02d", minutes, seconds)

        // Fare calculation (2 decimal places)
        val fare = calculateTotalFare()
        binding.tvFare.text = String.format("%.2f", fare)

        // Speed display
        currentLocation?.let {
            if (it.hasSpeed()) {
                val speedKmh = it.speed * 3.6 // Convert m/s to km/h
                binding.tvSpeed.text = String.format("%d km/h", speedKmh.roundToInt())
            } else {
                binding.tvSpeed.text = "0 km/h"
            }
        } ?: run {
            binding.tvSpeed.text = "0 km/h"
        }
    }

    /**
     * Get elapsed time in minutes (excluding paused time)
     */
    private fun getElapsedMinutes(): Double {
        if (tripStartTime == 0L) return 0.0

        val currentTime = if (isPaused) pausedTime else System.currentTimeMillis()
        val elapsed = currentTime - tripStartTime - totalPausedDuration

        return elapsed / 60000.0 // Convert milliseconds to minutes
    }

    /**
     * Calculate total fare based on project requirements:
     * - Base fare: 2.5 DH
     * - Distance fare: 1.5 DH per kilometer
     * - Time fare: 0.5 DH per COMPLETE minute (not per second)
     */
    private fun calculateTotalFare(): Double {
        val distanceKm = totalDistance / 1000.0
        val totalMinutes = getElapsedMinutes()

        // Only count COMPLETE minutes for time fare
        val completeMinutes = totalMinutes.toInt()

        val distanceFare = distanceKm * FARE_PER_KM
        val timeFare = completeMinutes * FARE_PER_MINUTE  // 0.5 DH per complete minute

        return BASE_FARE + distanceFare + timeFare
    }

    /**
     * Send notification at end of ride with trip summary
     */
    private fun sendTripNotification(distance: Double, duration: Double, fare: Double) {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_taxi)
            .setContentTitle(getString(R.string.trip_completed))
            .setContentText("${getString(R.string.fare)}: ${String.format("%.2f", fare)} DH")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "${getString(R.string.distance)}: ${String.format("%.2f", distance)} km\n" +
                                "${getString(R.string.duration)}: ${duration.roundToInt()} min\n" +
                                "${getString(R.string.total_fare)}: ${String.format("%.2f", fare)} DH\n\n" +
                                "Tarif de base: ${String.format("%.2f", BASE_FARE)} DH\n" +
                                "Tarif distance: ${String.format("%.2f", distance * FARE_PER_KM)} DH\n" +
                                "Tarif temps: ${String.format("%.2f", duration * FARE_PER_MINUTE)} DH"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        startLocationUpdates()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(updateRunnable)
    }
}