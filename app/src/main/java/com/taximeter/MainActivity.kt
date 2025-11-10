
package com.taximeter.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.taximeter.fragments.*
import kotlinx.android.synthetic.main.activity_main_drawer.*
import pub.devrel.easypermissions.EasyPermissions

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    EasyPermissions.PermissionCallbacks {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    var currentLocation: Location? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle

    var isRideStarted = false
    var isPaused = false
    var totalDistance = 0.0
    var tripStartTime = 0L
    var pausedTime = 0L
    var totalPausedDuration = 0L
    var lastLocation: Location? = null

    val BASE_FARE = 2.5
    val FARE_PER_KM = 1.5
    val FARE_PER_MINUTE = 0.5

    companion object {
        private const val PERMISSION_LOCATION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_drawer)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupNavigationDrawer()
        setupLocationCallback()
        checkLocationPermission()

        if (savedInstanceState == null) {
            loadFragment(MeterFragment())
            navigationView.setCheckedItem(R.id.nav_meter)
        }
    }

    private fun setupNavigationDrawer() {
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView: NavigationView = findViewById(R.id.navigationView)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    updateLocation(location)
                }
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

    fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 2000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun updateLocation(location: Location) {
        if (isRideStarted && !isPaused) {
            lastLocation?.let { last ->
                val distance = last.distanceTo(location)
                if (distance > 5) {
                    totalDistance += distance
                }
            }
            lastLocation = location
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_meter -> loadFragment(MeterFragment())
            R.id.nav_map -> loadFragment(MapFragment())
            R.id.nav_history -> loadFragment(HistoryFragment())
            R.id.nav_settings -> loadFragment(SettingsFragment())
            R.id.nav_profile -> openDriverProfile()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun openDriverProfile() {
        val intent = Intent(this, DriverProfileActivity::class.java)
        intent.putExtra("DRIVER_NAME", "KARIMA")
        intent.putExtra("DRIVER_AGE", 33)
        intent.putExtra("DRIVER_LICENSE", "Permis A")
        startActivity(intent)
    }

    fun getElapsedMinutes(): Double {
        if (tripStartTime == 0L) return 0.0
        val currentTime = if (isPaused) pausedTime else System.currentTimeMillis()
        val elapsed = currentTime - tripStartTime - totalPausedDuration
        return elapsed / 60000.0
    }

    fun calculateTotalFare(): Double {
        val distanceKm = totalDistance / 1000.0
        val minutes = getElapsedMinutes()
        val distanceFare = distanceKm * FARE_PER_KM
        val timeFare = minutes * FARE_PER_MINUTE
        return BASE_FARE + distanceFare + timeFare
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
    }
}