package com.example.tugas_proyek2.misc

import androidx.room.TypeConverter
import com.example.tugas_proyek2.data_class.DcProduk
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DcProdukConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromDcProduk(produk: DcProduk?): String? {
        return produk?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toDcProduk(json: String?): DcProduk? {
        return json?.let {
            val type = object : TypeToken<DcProduk>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Timestamp? {
        return value?.let { Timestamp(it / 1000, (it % 1000 * 1000000).toInt()) }
    }

    @TypeConverter
    fun timestampToLong(timestamp: Timestamp?): Long? {
        return timestamp?.toDate()?.time
    }
}