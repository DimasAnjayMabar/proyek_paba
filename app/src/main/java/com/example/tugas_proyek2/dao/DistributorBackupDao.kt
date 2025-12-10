package com.example.tugas_proyek2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tugas_proyek2.data_class.DistributorBackup
import kotlinx.coroutines.flow.Flow

@Dao
interface DistributorBackupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: DistributorBackup)

    @Query("SELECT * FROM distributor_backup WHERE firestoreId = :firestoreId")
    suspend fun getBackup(firestoreId: String): DistributorBackup?

    @Query("DELETE FROM distributor_backup WHERE firestoreId = :firestoreId")
    suspend fun deleteBackup(firestoreId: String)

    @Query("SELECT * FROM distributor_backup ORDER BY deletedAt DESC")
    fun getAllBackups(): Flow<List<DistributorBackup>>

    @Query("DELETE FROM distributor_backup WHERE deletedAt < :timestamp")
    suspend fun deleteOldBackups(timestamp: Long)
}