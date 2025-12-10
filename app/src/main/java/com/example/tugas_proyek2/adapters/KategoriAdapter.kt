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
import com.example.tugas_proyek2.data_class.DcDistributor
import com.example.tugas_proyek2.data_class.DcKategori
import com.example.tugas_proyek2.data_class.DcProduk
import java.text.NumberFormat
import java.util.Locale

class KategoriAdapter(
    private val kategoriList: List<DcKategori>,
    private val onItemClick: ((DcKategori) -> Unit)? = null  // Optional click listener
) : RecyclerView.Adapter<KategoriAdapter.KategoriViewHolder>() {

    class KategoriViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.text_kategori_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KategoriViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kategori, parent, false)
        return KategoriViewHolder(view)
    }

    override fun onBindViewHolder(holder: KategoriViewHolder, position: Int) {
        val kategori = kategoriList[position]

        // Debug log
        Log.d("KategoriAdapter", "Binding kategori: ${kategori.nama}")

        // Set product name
        holder.textName.text = kategori.nama ?: "Kategori"

        // Add click listener if provided
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(kategori)
        }
    }

    override fun getItemCount(): Int = kategoriList.size
}