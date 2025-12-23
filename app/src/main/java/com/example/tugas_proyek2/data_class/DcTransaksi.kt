package com.example.tugas_proyek2.data_class

import com.google.firebase.Timestamp

data class DcTransaksi(
    var id: String? = null,
    var timestamp: Timestamp? = null,
    var total_harga: Long? = 0,
    var items: List<DcItemTransaksi> = listOf()
)

data class DcItemTransaksi(
    var id_produk: String? = null,
    var nama_produk: String? = null,
    var harga_satuan: Long? = 0,
    var jumlah: Int? = 0,
    var subtotal: Long? = 0
)