package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.databinding.FragmentCashierBinding
import com.example.tugas_proyek2.service_layers.CartService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FragmentCashier : Fragment() {

    private var _binding: FragmentCashierBinding? = null
    private val binding get() = _binding!!
    private val cartService = CartService()

    // Simpan produk yang ditemukan beserta Firestore Document ID-nya
    private var currentProduk: Pair<String, DcProduk>? = null

    // Untuk debounce search
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCashierBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Sembunyikan tombol checkout awalnya
//        binding.btnCheckout.visibility = View.GONE

        // Atur hint dan enable/disable state
        binding.btnAddToCart.isEnabled = false
        binding.btnAddToCart.alpha = 0.5f
    }

    private fun setupListeners() {
        // Text changed listener dengan debounce
        binding.editTextSearchProduct.addTextChangedListener { editable ->
            searchJob?.cancel() // Batalkan job sebelumnya jika ada

            val searchText = editable.toString().trim()
            if (searchText.isNotEmpty() && searchText.length >= 2) {
                searchJob = lifecycleScope.launch {
                    delay(500) // Debounce 500ms
                    searchProduk(searchText)
                }
            } else {
                resetProdukInfo()
            }
        }

        // Tombol Tambah ke Keranjang
        binding.btnAddToCart.setOnClickListener {
            currentProduk?.let { (produkId, produk) ->
                addProdukToCart(produkId, produk)
            } ?: run {
                showSnackbar("Pilih produk terlebih dahulu")
            }
        }

        // Tombol Checkout
//        binding.btnCheckout.setOnClickListener {
//            navigateToCart()
//        }

        // FAB untuk buka cart
        binding.fabOpenCart.setOnClickListener {
            navigateToCart()
        }
    }

    private fun searchProduk(namaProduk: String) {
        lifecycleScope.launch {
            try {
                val result = cartService.searchProdukByName(namaProduk)

                if (result != null) {
                    val (produkId, produk) = result
                    // Produk ditemukan
                    currentProduk = produkId to produk
                    updateUIForProdukFound(produk)
                    showSnackbar("Produk ditemukan: ${produk.nama}")
                } else {
                    // Produk tidak ditemukan
                    resetProdukInfo()
                    showSnackbar("Produk tidak ditemukan")
                }
            } catch (e: Exception) {
                resetProdukInfo()
                showSnackbar("Error mencari produk: ${e.localizedMessage}")
            }
        }
    }

    private fun updateUIForProdukFound(produk: DcProduk) {
        binding.btnAddToCart.isEnabled = true
        binding.btnAddToCart.alpha = 1.0f
        binding.btnAddToCart.text = "Tambah ${produk.nama} ke Keranjang"

        // Update hint dengan info harga
        val hargaJual = produk.harga_jual ?: 0L
        binding.editTextSearchProduct.hint = "${produk.nama} - Rp ${formatRupiah(hargaJual)}"
    }

    private fun resetProdukInfo() {
        currentProduk = null
        binding.btnAddToCart.isEnabled = false
        binding.btnAddToCart.alpha = 0.5f
        binding.btnAddToCart.text = "Tambah ke Keranjang"
    }

    private fun addProdukToCart(produkId: String, produk: DcProduk) {
        lifecycleScope.launch {
            try {
                binding.btnAddToCart.isEnabled = false

                val success = cartService.addToCart(produkId, produk)

                if (success) {
                    // Reset form
                    binding.editTextSearchProduct.text?.clear()
                    resetProdukInfo()

                    // Tampilkan konfirmasi
                    showToast("${produk.nama} berhasil ditambahkan ke keranjang")

                    // Tampilkan tombol checkout
//                    binding.btnCheckout.visibility = View.VISIBLE

                    // Get jumlah item di cart
                    val cartItems = cartService.getAllCartItemsWithIds()
//                    if (cartItems.isNotEmpty()) {
//                        binding.btnCheckout.text = "Checkout (${cartItems.size} items)"
//                    }
                } else {
                    showSnackbar("Gagal menambahkan produk ke keranjang")
                }
            } catch (e: Exception) {
                showSnackbar("Error: ${e.localizedMessage}")
            } finally {
                binding.btnAddToCart.isEnabled = true
            }
        }
    }

    private fun navigateToCart() {
        findNavController().navigate(R.id.action_fragmentCashier_to_cartFragment)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun formatRupiah(amount: Long): String {
        return String.format("%,d", amount).replace(",", ".")
    }

    override fun onResume() {
        super.onResume()
        // Cek apakah cart ada isi saat fragment dibuka
        lifecycleScope.launch {
            val cartItems = cartService.getAllCartItemsWithIds()
//            if (cartItems.isNotEmpty()) {
//                binding.btnCheckout.visibility = View.VISIBLE
//                binding.btnCheckout.text = "Checkout (${cartItems.size} items)"
//            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel() // Pastikan job dibatalkan saat fragment dihancurkan
        _binding = null
    }
}