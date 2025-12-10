package com.example.tugas_proyek2.services

import android.util.Log
import com.example.tugas_proyek2.data_class.DcProduk
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class ProdukService {
    private val db = Firebase.firestore

    // READ: Get product by ID
    suspend fun getProductById(id: String): DcProduk? {
        return try {
            val document = db.collection("produk").document(id).get().await()
            document.toObject(DcProduk::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting product by ID: ${e.message}", e)
            null
        }
    }

    // READ: Get all products with document IDs
    suspend fun getAllProductsWithIds(): Map<String, DcProduk> {
        return try {
            val snapshot = db.collection("produk")
                .get()
                .await()

            snapshot.documents.associate { document ->
                document.id to (document.toObject(DcProduk::class.java) ?: DcProduk())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting products with IDs: ${e.message}", e)
            emptyMap()
        }
    }

    // READ: Get all products
    suspend fun getAllProducts(): List<DcProduk> {
        return try {
            val snapshot = db.collection("produk")
                .get()
                .await()

            snapshot.documents.mapNotNull { document ->
                document.toObject(DcProduk::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all products: ${e.message}", e)
            emptyList()
        }
    }

    // CREATE: Add new product
    suspend fun addProduct(produk: DcProduk): String? {
        return try {
            val documentRef = db.collection("produk").add(produk).await()
            documentRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error adding product: ${e.message}", e)
            null
        }
    }

    // UPDATE: Update existing product
    suspend fun updateProduct(id: String, produk: DcProduk): Boolean {
        return try {
            db.collection("produk").document(id)
                .set(produk)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product: ${e.message}", e)
            false
        }
    }

    // DELETE: Delete product
    suspend fun deleteProduct(id: String): Boolean {
        return try {
            db.collection("produk").document(id)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product: ${e.message}", e)
            false
        }
    }

    suspend fun productExists(productId: String): Boolean {
        return try {
            val doc = db.collection("produk").document(productId).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "ProdukService"
    }
}