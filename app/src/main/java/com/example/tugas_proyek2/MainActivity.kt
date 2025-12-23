package com.example.tugas_proyek2

import android.Manifest // Tambahkan import ini
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager // Tambahkan import ini
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast // Tambahkan import ini untuk feedback user
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts // Tambahkan import ini
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat // Tambahkan import ini
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
    // ... (kode variabel lain tetap sama) ...
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "app_settings"
    private val KEY_MIN_STOCK = "minimal_stock"
    private val DEFAULT_MIN_STOCK = 10
    private val CHANNEL_ID = "low_stock_channel_global"
    private val NOTIFICATION_ID = 2001
    private val produkService = ProdukService()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
            checkLowStockOnAppStart() // Cek langsung jika diizinkan
        } else {
            Log.d("MainActivity", "Notification permission denied")
            Toast.makeText(this, "Izin notifikasi diperlukan untuk peringatan stok", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        createNotificationChannel()
        initCloudinary()

        askNotificationPermission()
        checkLowStockOnAppStart()

        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.appbar))
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.frame_layout) as NavHostFragment
        navController = navHostFragment.navController
        val topLevelDestinations = setOf(R.id.homeFragment, R.id.settingsFragment)
        appBarConfiguration = AppBarConfiguration(topLevelDestinations, binding.drawer)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

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
        val minStock = sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)
        val lowStockProducts = products.filter { produk ->
            produk.stok != null && produk.stok!! <= minStock
        }
        if (lowStockProducts.isNotEmpty()) {
            showLowStockNotification(lowStockProducts, minStock)
        }
    }

    private fun showLowStockNotification(lowStockProducts: List<DcProduk>, minStock: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Jangan return begitu saja, coba log error atau biarkan (karena kita sudah minta di onCreate)
                Log.e("MainActivity", "Permission not granted for notification")
                return
            }
        }

        val productNames = lowStockProducts.joinToString(", ") { it.nama ?: "Unknown" }
        val notificationText = if (lowStockProducts.size == 1) {
            "Produk ${lowStockProducts[0].nama} memiliki stok rendah (${lowStockProducts[0].stok} ≤ $minStock). Harap cek modul inventory anda"
        } else {
            "${lowStockProducts.size} produk memiliki stok rendah: $productNames"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Pastikan icon ini ada, atau ganti android.R.drawable.ic_dialog_alert
            .setContentTitle("⚠️ Stok Produk Rendah")
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))

        try {
            with(NotificationManagerCompat.from(this)) {
                val notificationId = NOTIFICATION_ID + System.currentTimeMillis().toInt()
                notify(notificationId, builder.build())
            }
            Log.d("MainActivity", "Global low stock notification shown")
        } catch (e: SecurityException) {
            Log.e("MainActivity", "SecurityException: ${e.message}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing notification: ${e.message}")
        }
    }

    private fun initCloudinary() {
        try {
            MediaManager.get()
            Log.d("MainActivity", "Cloudinary sudah diinisialisasi")
        } catch (e: IllegalStateException) {
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

    fun getCurrentMinStock(): Int {
        return sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)
    }
}