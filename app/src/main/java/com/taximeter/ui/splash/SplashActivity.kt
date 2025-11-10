package com.taximeter.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.taximeter.MainActivity
import com.taximeter.app.MainActivity
import com.taximeter.app.databinding.ActivitySplashBinding
import com.taximeter.app.ui.auth.LoginActivity
import com.taximeter.app.viewmodels.AuthViewModel

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = viewModel.getCurrentUser()
            val intent = if (currentUser != null) {
                Intent(this, LoginActivity::class.java)
            } else {
                Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 2000)
    }
}