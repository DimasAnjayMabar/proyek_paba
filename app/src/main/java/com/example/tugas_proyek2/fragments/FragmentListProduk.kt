package com.example.tugas_proyek2.fragments

import android.app.AlertDialog
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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageButton
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
import com.example.tugas_proyek2.service_layers.DistributorService
import com.example.tugas_proyek2.service_layers.KategoriService
import com.example.tugas_proyek2.services.ProdukService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
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

    private lateinit var btnFilterProduk: AppCompatImageButton
    private val kategoriService = KategoriService()
    private val distributorService = DistributorService()

    private var selectedKategoriId: String? = null
    private var selectedDistributorId: String? = null
    private var hargaMin: Long? = null
    private var hargaMax: Long? = null

    private val kategoriMap = mutableMapOf<String, String>() // Nama kategori -> ID
    private val distributorMap = mutableMapOf<String, String>() // Nama distributor -> ID

    private val kategoriList = mutableListOf<String>()
    private val distributorList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListProdukBinding.inflate(inflater, container, false)

        // Inisialisasi SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupSwipeRefresh()
        setupFAB()
        setupFilterButton()
        loadProductsFromFirestore(showProgressBar = true)
    }

    private fun setupFilterButton() {
        btnFilterProduk = binding.btnFilterProduk

        btnFilterProduk.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        // Load data untuk dropdown
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Load kategori
                val kategoris = kategoriService.getAllKategoris()
                withContext(Dispatchers.Main) {
                    kategoriMap.clear()
                    kategoriList.clear()
                    kategoriList.add("Semua Kategori")
                    kategoris.forEach { (id, kategori) ->
                        kategori.nama?.let { nama ->
                            kategoriMap[nama] = id
                            kategoriList.add(nama)
                        }
                    }
                }

                // Load distributor
                val distributors = distributorService.getAllDistributors()
                withContext(Dispatchers.Main) {
                    distributorMap.clear()
                    distributorList.clear()
                    distributorList.add("Semua Distributor")
                    distributors.forEach { (id, distributor) ->
                        distributor.nama?.let { nama ->
                            distributorMap[nama] = id
                            distributorList.add(nama)
                        }
                    }

                    // Tampilkan dialog setelah data di-load
                    showFilterDialogUI()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("FragmentListProduk", "Error loading filter data: ${e.message}")
                    showSnackbar("Gagal memuat data filter")
                }
            }
        }
    }

    private fun showFilterDialogUI() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_filter_produk, null)

        val spinnerKategori = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerKategori)
        val spinnerDistributor = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerDistributor)
        val etHargaMin = dialogView.findViewById<TextInputEditText>(R.id.etHargaMin)
        val etHargaMax = dialogView.findViewById<TextInputEditText>(R.id.etHargaMax)
        val btnReset = dialogView.findViewById<Button>(R.id.btnReset)
        val btnTerapkan = dialogView.findViewById<Button>(R.id.btnTerapkan)

        // Setup dropdown kategori
        val kategoriAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            kategoriList
        )
        spinnerKategori.setAdapter(kategoriAdapter)

        // Set selected kategori jika ada
        selectedKategoriId?.let { selectedId ->
            val selectedKategoriName = kategoriMap.entries.find { it.value == selectedId }?.key
            selectedKategoriName?.let { spinnerKategori.setText(it, false) }
        }

        // Setup dropdown distributor
        val distributorAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            distributorList
        )
        spinnerDistributor.setAdapter(distributorAdapter)

        // Set selected distributor jika ada
        selectedDistributorId?.let { selectedId ->
            val selectedDistributorName = distributorMap.entries.find { it.value == selectedId }?.key
            selectedDistributorName?.let { spinnerDistributor.setText(it, false) }
        }

        // Set harga min/max jika ada
        hargaMin?.let { etHargaMin.setText(it.toString()) }
        hargaMax?.let { etHargaMax.setText(it.toString()) }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnReset.setOnClickListener {
            // Reset semua filter
            spinnerKategori.setText("Semua Kategori", false)
            spinnerDistributor.setText("Semua Distributor", false)
            etHargaMin.text?.clear()
            etHargaMax.text?.clear()

            selectedKategoriId = null
            selectedDistributorId = null
            hargaMin = null
            hargaMax = null

            // Refresh produk tanpa filter
            filterProducts(binding.searchViewProduk.query.toString())
            dialog.dismiss()
        }

        btnTerapkan.setOnClickListener {
            // Apply filter
            val selectedKategoriName = spinnerKategori.text.toString()
            val selectedDistributorName = spinnerDistributor.text.toString()

            selectedKategoriId = if (selectedKategoriName != "Semua Kategori" && selectedKategoriName.isNotEmpty()) {
                kategoriMap[selectedKategoriName]
            } else {
                null
            }

            selectedDistributorId = if (selectedDistributorName != "Semua Distributor" && selectedDistributorName.isNotEmpty()) {
                distributorMap[selectedDistributorName]
            } else {
                null
            }

            // Parse harga min/max
            hargaMin = etHargaMin.text.toString().toLongOrNull()
            hargaMax = etHargaMax.text.toString().toLongOrNull()

            // Validasi harga
            if (hargaMin != null && hargaMax != null && hargaMin!! > hargaMax!!) {
                showSnackbar("Harga minimum tidak boleh lebih besar dari harga maksimum")
                return@setOnClickListener
            }

            // Apply filter ke produk
            applyFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyFilters() {
        val searchQuery = binding.searchViewProduk.query.toString()

        // Filter berdasarkan semua kriteria
        val filtered = productList.filter { produk ->
            // Filter kategori
            val kategoriMatch = selectedKategoriId?.let {
                produk.kategori_id == it
            } ?: true

            // Filter distributor
            val distributorMatch = selectedDistributorId?.let {
                produk.distributor_id == it
            } ?: true

            // Filter harga
            val hargaJual = produk.harga_jual ?: 0L
            val minMatch = hargaMin?.let { hargaJual >= it } ?: true
            val maxMatch = hargaMax?.let { hargaJual <= it } ?: true

            // Filter pencarian
            val searchMatch = if (searchQuery.isNotEmpty()) {
                produk.nama?.contains(searchQuery, ignoreCase = true) == true
            } else {
                true
            }

            kategoriMatch && distributorMatch && minMatch && maxMatch && searchMatch
        }

        // Update UI
        filteredList.clear()
        filteredList.addAll(filtered)
        productAdapter.notifyDataSetChanged()

        // Tampilkan pesan jika tidak ada produk
        binding.textEmptyProduk.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewProduk.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE

        // Tampilkan jumlah hasil filter
        if (filteredList.isNotEmpty()) {
            val filterInfo = buildString {
                append("Menampilkan ${filteredList.size} produk")
                selectedKategoriId?.let { append(" • Kategori terfilter") }
                selectedDistributorId?.let { append(" • Distributor terfilter") }
                if (hargaMin != null || hargaMax != null) append(" • Harga terfilter")
            }
            showSnackbar(filterInfo)
        }
    }

    private fun setupRecyclerView() {
        productRecyclerView = binding.recyclerViewProduk

        productAdapter = ProductAdapter(filteredList) { produk ->
            navigateToDetailProduk(produk)
        }

        val layoutManager = GridLayoutManager(requireContext(), 2)
        productRecyclerView.layoutManager = layoutManager
        productRecyclerView.adapter = productAdapter

        val animation = android.view.animation.AnimationUtils.loadLayoutAnimation(
            requireContext(),
            R.anim.layout_animation_fall_down
        )
        productRecyclerView.layoutAnimation = animation

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.grid_spacing)

        productRecyclerView.addItemDecoration(GridSpacingItemDecoration(2, spacingInPixels, true))

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

                    selectedKategoriId = null
                    selectedDistributorId = null
                    hargaMin = null
                    hargaMax = null

                    applyFilters()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}