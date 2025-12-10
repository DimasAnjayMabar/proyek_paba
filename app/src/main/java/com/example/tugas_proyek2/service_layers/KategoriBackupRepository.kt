package com.example.tugas_proyek2.service_layers

import com.example.tugas_proyek2.data_class.DcKategori
import com.example.tugas_proyek2.data_class.KategoriBackup
import com.example.tugas_proyek2.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class KategoriBackupRepository(context: android.content.Context) {
    private val dao = AppDatabase.getDatabase(context).kategoriBackupDao()

    suspend fun backupKategori(firestoreId: String, kategori: DcKategori) {
        val backup = KategoriBackup(
            firestoreId = firestoreId,
            nama = kategori.nama
        )
        dao.insertBackup(backup)

        val oneDayAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        dao.deleteOldBackups(oneDayAgo)
    }

    suspend fun getBackupKategori(firestoreId: String): DcKategori? {
        val backup = dao.getBackup(firestoreId)
        return backup?.let {
            DcKategori(
                nama = it.nama
            )
        }
    }

    suspend fun deleteBackup(firestoreId: String) {
        dao.deleteBackup(firestoreId)
    }

    fun getAllBackups(): Flow<List<KategoriBackup>> {
        return dao.getAllBackups()

    }
}