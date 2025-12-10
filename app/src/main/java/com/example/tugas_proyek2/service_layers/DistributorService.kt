package com.example.tugas_proyek2.service_layers

import android.content.ContentValues.TAG
import android.util.Log
import com.example.tugas_proyek2.data_class.DcDistributor
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.services.ProdukService
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class DistributorService {
    private val db = Firebase.firestore

//    suspend fun getAllDistributor(): List<DcDistributor>{
//        return try {
//            Log.d(TAG, "Mengambil data dari koleksi 'distributor'...")
//
//            val snapshot = db.collection("distributor")
//                .get()
//                .await()
//
//            Log.d(TAG, "Jumlah dokumen: ${snapshot.documents.size}")
//
//            val distributors = snapshot.documents.mapNotNull { document ->
//                Log.d(TAG, "Document ID: ${document.id}")
//                Log.d(TAG, "Document Data: ${document.data}")
//
//                val distributor = document.toObject(DcDistributor::class.java)
//                if (distributor == null) {
//                    Log.e(TAG, "Gagal konversi document ke DcDistributor")
//                } else {
//                    Log.d(TAG, "Distributor berhasil diambil: ${distributor.nama}")
//                }
//                distributor
//            }
//
//            Log.d(TAG, "Total distributor setelah mapping: ${distributors.size}")
//            distributors
//        }catch(e: Exception){
//            Log.e(TAG, "Error mengambil distributor: ${e.message}", e)
//            emptyList()
//        }
//    }

    suspend fun getAllDistributors(): Map<String, DcDistributor> {
        return try {
            val snapshot = db.collection("distributor")
                .get()
                .await()

            snapshot.documents.associate { document ->
                document.id to (document.toObject(DcDistributor::class.java) ?: DcDistributor())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting distributors: ${e.message}", e)
            emptyMap()
        }
    }

    suspend fun getDistributorById(id: String): DcDistributor? {
        return try {
            val document = db.collection("distributor").document(id).get().await()
            document.toObject(DcDistributor::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting distributor by ID: ${e.message}", e)
            null
        }
    }

    suspend fun addDistributor(distributor: DcDistributor): String? {
        return try {
            val documentRef = db.collection("distributor").add(distributor).await()
            documentRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding distributor: ${e.message}", e)
            null
        }
    }

    suspend fun updateDistributor(id: String, distributor: DcDistributor): Boolean {
        return try {
            db.collection("distributor").document(id)
                .set(distributor)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating distributor: ${e.message}", e)
            false
        }
    }

    suspend fun deleteDistributor(id: String): Boolean {
        return try {
            db.collection("distributor").document(id)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting distributor: ${e.message}", e)
            false
        }
    }

    suspend fun distributorExists(distributorId: String): Boolean {
        return try {
            val doc = db.collection("distributor").document(distributorId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllDistributorsWithIds(): Map<String, DcDistributor> {
        return try {
            val snapshot = db.collection("distributor")
                .get()
                .await()

            snapshot.documents.associate { document ->
                document.id to (document.toObject(DcDistributor::class.java) ?: DcDistributor())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting distributors with IDs: ${e.message}", e)
            emptyMap()
        }
    }

    companion object {
        private const val TAG = "DistributorService"
    }
}