
package com.taximeter.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.taximeter.MainActivity
import com.taximeter.app.R

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var mainActivity: MainActivity
    private val routePoints = mutableListOf<LatLng>()
    private var polyline: Polyline? = null
    private var currentMarker: Marker? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivity = activity as MainActivity
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
            isCompassEnabled = true
            isMapToolbarEnabled = true
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            mainActivity.currentLocation?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                addCurrentLocationMarker(latLng)
            }
        }
        startLocationTracking()
    }

    private fun addCurrentLocationMarker(latLng: LatLng) {
        currentMarker?.remove()
        currentMarker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(getString(R.string.current_location))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        )
    }

    private fun startLocationTracking() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                updateMapLocation()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun updateMapLocation() {
        if (!isAdded) return
        mainActivity.currentLocation?.let { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            addCurrentLocationMarker(latLng)

            if (mainActivity.isRideStarted && !mainActivity.isPaused) {
                routePoints.add(latLng)
                updatePolyline()
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
        }
    }

    private fun updatePolyline() {
        polyline?.remove()
        if (routePoints.size > 1) {
            val polylineOptions = PolylineOptions()
                .addAll(routePoints)
                .color(Color.parseColor("#FFD700"))
                .width(10f)
                .geodesic(true)
            polyline = googleMap.addPolyline(polylineOptions)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        routePoints.clear()
        polyline?.remove()
    }
}
