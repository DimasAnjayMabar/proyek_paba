package com.example.tugas_proyek2.data_class

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "distributor_backup")
data class DistributorBackup(
    @PrimaryKey
    val firestoreId: String,
    val nama: String? = "",
    val email: String? = "",
    val deletedAt: Long = System.currentTimeMillis()
)
