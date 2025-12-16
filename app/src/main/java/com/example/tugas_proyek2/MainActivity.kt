package com.example.tugas_proyek2

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.cloudinary.android.MediaManager
import com.example.tugas_proyek2.databinding.ActivityMainBinding
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.services.ProdukService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // SharedPreferences untuk stok minimal
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "app_settings"
    private val KEY_MIN_STOCK = "minimal_stock"
    private val DEFAULT_MIN_STOCK = 10

    // Notification
    private val CHANNEL_ID = "low_stock_channel_global"
    private val NOTIFICATION_ID = 2001

    // Service
    private val produkService = ProdukService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Buat notification channel
        createNotificationChannel()

        // ============ INISIALISASI CLOUDINARY ============
        initCloudinary()
        // =================================================

        // ============ CEK STOK RENDAH SAAT APP DIBUKA ============
        checkLowStockOnAppStart()
        // =========================================================

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

    // ============ METHOD UNTUK CEK STOK RENDAH ============
    private fun checkLowStockOnAppStart() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val productsWithIds = produkService.getAllProductsWithIds()
                val products = productsWithIds.values.toList()

                withContext(Dispatchers.Main) {
                    checkLowStockProducts(products)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking low stock: ${e.message}")
            }
        }
    }

    // Bisa dipanggil dari fragment lain jika perlu
    fun checkLowStockGlobally() {
        checkLowStockOnAppStart()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Stok Rendah Notification Global"
            val descriptionText = "Notifikasi untuk produk dengan stok rendah (global)"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkLowStockProducts(products: List<DcProduk>) {
        // Ambil nilai stok minimal dari SharedPreferences
        val minStock = sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)

        // Filter produk dengan stok rendah
        val lowStockProducts = products.filter { produk ->
            produk.stok != null && produk.stok!! <= minStock
        }

        // Jika ada produk dengan stok rendah, tampilkan notifikasi
        if (lowStockProducts.isNotEmpty()) {
            showLowStockNotification(lowStockProducts, minStock)
        }
    }

    private fun showLowStockNotification(lowStockProducts: List<DcProduk>, minStock: Int) {
        // Cek apakah izin notifikasi sudah diberikan (untuk Android 13/Tiramisu ke atas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = NotificationManagerCompat.from(this)
            if (!notificationManager.areNotificationsEnabled()) {
                Log.d("MainActivity", "Notifications are disabled globally")
                return
            }
        }

        // Buat teks untuk notifikasi
        val productNames = lowStockProducts.joinToString(", ") { it.nama ?: "Unknown" }
        val notificationText = if (lowStockProducts.size == 1) {
            "Produk ${lowStockProducts[0].nama} memiliki stok rendah (${lowStockProducts[0].stok} ≤ $minStock). Harap cek modul inventory anda"
        } else {
            "${lowStockProducts.size} produk memiliki stok rendah: $productNames"
        }

        // Buat notification builder
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Stok Produk Rendah")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))

        // Tampilkan notifikasi dengan pengecekan izin
        try {
            with(NotificationManagerCompat.from(this)) {
                // Gunakan ID yang unik berdasarkan timestamp agar notifikasi baru muncul
                val notificationId = NOTIFICATION_ID + System.currentTimeMillis().toInt()
                notify(notificationId, builder.build())
            }

            Log.d("MainActivity", "Global low stock notification shown for ${lowStockProducts.size} products")
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException when showing global notification: ${e.message}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing global notification: ${e.message}")
        }
    }
    // ========================================================

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

    // Fungsi untuk mendapatkan nilai stok minimal (bisa diakses dari fragment)
    fun getCurrentMinStock(): Int {
        return sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)
    }
}