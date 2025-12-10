package com.example.tugas_proyek2.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tugas_proyek2.data_class.KategoriBackup
import kotlinx.coroutines.flow.Flow

@Dao
interface KategoriBackupDao {
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: KategoriBackup)

    @Query("SELECT * FROM kategori_backup WHERE firestoreId = :firestoreId")
    suspend fun getBackup(firestoreId: String): KategoriBackup?

    @Query("DELETE FROM kategori_backup WHERE firestoreId = :firestoreId")
    suspend fun deleteBackup(firestoreId: String)

    @Query("SELECT * FROM kategori_backup ORDER BY deletedAt DESC")
    fun getAllBackups(): Flow<List<KategoriBackup>>

    @Query("DELETE FROM kategori_backup WHERE deletedAt < :timestamp")
    suspend fun deleteOldBackups(timestamp: Long)
}