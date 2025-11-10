package com.taximeter.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.taximeter.app.databinding.ActivityDriverProfileBinding
import com.taximeter.app.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Driver Profile"

        // Get data from intent
        val driverName = intent.getStringExtra("DRIVER_NAME") ?: "Unknown"
        val driverAge = intent.getIntExtra("DRIVER_AGE", 0)
        val driverLicense = intent.getStringExtra("DRIVER_LICENSE") ?: "Unknown"

        // Display data
        binding.tvDriverName.text = "Name: $driverName"
        binding.tvAge.text = "Age: $driverAge"
        binding.tvLicense.text = "License: $driverLicense"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}