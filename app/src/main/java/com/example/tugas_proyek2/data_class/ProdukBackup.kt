package com.example.tugas_proyek2.data_class

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produk_backup")
data class ProdukBackup(
    @PrimaryKey
    val firestoreId: String,

    // Simpan field secara terpisah (tanpa DcProduk)
    val nama: String? = "",
    val harga_beli: Long? = 0L,
    val persentase_keuntungan: Double? = 0.0,
    val harga_jual: Long? = 0L,
    val stok: Long? = 0L,
    val kategori_id: String? = "",
    val distributor_id: String? = "",
    val tanggal: Long? = null,  // Simpan sebagai Long
    val image: String? = "",

    val deletedAt: Long = System.currentTimeMillis()
)