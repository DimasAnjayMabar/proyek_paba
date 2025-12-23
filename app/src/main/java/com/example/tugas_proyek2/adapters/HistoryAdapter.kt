package com.example.tugas_proyek2.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tugas_proyek2.data_class.DcTransaksi
import com.example.tugas_proyek2.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

// Perhatikan penambahan parameter 'onItemClick' di constructor
class HistoryAdapter(
    private val historyList: List<DcTransaksi>,
    private val onItemClick: (DcTransaksi) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaksi: DcTransaksi) {
            // Event Click
            binding.root.setOnClickListener {
                onItemClick(transaksi)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val transaksi = historyList[position]
        holder.bind(transaksi) // Panggil fungsi bind untuk set listener

        // Format Tanggal
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val date = transaksi.timestamp?.toDate()
        holder.binding.tvTanggal.text = if (date != null) sdf.format(date) else "-"

        // Format Total
        holder.binding.tvTotal.text = "Rp ${String.format("%,d", transaksi.total_harga).replace(",", ".")}"

        // Ringkasan Items
        val summary = transaksi.items.joinToString(", ") { item ->
            val jumlah = item.jumlah.toString().toIntOrNull() ?: 0
            "${item.nama_produk} ($jumlah)"
        }
        holder.binding.tvSummaryItems.text = summary
    }

    override fun getItemCount() = historyList.size
}