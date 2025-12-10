package com.example.tugas_proyek2.data_class

import com.google.firebase.Timestamp

data class DcProduk(
    var nama: String? = "",
    var harga_beli: Long? = 0L,          // Ganti Number menjadi Long
    var persentase_keuntungan: Double? = 0.0, // Ganti Number menjadi Double
    var harga_jual: Long? = 0L,          // Ganti Number menjadi Long
    var stok: Long? = 0L,                // Ganti Number menjadi Long
    var kategori_id: String? = "",       // Sesuai field di Firestore
    var distributor_id: String? = "",    // Sesuai field di Firestore
    var tanggal: Timestamp? = null,
    var image: String? = ""
)