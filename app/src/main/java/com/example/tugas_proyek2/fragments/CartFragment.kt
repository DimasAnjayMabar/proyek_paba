package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.data_class.DcCart
import com.example.tugas_proyek2.data_class.DcItemTransaksi
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.data_class.DcTransaksi
import com.example.tugas_proyek2.databinding.FragmentCartBinding
import com.example.tugas_proyek2.service_layers.CartService
import com.example.tugas_proyek2.services.ProdukService
import com.example.tugas_proyek2.services.TransaksiService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val cartService = CartService()
    private val produkService = ProdukService()
    private val transaksiService = TransaksiService() // Service baru

    private lateinit var cartAdapter: CartAdapter
    private val cartItemsWithDetails = mutableListOf<CartItemWithDetails>()
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        setupListeners()
        loadCartData(showLoading = true)
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(cartItemsWithDetails) { cartId, action, newQuantity ->
            when (action) {
                "increase" -> updateQuantity(cartId, newQuantity + 1)
                "decrease" -> updateQuantity(cartId, newQuantity - 1)
                "remove" -> removeFromCart(cartId)
            }
        }
        binding.recyclerViewCart.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshCart.setColorSchemeResources(
            R.color.purple_500, R.color.blue_500, R.color.green_500
        )
        binding.swipeRefreshCart.setOnRefreshListener {
            loadCartData(showLoading = false)
        }
    }

    private fun setupListeners() {
        binding.btnCheckout.setOnClickListener {
            if (cartItemsWithDetails.isNotEmpty()) {
                showCheckoutConfirmationDialog()
            }
        }
    }

    private fun loadCartData(showLoading: Boolean = true) {
        if (isLoading) return
        isLoading = true
        if (showLoading) binding.swipeRefreshCart.isRefreshing = true

        lifecycleScope.launch {
            try {
                cartItemsWithDetails.clear()
                val cartItemsMap = cartService.getAllCartItemsWithIds()

                if (cartItemsMap.isEmpty()) {
                    showEmptyCart()
                    return@launch
                }

                for ((cartId, cartItem) in cartItemsMap) {
                    val produk = produkService.getProductById(cartItem.produk_id)
                    if (produk != null) {
                        cartItemsWithDetails.add(
                            CartItemWithDetails(cartId, cartItem, produk)
                        )
                    }
                }

                if (cartItemsWithDetails.isEmpty()) {
                    showEmptyCart()
                } else {
                    showCartItems()
                    updateGrandTotal()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyCart()
            } finally {
                isLoading = false
                binding.swipeRefreshCart.isRefreshing = false
            }
        }
    }

    private fun safeRefreshCart() {
        if (!isLoading) loadCartData(showLoading = false)
    }

    private fun updateQuantity(cartId: String, newQuantity: Long) {
        lifecycleScope.launch {
            try {
                if (newQuantity <= 0) {
                    removeFromCart(cartId)
                } else {
                    val success = cartService.updateCartItemQuantity(cartId, newQuantity)
                    if (success) safeRefreshCart()
                    else Toast.makeText(requireContext(), "Gagal update jumlah", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeFromCart(cartId: String) {
        lifecycleScope.launch {
            try {
                val success = cartService.removeFromCart(cartId)
                if (success) {
                    val index = cartItemsWithDetails.indexOfFirst { it.cartId == cartId }
                    if (index != -1) {
                        cartItemsWithDetails.removeAt(index)
                        cartAdapter.notifyItemRemoved(index)
                    }
                    if (cartItemsWithDetails.isEmpty()) showEmptyCart()
                    else updateGrandTotal()
                    Toast.makeText(requireContext(), "Produk dihapus", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun validateStockBeforeCheckout(): Boolean {
        return try {
            var isValid = true
            val errorMessages = mutableListOf<String>()

            for (item in cartItemsWithDetails) {
                val produk = produkService.getProductById(item.cartItem.produk_id)
                if (produk != null) {
                    val stokTersedia = produk.stok ?: 0L
                    val jumlahDipesan = item.cartItem.jumlah ?: 0L

                    if (jumlahDipesan > stokTersedia) {
                        isValid = false
                        errorMessages.add(
                            "${item.produk.nama ?: "Produk"}: " +
                                    "Stok tersedia: $stokTersedia, " +
                                    "Dipesan: $jumlahDipesan"
                        )
                    }
                } else {
                    isValid = false
                    errorMessages.add("Produk ${item.produk.nama} tidak ditemukan")
                }
            }

            if (!isValid) {
                // Tampilkan dialog error
                showStockValidationErrorDialog(errorMessages)
            }

            isValid
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error validasi stok: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun showStockValidationErrorDialog(errorMessages: List<String>) {
        val errorMessage = StringBuilder()
        errorMessage.append("Stok tidak mencukupi untuk produk berikut:\n\n")

        errorMessages.forEachIndexed { index, message ->
            errorMessage.append("${index + 1}. $message\n")
        }

        errorMessage.append("\nSilahkan kurangi jumlah pesanan atau hapus produk dari keranjang.")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Stok Tidak Cukup")
            .setMessage(errorMessage.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showCheckoutConfirmationDialog() {
        val grandTotal = calculateGrandTotal()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Checkout")
            .setMessage("""
            Konfirmasi Pembelian:
            
            ${cartItemsWithDetails.size} jenis produk
            Total Biaya: Rp ${formatRupiah(grandTotal)}
            
            Lanjutkan proses pembayaran?
        """.trimIndent())
            .setPositiveButton("Bayar") { dialog, _ ->
                dialog.dismiss()
                // Panggil validasi stok sebelum checkout
                validateStockAndCheckout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun validateStockAndCheckout() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshCart.isRefreshing = true

                // 1. Validasi stok sebelum melanjutkan
                val isStockValid = validateStockBeforeCheckout()
                if (!isStockValid) {
                    binding.swipeRefreshCart.isRefreshing = false
                    return@launch
                }

                // 2. Jika stok valid, lanjutkan proses checkout
                processCheckoutAfterValidation()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefreshCart.isRefreshing = false
            }
        }
    }

    private fun processCheckoutAfterValidation() {
        lifecycleScope.launch {
            try {
                // === PERSIAPAN DATA HISTORY ===
                val grandTotal = calculateGrandTotal()
                val detailItems = cartItemsWithDetails.map { item ->
                    val hargaSatuan = item.produk.harga_jual?.toString()?.toLongOrNull() ?: 0L
                    val jumlah = item.cartItem.jumlah?.toString()?.toIntOrNull() ?: 0
                    val subtotal = item.cartItem.subtotal?.toString()?.toLongOrNull() ?: 0L

                    DcItemTransaksi(
                        id_produk = item.cartItem.produk_id,
                        nama_produk = item.produk.nama,
                        harga_satuan = hargaSatuan,
                        jumlah = jumlah,
                        subtotal = subtotal
                    )
                }

                val transaksiBaru = DcTransaksi(
                    timestamp = Timestamp.now(),
                    total_harga = grandTotal,
                    items = detailItems
                )
                // ==============================

                // 2. Update stok (sekarang stok sudah pasti cukup)
                val updateStockJobs = cartItemsWithDetails.map { item ->
                    launch {
                        val currentProduk = produkService.getProductById(item.cartItem.produk_id)
                        if (currentProduk != null) {
                            val stokSekarang = currentProduk.stok?.toString()?.toLongOrNull() ?: 0L
                            val jumlahBeli = item.cartItem.jumlah ?: 0L

                            val newStock = stokSekarang - jumlahBeli
                            // Validasi tambahan untuk memastikan stok tidak negatif
                            if (newStock >= 0) {
                                val updatedProduk = currentProduk.copy(stok = newStock)
                                produkService.updateProduct(item.cartItem.produk_id, updatedProduk)
                            } else {
                                Log.e("Checkout", "Stok menjadi negatif untuk produk: ${item.produk.nama}")
                            }
                        }
                    }
                }
                updateStockJobs.forEach { it.join() }

                // 3. SIMPAN KE FIREBASE
                val isSaved = transaksiService.addTransaksi(transaksiBaru)

                if (isSaved) {
                    // 4. Kosongkan Cart hanya jika simpan history berhasil
                    val clearSuccess = cartService.clearCart()
                    if (clearSuccess) {
                        cartItemsWithDetails.clear()
                        cartAdapter.notifyDataSetChanged()
                        showEmptyCart()
                        Toast.makeText(requireContext(), "Checkout Berhasil!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal menyimpan riwayat transaksi", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshCart.isRefreshing = false
            }
        }
    }

    private fun calculateGrandTotal(): Long = cartItemsWithDetails.sumOf { it.cartItem.subtotal ?: 0L }
    private fun updateGrandTotal() { binding.textGrandTotal.text = "Rp ${formatRupiah(calculateGrandTotal())}" }
    private fun showEmptyCart() {
        binding.swipeRefreshCart.visibility = View.GONE
        binding.textEmptyCart.visibility = View.VISIBLE
        binding.layoutCartFooter.visibility = View.GONE
        binding.btnCheckout.visibility = View.GONE
    }
    private fun showCartItems() {
        binding.swipeRefreshCart.visibility = View.VISIBLE
        binding.textEmptyCart.visibility = View.GONE
        binding.layoutCartFooter.visibility = View.VISIBLE
        binding.btnCheckout.visibility = View.VISIBLE
        cartAdapter.notifyDataSetChanged()
    }
    private fun formatRupiah(amount: Long): String = String.format("%,d", amount).replace(",", ".")

    override fun onResume() { super.onResume(); safeRefreshCart() }
    override fun onPause() { super.onPause(); isLoading = false }
    override fun onDestroyView() { super.onDestroyView(); cartItemsWithDetails.clear(); _binding = null }

    data class CartItemWithDetails(
        val cartId: String,
        val cartItem: DcCart,
        val produk: DcProduk
    )
}