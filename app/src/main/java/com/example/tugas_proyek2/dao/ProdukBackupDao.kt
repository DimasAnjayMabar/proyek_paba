package com.example.tugas_proyek2.dao

import androidx.room.*
import com.example.tugas_proyek2.data_class.ProdukBackup
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdukBackupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: ProdukBackup)

    @Query("SELECT * FROM produk_backup WHERE firestoreId = :firestoreId")
    suspend fun getBackup(firestoreId: String): ProdukBackup?

    @Query("DELETE FROM produk_backup WHERE firestoreId = :firestoreId")
    suspend fun deleteBackup(firestoreId: String)

    @Query("SELECT * FROM produk_backup ORDER BY deletedAt DESC")
    fun getAllBackups(): Flow<List<ProdukBackup>>

    @Query("DELETE FROM produk_backup WHERE deletedAt < :timestamp")
    suspend fun deleteOldBackups(timestamp: Long)
}