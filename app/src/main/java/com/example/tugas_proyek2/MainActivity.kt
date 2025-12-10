package com.example.tugas_proyek2

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.cloudinary.android.MediaManager
import com.example.tugas_proyek2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ============ TAMBAHKAN INISIALISASI CLOUDINARY DI SINI ============
        initCloudinary()
        // ===================================================================

        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.appbar))

        // Handle edge-to-edge hanya untuk root view
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.frame_layout) as NavHostFragment
        navController = navHostFragment.navController

        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.settingsFragment
        )

        appBarConfiguration = AppBarConfiguration(
            topLevelDestinations,
            binding.drawer
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    // ============ TAMBAHKAN METHOD INISIALISASI CLOUDINARY ============
    private fun initCloudinary() {
        try {
            // Coba dapatkan instance untuk cek apakah sudah diinisialisasi
            MediaManager.get()
            Log.d("MainActivity", "Cloudinary sudah diinisialisasi")
        } catch (e: IllegalStateException) {
            // Jika belum diinisialisasi, init sekarang
            val config = mapOf(
                "cloud_name" to "djozilizv",
                "upload_preset" to "preset1"
            )
            MediaManager.init(this, config)
            Log.d("MainActivity", "Cloudinary diinisialisasi")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing Cloudinary: ${e.message}")
        }
    }
    // ===================================================================

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
            binding.drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}