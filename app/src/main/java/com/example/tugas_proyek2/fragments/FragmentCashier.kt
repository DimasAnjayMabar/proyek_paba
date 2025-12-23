package com.example.tugas_proyek2.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.adapters.HistoryAdapter
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.data_class.DcTransaksi
import com.example.tugas_proyek2.databinding.FragmentCashierBinding
import com.example.tugas_proyek2.service_layers.CartService
import com.example.tugas_proyek2.services.TransaksiService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class FragmentCashier : Fragment() {

    private var _binding: FragmentCashierBinding? = null
    private val binding get() = _binding!!

    // Service
    private val cartService = CartService()
    private val transaksiService = TransaksiService()

    // Variable Pencarian Produk
    private var currentProduk: Pair<String, DcProduk>? = null
    private var searchJob: Job? = null

    // Variable History
    private val historyList = mutableListOf<DcTransaksi>()
    private lateinit var historyAdapter: HistoryAdapter

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
        setupHistoryRecyclerView()
        setupListeners()
        loadHistoryData() // Load awal
    }

    override fun onResume() {
        super.onResume()
        // Reload history agar data selalu fresh setelah kembali dari halaman lain
        loadHistoryData()
    }

    // ==========================================
    // BAGIAN 1: SETUP RECYCLERVIEW & DIALOG DETAIL
    // ==========================================

    private fun setupHistoryRecyclerView() {
        // Inisialisasi Adapter dengan Click Listener
        historyAdapter = HistoryAdapter(historyList) { transaksi ->
            // Saat item diklik, panggil fungsi showDetailDialog
            showDetailDialog(transaksi)
        }

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun loadHistoryData() {
        // Tampilkan loading hanya jika list masih kosong
        if (historyList.isEmpty()) {
            binding.progressBarHistory.visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                // Ambil data dari Firestore
                val data = transaksiService.getAllHistory()

                historyList.clear()
                historyList.addAll(data)
                historyAdapter.notifyDataSetChanged()

                // Atur visibilitas Empty State
                if (historyList.isEmpty()) {
                    binding.tvEmptyHistory.visibility = View.VISIBLE
                    binding.recyclerViewHistory.visibility = View.GONE
                } else {
                    binding.tvEmptyHistory.visibility = View.GONE
                    binding.recyclerViewHistory.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                binding.tvEmptyHistory.text = "Gagal memuat data."
                binding.tvEmptyHistory.visibility = View.VISIBLE
            } finally {
                binding.progressBarHistory.visibility = View.GONE
            }
        }
    }

    private fun showDetailDialog(transaksi: DcTransaksi) {
        // Inflate Layout Dialog Custom
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_detail_transaksi, null)

        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)

        val dialog = builder.create()
        // Buat background transparan agar sudut rounded CardView terlihat
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // --- Bind Data ke Dialog ---
        val tvDate = dialogView.findViewById<TextView>(R.id.tvDialogDate)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvDialogTotal)
        val containerItems = dialogView.findViewById<LinearLayout>(R.id.containerItems)
        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseDialog)

        // 1. Set Tanggal (Format Indonesia)
        val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))
        val date = transaksi.timestamp?.toDate()
        tvDate.text = if (date != null) sdf.format(date) else "-"

        // 2. Set Grand Total
        tvTotal.text = "Rp ${formatRupiah(transaksi.total_harga ?: 0)}"

        // 3. Set List Barang (Looping dinamis)
        containerItems.removeAllViews() // Bersihkan container dulu

        transaksi.items.forEach { item ->
            // Inflate layout per baris item
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_detail_row, containerItems, false)

            val tvNama = itemView.findViewById<TextView>(R.id.tvRowNama)
            val tvHargaQty = itemView.findViewById<TextView>(R.id.tvRowHargaQty)
            val tvSubtotal = itemView.findViewById<TextView>(R.id.tvRowSubtotal)

            // Casting aman untuk mencegah error tipe data
            val jumlah = item.jumlah.toString().toIntOrNull() ?: 0
            val harga = item.harga_satuan.toString().toLongOrNull() ?: 0L
            val subtotal = item.subtotal.toString().toLongOrNull() ?: 0L

            tvNama.text = item.nama_produk
            tvHargaQty.text = "$jumlah x Rp ${formatRupiah(harga)}"
            tvSubtotal.text = "Rp ${formatRupiah(subtotal)}"

            // Tambahkan ke container
            containerItems.addView(itemView)
        }

        // 4. Tombol Tutup
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // ==========================================
    // BAGIAN 2: LOGIKA KASIR (PENCARIAN & CART)
    // ==========================================

    private fun setupUI() {
        binding.btnAddToCart.isEnabled = false
        binding.btnAddToCart.alpha = 0.5f
    }

    private fun setupListeners() {
        // Listener Pencarian dengan Debounce
        binding.editTextSearchProduct.addTextChangedListener { editable ->
            searchJob?.cancel() // Batalkan pencarian sebelumnya

            val searchText = editable.toString().trim()
            if (searchText.isNotEmpty() && searchText.length >= 2) {
                searchJob = lifecycleScope.launch {
                    delay(500) // Tunggu 500ms setelah user berhenti mengetik
                    searchProduk(searchText)
                }
            } else {
                resetProdukInfo()
            }
        }

        // Tombol Tambah ke Cart
        binding.btnAddToCart.setOnClickListener {
            currentProduk?.let { (produkId, produk) ->
                addProdukToCart(produkId, produk)
            } ?: run {
                showSnackbar("Pilih produk terlebih dahulu")
            }
        }

        // FAB Cart
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
                    currentProduk = produkId to produk
                    updateUIForProdukFound(produk)
                } else {
                    resetProdukInfo()
                    showSnackbar("Produk tidak ditemukan")
                }
            } catch (e: Exception) {
                resetProdukInfo()
            }
        }
    }

    private fun updateUIForProdukFound(produk: DcProduk) {
        binding.btnAddToCart.isEnabled = true
        binding.btnAddToCart.alpha = 1.0f
        binding.btnAddToCart.text = "Tambah ${produk.nama}"

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
                    binding.editTextSearchProduct.text?.clear()
                    resetProdukInfo()
                    showToast("${produk.nama} berhasil masuk keranjang")
                } else {
                    showSnackbar("Gagal menambahkan produk")
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

    // ==========================================
    // BAGIAN 3: UTILITIES
    // ==========================================

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun formatRupiah(amount: Long): String {
        return String.format("%,d", amount).replace(",", ".")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}