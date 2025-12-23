package com.example.tugas_proyek2.services

import android.util.Log
import com.example.tugas_proyek2.data_class.DcTransaksi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class TransaksiService {
    private val db = FirebaseFirestore.getInstance()
    private val collectionRef = db.collection("transaksi")

    // Fungsi untuk menyimpan transaksi baru
    suspend fun addTransaksi(transaksi: DcTransaksi): Boolean {
        return try {
            // Biarkan Firestore membuat ID unik secara otomatis
            val docRef = collectionRef.document()
            transaksi.id = docRef.id

            docRef.set(transaksi).await()
            true
        } catch (e: Exception) {
            Log.e("TransaksiService", "Error adding transaction", e)
            false
        }
    }

    // Fungsi untuk mengambil semua history (diurutkan dari yang terbaru)
    suspend fun getAllHistory(): List<DcTransaksi> {
        return try {
            val snapshot = collectionRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(DcTransaksi::class.java)
        } catch (e: Exception) {
            Log.e("TransaksiService", "Error getting history", e)
            emptyList()
        }
    }
}