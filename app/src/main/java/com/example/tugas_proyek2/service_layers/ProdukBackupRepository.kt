package com.example.tugas_proyek2.repository

import android.content.Context
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.data_class.ProdukBackup
import com.example.tugas_proyek2.database.AppDatabase
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class ProdukBackupRepository(context: Context) {

    private val dao = AppDatabase.getDatabase(context).produkBackupDao()

    suspend fun backupProduk(firestoreId: String, produk: DcProduk) {
        val backup = ProdukBackup(
            firestoreId = firestoreId,
            nama = produk.nama,
            harga_beli = produk.harga_beli,
            persentase_keuntungan = produk.persentase_keuntungan,
            harga_jual = produk.harga_jual,
            stok = produk.stok,
            kategori_id = produk.kategori_id,
            distributor_id = produk.distributor_id,
            tanggal = produk.tanggal?.toDate()?.time,
            image = produk.image
        )
        dao.insertBackup(backup)

        val oneDayAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        dao.deleteOldBackups(oneDayAgo)
    }

    suspend fun getBackupProduk(firestoreId: String): DcProduk? {
        val backup = dao.getBackup(firestoreId)
        return backup?.let {
            DcProduk(
                nama = it.nama,
                harga_beli = it.harga_beli,
                persentase_keuntungan = it.persentase_keuntungan,
                harga_jual = it.harga_jual,
                stok = it.stok,
                kategori_id = it.kategori_id,
                distributor_id = it.distributor_id,
                tanggal = it.tanggal?.let { time ->
                    val date = java.util.Date(time)
                    Timestamp(date)
                },
                image = it.image
            )
        }
    }

    suspend fun deleteBackup(firestoreId: String) {
        dao.deleteBackup(firestoreId)
    }

    fun getAllBackups(): Flow<List<ProdukBackup>> {
        return dao.getAllBackups()
    }
}