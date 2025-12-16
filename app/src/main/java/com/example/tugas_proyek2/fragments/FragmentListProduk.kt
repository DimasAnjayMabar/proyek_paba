package com.example.tugas_proyek2.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.adapters.ProductAdapter
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.databinding.FragmentListProdukBinding
import com.example.tugas_proyek2.misc.GridSpacingItemDecoration
import com.example.tugas_proyek2.services.ProdukService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentListProduk : Fragment() {

    private var _binding: FragmentListProdukBinding? = null
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProductAdapter
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabAddProduk: FloatingActionButton
    private val productList = mutableListOf<DcProduk>()
    private val filteredList = mutableListOf<DcProduk>()
    private val produkService = ProdukService()

    // SharedPreferences untuk stok minimal
    private lateinit var sharedPreferences: SharedPreferences
    private val PREF_NAME = "app_settings"
    private val KEY_MIN_STOCK = "minimal_stock"
    private val DEFAULT_MIN_STOCK = 10

    // Map untuk menyimpan hubungan antara produk dan document ID
    private val productIdMap = mutableMapOf<DcProduk, String>() // Key: objek produk, Value: document ID

    // Notification
    private val CHANNEL_ID = "low_stock_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListProdukBinding.inflate(inflater, container, false)

        // Inisialisasi SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

//        // Buat notification channel (untuk Android Oreo ke atas)
//        createNotificationChannel()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupSwipeRefresh()
        setupFAB()
        loadProductsFromFirestore(showProgressBar = true)
    }

