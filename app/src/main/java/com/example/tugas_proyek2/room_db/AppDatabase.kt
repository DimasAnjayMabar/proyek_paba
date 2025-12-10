package com.example.tugas_proyek2.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tugas_proyek2.dao.DistributorBackupDao
import com.example.tugas_proyek2.dao.KategoriBackupDao
import com.example.tugas_proyek2.dao.ProdukBackupDao
import com.example.tugas_proyek2.data_class.DistributorBackup
import com.example.tugas_proyek2.data_class.KategoriBackup
import com.example.tugas_proyek2.data_class.ProdukBackup

@Database(
    entities = [
        ProdukBackup::class,
        DistributorBackup::class,
        KategoriBackup::class
    ],
    version = 2,  // Mulai dari versi 1
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produkBackupDao(): ProdukBackupDao
    abstract fun distributorBackupDao(): DistributorBackupDao
    abstract fun kategoriBackupDao(): KategoriBackupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}