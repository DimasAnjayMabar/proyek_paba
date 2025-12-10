package com.example.tugas_proyek2.service_layers

import com.example.tugas_proyek2.data_class.DcDistributor
import com.example.tugas_proyek2.data_class.DistributorBackup
import com.example.tugas_proyek2.database.AppDatabase
import com.google.api.Context
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class DistributorBackupRepository(context: android.content.Context) {
    private val dao = AppDatabase.getDatabase(context).distributorBackupDao()

    suspend fun backupDistributor(firestoreId: String, distributor: DcDistributor) {
        val backup = DistributorBackup(
            firestoreId = firestoreId,
            nama = distributor.nama,
            email = distributor.email
        )
        dao.insertBackup(backup)

        val oneDayAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
        dao.deleteOldBackups(oneDayAgo)
    }

    suspend fun getBackupDistributor(firestoreId: String): DcDistributor?{
        val backup = dao.getBackup(firestoreId)
        return backup?.let {
            DcDistributor(
                nama = it.nama,
                email = it.email
            )
        }
    }

    suspend fun deleteBackup(firestoreId: String) {
        dao.deleteBackup(firestoreId)
    }

    fun getAllBackups(): Flow<List<DistributorBackup>> {
        return dao.getAllBackups()
    }
}