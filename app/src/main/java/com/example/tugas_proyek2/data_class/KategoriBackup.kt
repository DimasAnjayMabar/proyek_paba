package com.example.tugas_proyek2.data_class

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kategori_backup")
data class KategoriBackup(
    @PrimaryKey
    val firestoreId: String,
    val nama: String? = "",
    val deletedAt: Long = System.currentTimeMillis()
)
