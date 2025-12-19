package com.example.tugas_proyek2.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.data_class.DcProduk

class CartAdapter(
    private val items: List<CartFragment.CartItemWithDetails>,
    private val onItemAction: (cartId: String, action: String, currentQuantity: Long) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageProduct: ImageView = itemView.findViewById(R.id.imageCartProduct)
        val textProductName: TextView = itemView.findViewById(R.id.textCartProductName)
        val textPrice: TextView = itemView.findViewById(R.id.textCartPrice)
        val textQuantity: TextView = itemView.findViewById(R.id.textCartQuantity)
        val textSubtotal: TextView = itemView.findViewById(R.id.textCartSubtotal)
        val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
        val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemoveFromCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = items[position]

        // Set data produk
        holder.textProductName.text = item.produk.nama ?: "Produk"
        holder.textPrice.text = "Rp ${formatRupiah(item.produk.harga_jual ?: 0L)}"
        holder.textQuantity.text = item.cartItem.jumlah.toString()
        holder.textSubtotal.text = "Rp ${formatRupiah(item.cartItem.subtotal ?: 0L)}"

        // Load gambar jika ada
        if (!item.produk.image.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.produk.image)
                .placeholder(R.drawable.error)
                .into(holder.imageProduct)
        }

        // Tombol increase
        holder.btnIncrease.setOnClickListener {
            val currentQty = item.cartItem.jumlah ?: 1L
            onItemAction(item.cartId, "increase", currentQty)
        }

        // Tombol decrease
        holder.btnDecrease.setOnClickListener {
            val currentQty = item.cartItem.jumlah ?: 1L
            onItemAction(item.cartId, "decrease", currentQty)
        }

        // Tombol remove
        holder.btnRemove.setOnClickListener {
            val currentQty = item.cartItem.jumlah ?: 1L
            onItemAction(item.cartId, "remove", currentQty)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun formatRupiah(amount: Long): String {
        return String.format("%,d", amount).replace(",", ".")
    }
}