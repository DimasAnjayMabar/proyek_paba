package com.example.tugas_proyek2.service_layers

import android.util.Log
import com.example.tugas_proyek2.data_class.DcKategori
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class KategoriService {
    private val db = Firebase.firestore

    suspend fun getAllKategoris(): Map<String, DcKategori> {
        return try {
            val snapshot = db.collection("kategori")
                .get()
                .await()

            snapshot.documents.associate { document ->
                document.id to (document.toObject(DcKategori::class.java) ?: DcKategori())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting kategoris: ${e.message}", e)
            emptyMap()
        }
    }

    suspend fun getKategoriById(id: String): DcKategori?{
        return try{
            val document = db.collection("kategori").document(id).get().await()
            document.toObject(DcKategori::class.java)
        }catch(e: Exception){
            Log.e(TAG, "Error getting kategori by ID: ${e.message}", e)
            null
        }
    }

    suspend fun addKategori(kategori: DcKategori): String? {
        return try {
            val documentRef = db.collection("kategori").add(kategori).await()
            documentRef.id
        }catch(e: Exception){
            Log.e(TAG, "Error adding kategori: ${e.message}", e)
            null
        }
    }

    suspend fun updateKategori(id: String, kategori: DcKategori): Boolean{
        return try{
            db.collection("kategori").document(id)
                .set(kategori)
                .await()
            true
        }catch(e: Exception){
            Log.e(TAG, "Error updating kategori: ${e.message}", e)
            false
        }
    }

    suspend fun deleteKategori(id: String): Boolean{
        return try{
            db.collection("kategori").document(id)
                .delete()
                .await()
            true
        }catch(e: Exception){
            Log.e(TAG, "Error deleting kategori: ${e.message}", e)
            false
        }
    }

    suspend fun kategoriExists(id: String): Boolean {
        return try{
            val doc = db.collection("kategori").document(id).get().await()
            doc.exists()
        }catch(e: Exception){
            false
        }
    }

    suspend fun getAllKategorisWithIds(): Map<String, DcKategori> {
        return try{
            val snapshot = db.collection("kategori")
                .get()
                .await()

            snapshot.documents.associate {
                document ->
                document.id to (document.toObject(DcKategori::class.java) ?: DcKategori())
            }
        }catch (e: Exception){
            Log.e(TAG, "Error getting kategoris with IDs: ${e.message}", e)
            emptyMap()
        }
    }

    companion object {
        private const val TAG = "KategoriService"
    }
}