//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = "Stok Rendah Notification"
//            val descriptionText = "Notifikasi untuk produk dengan stok rendah"
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
//                description = descriptionText
//            }
//
//            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            notificationManager.createNotificationChannel(channel)
//        }
//    }
//
//    private fun checkLowStockProducts(products: List<DcProduk>) {
//        // Ambil nilai stok minimal dari SharedPreferences
//        val minStock = sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)
//
//        // Filter produk dengan stok rendah
//        val lowStockProducts = products.filter { produk ->
//            produk.stok != null && produk.stok!! <= minStock
//        }
//
//        // Jika ada produk dengan stok rendah, tampilkan notifikasi
//        if (lowStockProducts.isNotEmpty()) {
//            showLowStockNotification(lowStockProducts, minStock)
//        }
//    }
//
//    private fun showLowStockNotification(lowStockProducts: List<DcProduk>, minStock: Int) {
//        val context = requireContext()
//
//        // Cek apakah izin notifikasi sudah diberikan (untuk Android 13/Tiramisu ke atas)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            val notificationManager = NotificationManagerCompat.from(context)
//            if (!notificationManager.areNotificationsEnabled()) {
//                Log.d("FragmentListProduk", "Notifications are disabled")
//                showSnackbar("Notifikasi dinonaktifkan. Aktifkan untuk menerima peringatan stok rendah.")
//                return
//            }
//        }
//
//        // Buat teks untuk notifikasi
//        val productNames = lowStockProducts.joinToString(", ") { it.nama ?: "Unknown" }
//        val notificationText = if (lowStockProducts.size == 1) {
//            "${lowStockProducts[0].nama} memiliki stok rendah (${lowStockProducts[0].stok} ≤ $minStock)"
//        } else {
//            "${lowStockProducts.size} produk memiliki stok rendah: $productNames"
//        }
//
//        // Buat notification builder
//        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
//            .setSmallIcon(android.R.drawable.ic_dialog_alert)
//            .setContentTitle("⚠️ Stok Produk Rendah")
//            .setContentText(notificationText)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .setAutoCancel(true)
//            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
//
//        // Tampilkan notifikasi dengan pengecekan izin
//        try {
//            with(NotificationManagerCompat.from(context)) {
//                // Gunakan NOTIFICATION_ID yang unik agar notifikasi tidak bertumpuk
//                notify(NOTIFICATION_ID + lowStockProducts.size, builder.build())
//            }
//
//            Log.d("FragmentListProduk", "Low stock notification shown for ${lowStockProducts.size} products")
//        } catch (e: SecurityException) {
//            Log.e("FragmentListProduk", "SecurityException when showing notification: ${e.message}")
//            showSnackbar("Izin notifikasi diperlukan untuk peringatan stok rendah")
//        } catch (e: Exception) {
//            Log.e("FragmentListProduk", "Error showing notification: ${e.message}")
//        }
//    }

    private fun setupRecyclerView() {
        productRecyclerView = binding.recyclerViewProduk

        // Inisialisasi adapter dengan lambda untuk navigasi ke detail
        productAdapter = ProductAdapter(filteredList) { produk ->
            // Navigasi ke detail produk
            navigateToDetailProduk(produk)
        }

        val layoutManager = GridLayoutManager(requireContext(), 3)
        productRecyclerView.layoutManager = layoutManager
        productRecyclerView.adapter = productAdapter

        val animation = android.view.animation.AnimationUtils.loadLayoutAnimation(
            requireContext(),
            R.anim.layout_animation_fall_down
        )
        productRecyclerView.layoutAnimation = animation

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        productRecyclerView.addItemDecoration(GridSpacingItemDecoration(3, spacingInPixels, true))

        productRecyclerView.overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
    }

    private fun navigateToDetailProduk(produk: DcProduk) {
        // Dapatkan document ID dari map menggunakan objek produk sebagai key
        val productId = productIdMap[produk]

        if (productId != null) {
            // Navigasi ke FragmentDetailProduk dengan productId sebagai argument
            val bundle = Bundle().apply {
                putString("produk_id", productId)
            }

            try {
                findNavController().navigate(
                    R.id.action_fragmentListProduk_to_fragmentDetailProduk,
                    bundle
                )
            } catch (e: Exception) {
                Log.e("FragmentListProduk", "Navigation error: ${e.message}")
                showSnackbar("Gagal membuka detail produk")
            }
        } else {
            showSnackbar("Tidak dapat menemukan ID produk")
            Log.e("FragmentListProduk", "Product ID not found for product: $produk")
        }
    }

    private fun setupSearchBar() {
        binding.searchViewProduk.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProducts(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText ?: "")
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout

        // Set warna loading indicator
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Set warna background
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.white)

        // Listener untuk refresh
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("FragmentListProduk", "Swipe refresh triggered")
            loadProductsFromFirestore(showProgressBar = false)
        }
    }

    private fun setupFAB() {
        fabAddProduk = binding.fabAddProduk

        fabAddProduk.setOnClickListener {
            navigateToFormProduk()
        }
    }

    private fun navigateToFormProduk() {
        try {
            findNavController().navigate(
                R.id.action_fragmentListProduk_to_fragmentFormProduk
            )
        } catch (e: Exception) {
            Log.e("FragmentListProduk", "Navigation to form error: ${e.message}")
            showSnackbar("Gagal membuka form produk")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG // Perpanjang durasi untuk notifikasi penting
        ).apply {
            setAction("OK") { dismiss() }
            setActionTextColor(resources.getColor(android.R.color.white, null))
        }.show()
    }

    private fun filterProducts(query: String) {
        filteredList.clear()

        val filtered = if (query.isEmpty()) {
            productList
        } else {
            productList.filter {
                it.nama?.contains(query, ignoreCase = true) == true
            }
        }

        filteredList.addAll(filtered)
        productAdapter.notifyDataSetChanged()

        // Tampilkan pesan jika tidak ada produk
        binding.textEmptyProduk.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewProduk.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadProductsFromFirestore(showProgressBar: Boolean = true) {
        if (showProgressBar) {
            binding.progressBarProduk.visibility = View.VISIBLE
            binding.swipeRefreshLayout.isRefreshing = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Dapatkan produk dengan ID
                val productsWithIds = produkService.getAllProductsWithIds()

                withContext(Dispatchers.Main) {
                    // Clear semua list dan map
                    productList.clear()
                    filteredList.clear()
                    productIdMap.clear()

                    // Tambahkan produk ke list dan simpan mapping ID
                    productsWithIds.forEach { (documentId, produk) ->
                        productList.add(produk)
                        // Simpan mapping objek produk -> document ID
                        productIdMap[produk] = documentId
                    }

                    // Update filtered list
                    filteredList.addAll(productList)

                    productAdapter.notifyDataSetChanged()

                    // Jalankan animasi jika ada data
                    if (productList.isNotEmpty()) {
                        productRecyclerView.scheduleLayoutAnimation()
                    }

                    // Update UI state
                    binding.progressBarProduk.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

//                    // Cek stok rendah
//                    checkLowStockProducts(productList)

                    // Show empty state if no data
                    if (productList.isEmpty()) {
                        binding.textEmptyProduk.text = "Belum ada produk"
                        binding.textEmptyProduk.visibility = View.VISIBLE
                        binding.recyclerViewProduk.visibility = View.GONE
                        fabAddProduk.show() // Tetap tampilkan FAB
                    } else {
                        binding.textEmptyProduk.visibility = View.GONE
                        binding.recyclerViewProduk.visibility = View.VISIBLE
                        fabAddProduk.show()

                        // Show success snackbar on refresh
                        if (!showProgressBar) {
                            showSnackbar("${productList.size} produk dimuat ulang")
                        }

                        Log.d("FragmentListProduk", "Loaded ${productList.size} products")
                        Log.d("FragmentListProduk", "Product ID map size: ${productIdMap.size}")

                        // Tampilkan info stok minimal
                        val minStock = sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)
                        Log.d("FragmentListProduk", "Checking stock against minimum: $minStock")
                    }
                }
            } catch (e: Exception) {
                Log.e("FragmentListProduk", "Error loading products: ${e.message}")

                withContext(Dispatchers.Main) {
                    binding.progressBarProduk.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    binding.textEmptyProduk.text = "Gagal memuat produk: ${e.message}"
                    binding.textEmptyProduk.visibility = View.VISIBLE
                    binding.recyclerViewProduk.visibility = View.GONE
                    fabAddProduk.show() // Tetap tampilkan FAB meski error

                    // Show error snackbar
                    showSnackbar("Gagal memuat produk")
                }
            }
        }
    }

    // Fungsi untuk mendapatkan nilai stok minimal
    fun getCurrentMinStock(): Int {
        return sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}