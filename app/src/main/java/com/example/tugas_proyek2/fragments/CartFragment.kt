package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.data_class.DcCart
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.databinding.FragmentCartBinding
import com.example.tugas_proyek2.service_layers.CartService
import com.example.tugas_proyek2.services.ProdukService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val cartService = CartService()
    private val produkService = ProdukService()

    private lateinit var cartAdapter: CartAdapter
    private val cartItemsWithDetails = mutableListOf<CartItemWithDetails>()

    // Flag untuk mencegah multiple loading
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
            setHasFixedSize(true) // Tambahkan ini untuk performance
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshCart.setColorSchemeResources(
            R.color.purple_500,
            R.color.blue_500,
            R.color.green_500
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
        // Cegah multiple loading
        if (isLoading) return

        isLoading = true

        if (showLoading) {
            binding.swipeRefreshCart.isRefreshing = true
        }

        lifecycleScope.launch {
            try {
                // CLEAR LIST SEBELUM LOAD DATA BARU
                cartItemsWithDetails.clear()

                // Get semua item cart dengan ID
                val cartItemsMap = cartService.getAllCartItemsWithIds()

                if (cartItemsMap.isEmpty()) {
                    showEmptyCart()
                    return@launch
                }

                // Untuk setiap cart item, get detail produk
                for ((cartId, cartItem) in cartItemsMap) {
                    val produk = produkService.getProductById(cartItem.produk_id)
                    if (produk != null) {
                        cartItemsWithDetails.add(
                            CartItemWithDetails(
                                cartId = cartId,
                                cartItem = cartItem,
                                produk = produk
                            )
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

    // Fungsi refresh yang lebih aman (gunakan ini di onResume)
    private fun safeRefreshCart() {
        if (!isLoading) {
            loadCartData(showLoading = false)
        }
    }

    private fun updateQuantity(cartId: String, newQuantity: Long) {
        lifecycleScope.launch {
            try {
                if (newQuantity <= 0) {
                    removeFromCart(cartId)
                } else {
                    val success = cartService.updateCartItemQuantity(cartId, newQuantity)

                    if (success) {
                        // Gunakan safeRefreshCart bukan loadCartData langsung
                        safeRefreshCart()
                    } else {
                        Toast.makeText(requireContext(), "Gagal mengupdate jumlah", Toast.LENGTH_SHORT).show()
                    }
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
                    // Hapus dari list lokal
                    val index = cartItemsWithDetails.indexOfFirst { it.cartId == cartId }
                    if (index != -1) {
                        cartItemsWithDetails.removeAt(index)
                        cartAdapter.notifyItemRemoved(index)
                    }

                    // Update UI
                    if (cartItemsWithDetails.isEmpty()) {
                        showEmptyCart()
                    } else {
                        updateGrandTotal()
                    }

                    Toast.makeText(requireContext(), "Produk dihapus dari keranjang", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal menghapus produk", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCheckoutConfirmationDialog() {
        val grandTotal = calculateGrandTotal()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Checkout")
            .setMessage("""
                Anda akan melakukan checkout dengan total:
                
                ${cartItemsWithDetails.size} produk
                Grand Total: Rp ${formatRupiah(grandTotal)}
                
                Apakah Anda yakin ingin melanjutkan?
            """.trimIndent())
            .setPositiveButton("Ya, Checkout") { dialog, _ ->
                dialog.dismiss()
                processCheckout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun processCheckout() {
        lifecycleScope.launch {
            try {
                binding.swipeRefreshCart.isRefreshing = true

                // 1. Update stok untuk setiap produk
                val updateStockJobs = cartItemsWithDetails.map { item ->
                    launch {
                        val currentProduk = produkService.getProductById(item.cartItem.produk_id)
                        if (currentProduk != null) {
                            val newStock = (currentProduk.stok ?: 0L) - item.cartItem.jumlah!!
                            val updatedProduk = currentProduk.copy(stok = newStock)
                            produkService.updateProduct(item.cartItem.produk_id, updatedProduk)
                        }
                    }
                }

                updateStockJobs.forEach { it.join() }

                // 2. Kosongkan cart
                val success = cartService.clearCart()

                if (success) {
                    // CLEAR LIST SETELAH CHECKOUT
                    cartItemsWithDetails.clear()
                    cartAdapter.notifyDataSetChanged()
                    showEmptyCart()

                    Toast.makeText(requireContext(), "Checkout berhasil!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal checkout", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefreshCart.isRefreshing = false
            }
        }
    }

    private fun calculateGrandTotal(): Long {
        return cartItemsWithDetails.sumOf { it.cartItem.subtotal ?: 0L }
    }

    private fun updateGrandTotal() {
        val grandTotal = calculateGrandTotal()
        binding.textGrandTotal.text = "Rp ${formatRupiah(grandTotal)}"
    }

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

        // Gunakan notifyDataSetChanged, bukan notifyItemRangeChanged
        cartAdapter.notifyDataSetChanged()
    }

    private fun formatRupiah(amount: Long): String {
        return String.format("%,d", amount).replace(",", ".")
    }

    override fun onResume() {
        super.onResume()
        // Gunakan safeRefreshCart untuk menghindari duplicate
        safeRefreshCart()
    }

    override fun onPause() {
        super.onPause()
        // Reset loading flag
        isLoading = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear list saat fragment dihancurkan
        cartItemsWithDetails.clear()
        _binding = null
    }

    data class CartItemWithDetails(
        val cartId: String,
        val cartItem: DcCart,
        val produk: DcProduk
    )
}