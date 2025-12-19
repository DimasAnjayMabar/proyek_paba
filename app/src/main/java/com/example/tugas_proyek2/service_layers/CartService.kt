package com.example.tugas_proyek2.service_layers

import android.util.Log
import com.example.tugas_proyek2.data_class.DcCart
import com.example.tugas_proyek2.data_class.DcProduk
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class CartService {
    private val db = Firebase.firestore

    companion object {
        private const val TAG = "CartService"
        private const val COLLECTION_CART = "cart"
        private const val COLLECTION_PRODUK = "produk"
    }

    /**
     * Cari produk berdasarkan nama (case-insensitive)
     * Mengembalikan Pair(documentId, produk) seperti di ProdukService
     */
    suspend fun searchProdukByName(namaProduk: String): Pair<String, DcProduk>? {
        return try {
            // Gunakan query case-insensitive seperti di contoh
            val snapshot = db.collection(COLLECTION_PRODUK)
                .get()
                .await()

            // Filter di memory seperti di ProdukService.searchProductsByName
            val allProducts = snapshot.documents.map { doc ->
                doc.id to (doc.toObject(DcProduk::class.java) ?: DcProduk())
            }

            val searchTerm = namaProduk.lowercase()
            allProducts.firstOrNull { (_, produk) ->
                produk.nama?.lowercase()?.contains(searchTerm) == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching produk: ${e.message}")
            null
        }
    }

    /**
     * Tambah item ke cart
     * Simpan produk_id sebagai Firestore Document ID
     */
    suspend fun addToCart(produkId: String, produk: DcProduk, jumlah: Long = 1): Boolean {
        return try {
            // Validasi input
            if (produk.harga_jual == null) {
                Log.e(TAG, "Harga jual tidak valid")
                return false
            }

            // Hitung subtotal
            val subtotal = produk.harga_jual!! * jumlah

            // Buat cart item menggunakan Firestore Document ID sebagai produk_id
            val cartItem = DcCart(
                produk_id = produkId, // Gunakan Firestore Document ID
                jumlah = jumlah,
                subtotal = subtotal
            )

            // Simpan ke Firebase dengan auto-generated ID
            db.collection(COLLECTION_CART).add(cartItem).await()

            Log.d(TAG, "Produk berhasil ditambahkan ke cart: ${produk.nama}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to cart: ${e.message}")
            false
        }
    }

    /**
     * Get semua item di cart dengan Firestore Document ID
     */
    suspend fun getAllCartItemsWithIds(): Map<String, DcCart> {
        return try {
            val snapshot = db.collection(COLLECTION_CART)
                .get()
                .await()

            snapshot.documents.associate { document ->
                document.id to (document.toObject(DcCart::class.java) ?: DcCart())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cart items: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Get produk detail untuk cart item
     */
    suspend fun getProdukForCart(produkId: String): DcProduk? {
        return try {
            val document = db.collection(COLLECTION_PRODUK)
                .document(produkId)
                .get()
                .await()

            document.toObject(DcProduk::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting produk for cart: ${e.message}")
            null
        }
    }

    /**
     * Update jumlah item di cart
     */
    suspend fun updateCartItemQuantity(cartId: String, newJumlah: Long): Boolean {
        return try {
            // Get cart item
            val cartDoc = db.collection(COLLECTION_CART).document(cartId).get().await()
            val cartItem = cartDoc.toObject(DcCart::class.java)

            if (cartItem != null) {
                // Get produk untuk mendapatkan harga_jual
                val produk = getProdukForCart(cartItem.produk_id)

                if (produk != null && produk.harga_jual != null) {
                    val newSubtotal = produk.harga_jual!! * newJumlah

                    // Update cart item
                    db.collection(COLLECTION_CART).document(cartId)
                        .update(
                            mapOf(
                                "jumlah" to newJumlah,
                                "subtotal" to newSubtotal
                            )
                        ).await()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cart item: ${e.message}")
            false
        }
    }

    /**
     * Hapus item dari cart
     */
    suspend fun removeFromCart(cartId: String): Boolean {
        return try {
            db.collection(COLLECTION_CART).document(cartId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from cart: ${e.message}")
            false
        }
    }

    /**
     * Kosongkan cart
     */
    suspend fun clearCart(): Boolean {
        return try {
            val allItems = getAllCartItemsWithIds()
            val batch = db.batch()

            allItems.forEach { (id, _) ->
                val docRef = db.collection(COLLECTION_CART).document(id)
                batch.delete(docRef)
            }

            batch.commit().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cart: ${e.message}")
            false
        }
    }

    suspend fun getProdukDetailsForCart(cartItemsMap: Map<String, DcCart>): Map<String, DcProduk?> {
        return try {
            val produkIds = cartItemsMap.values.map { it.produk_id }.distinct()
            val produkMap = mutableMapOf<String, DcProduk?>()

            // Get produk details secara paralel
            produkIds.forEach { produkId ->
                val produk = getProdukForCart(produkId)
                produkMap[produkId] = produk
            }

            produkMap
        } catch (e: Exception) {
            Log.e(TAG, "Error getting produk details: ${e.message}")
            emptyMap()
        }
    }
}