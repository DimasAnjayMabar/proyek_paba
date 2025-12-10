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
import java.text.NumberFormat
import java.util.Locale

class DistributorAdapter(
    private val distributorList: List<DcDistributor>,
    private val onItemClick: ((DcDistributor) -> Unit)? = null  // Optional click listener
) : RecyclerView.Adapter<DistributorAdapter.DistributorViewHolder>() {

    class DistributorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.text_distributor_name)
        val textEmail: TextView = itemView.findViewById(R.id.text_distributor_email)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DistributorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_distributor, parent, false)
        return DistributorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DistributorViewHolder, position: Int) {
        val distributor = distributorList[position]

        // Debug log
        Log.d("DistributorAdapter", "Binding distributor: ${distributor.nama}")

        // Set product name
        holder.textName.text = distributor.nama ?: "Distributor"

        holder.textEmail.text = distributor.email ?: "distributor@gmail.com"

        // Add click listener if provided
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(distributor)
        }
    }

    override fun getItemCount(): Int = distributorList.size
}