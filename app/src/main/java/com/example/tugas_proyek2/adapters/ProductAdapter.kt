package com.example.tugas_proyek2.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.data_class.DcProduk
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val productList: List<DcProduk>,
    private val onItemClick: ((DcProduk) -> Unit)? = null  // Optional click listener
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageProduct: ImageView = itemView.findViewById(R.id.image_product)
        val textName: TextView = itemView.findViewById(R.id.text_product_name)
        val textPrice: TextView = itemView.findViewById(R.id.text_product_price)
        val textStock: TextView = itemView.findViewById(R.id.text_product_stok)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        // Debug log
        Log.d("ProductAdapter", "Binding product: ${product.nama}")

        // Load image from Cloudinary using Glide
        product.image?.let { imageUrl ->
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.error)
                .centerCrop()
                .into(holder.imageProduct)
        } ?: run {
            holder.imageProduct.setImageResource(R.drawable.placeholder)
        }

        // Set product name
        holder.textName.text = product.nama ?: "Produk"

        // Format harga jual to Indonesian Rupiah
        val hargaJual = product.harga_jual ?: 0L
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        holder.textPrice.text = formatter.format(hargaJual)

        holder.textStock.text = product.stok.toString()

        // Add click listener if provided
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(product)
        }
    }

    override fun getItemCount(): Int = productList.size
}