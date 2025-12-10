package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
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

    // Map untuk menyimpan hubungan antara produk dan document ID
    private val productIdMap = mutableMapOf<DcProduk, String>() // Key: objek produk, Value: document ID

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListProdukBinding.inflate(inflater, container, false)
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
            Snackbar.LENGTH_SHORT
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