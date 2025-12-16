package com.example.tugas_proyek2.fragments

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.data_class.DcDistributor
import com.example.tugas_proyek2.data_class.DcKategori
import com.example.tugas_proyek2.data_class.DcProduk
import com.example.tugas_proyek2.service_layers.DistributorService
import com.example.tugas_proyek2.service_layers.KategoriService
import com.example.tugas_proyek2.services.ProdukService
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FragmentFormProduk : Fragment() {

    private lateinit var etNama: EditText
    private lateinit var etHargaBeli: EditText
    private lateinit var etPersentaseKeuntungan: EditText
    private lateinit var tvHargaJual: TextView
    private lateinit var etStok: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var spinnerDistributor: Spinner
    private lateinit var etTanggal: EditText
    private lateinit var btnPickDate: Button
    private lateinit var ivProduk: ImageView
    private lateinit var btnUploadImage: Button
    private lateinit var btnBatal: Button
    private lateinit var btnAdd: Button
    private lateinit var progressBarUpload: ProgressBar

    private val produkService = ProdukService()
    private val kategoriService = KategoriService()
    private val distributorService = DistributorService()

    private var imageUri: Uri? = null

    // Data untuk dropdown
    private val kategoriList = mutableListOf<Pair<String, String>>() // (id, nama)
    private val distributorList = mutableListOf<Pair<String, String>>() // (id, nama)

    // Adapter untuk spinner
    private lateinit var kategoriAdapter: ArrayAdapter<String>
    private lateinit var distributorAdapter: ArrayAdapter<String>

    // Variabel untuk menyimpan ID yang dipilih
    private var selectedKategoriId: String? = null
    private var selectedDistributorId: String? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()
    private val UNSIGNED_UPLOAD_PRESET = "preset1"

    companion object {
        fun newInstance() = FragmentFormProduk()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_form_produk, container, false)

        initViews(view)
        setupAdapters()
        setupListeners()
        loadDropdownData()

        return view
    }

    private fun initViews(view: View) {
        etNama = view.findViewById(R.id.et_nama_produk_new)
        etHargaBeli = view.findViewById(R.id.et_harga_beli_produk_new)
        etPersentaseKeuntungan = view.findViewById(R.id.et_persentase_keuntungan_produk_new)
        tvHargaJual = view.findViewById(R.id.tv_harga_jual_produk_new)
        etStok = view.findViewById(R.id.et_stok_produk_new)
        spinnerKategori = view.findViewById(R.id.spinner_kategori_produk_new)
        spinnerDistributor = view.findViewById(R.id.spinner_distributor_produk_new)
        etTanggal = view.findViewById(R.id.et_tanggal_produk_new)
        btnPickDate = view.findViewById(R.id.btn_pick_date_produk_new)
        ivProduk = view.findViewById(R.id.iv_produk_new)
        btnUploadImage = view.findViewById(R.id.btn_upload_image_produk_new)
        btnBatal = view.findViewById(R.id.btn_kembali_produk_new)
        btnAdd = view.findViewById(R.id.btn_add_produk_new)
        progressBarUpload = view.findViewById(R.id.progress_bar_upload_produk_new)

        // Set tanggal default ke hari ini
        etTanggal.setText(dateFormat.format(calendar.time))
    }

    private fun setupAdapters() {
        // Setup adapter untuk spinner kategori
        kategoriAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf("Pilih Kategori")
        )
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = kategoriAdapter

        // Setup adapter untuk spinner distributor
        distributorAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf("Pilih Distributor")
        )
        distributorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDistributor.adapter = distributorAdapter
    }

    private fun setupListeners() {
        // Hitung harga jual otomatis
        etHargaBeli.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateHargaJual()
            }
        })

        etPersentaseKeuntungan.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateHargaJual()
            }
        })

        // Setup spinner listeners
        setupSpinnerListeners()

        // Date picker
        btnPickDate.setOnClickListener {
            showDatePicker()
        }

        etTanggal.setOnClickListener {
            showDatePicker()
        }

        // Upload image
        val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                imageUri = it
                Glide.with(requireContext())
                    .load(it)
                    .centerCrop()
                    .into(ivProduk)
            }
        }

        btnUploadImage.setOnClickListener {
            getContent.launch("image/*")
        }

        // Tombol batal
        btnBatal.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Tombol add
        btnAdd.setOnClickListener {
            addNewProduct()
        }
    }

    private fun loadDropdownData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Load kategori dan distributor secara paralel
                val kategoriDeferred = async { kategoriService.getAllKategoris() }
                val distributorDeferred = async { distributorService.getAllDistributors() }

                // Tunggu kategori dan distributor selesai
                val kategoriMap = kategoriDeferred.await()
                val distributorMap = distributorDeferred.await()

                // Update spinner dengan data
                updateKategoriSpinner(kategoriMap)
                updateDistributorSpinner(distributorMap)

                // Reset spinner listeners setelah data di-load
                setupSpinnerListeners()

            } catch (e: Exception) {
                Log.e("FragmentFormProduk", "Error loading dropdown data", e)
                Toast.makeText(requireContext(), "Gagal memuat data kategori/distributor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinnerListeners() {
        // Spinner kategori listener
        spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && kategoriList.isNotEmpty()) {
                    selectedKategoriId = kategoriList[position - 1].first
                    Log.d("FragmentFormProduk", "Selected kategori ID: $selectedKategoriId")
                } else {
                    selectedKategoriId = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedKategoriId = null
            }
        }

        // Spinner distributor listener
        spinnerDistributor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && distributorList.isNotEmpty()) {
                    selectedDistributorId = distributorList[position - 1].first
                    Log.d("FragmentFormProduk", "Selected distributor ID: $selectedDistributorId")
                } else {
                    selectedDistributorId = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedDistributorId = null
            }
        }
    }

    private fun updateKategoriSpinner(kategoriMap: Map<String, DcKategori>) {
        requireActivity().runOnUiThread {
            kategoriList.clear()
            kategoriAdapter.clear()

            // Tambah item default
            kategoriAdapter.add("Pilih Kategori")

            // Tambah semua kategori dari Firestore
            kategoriMap.forEach { (id, kategori) ->
                val nama = kategori.nama ?: "Kategori $id"
                kategoriList.add(Pair(id, nama))
                kategoriAdapter.add(nama)
            }

            kategoriAdapter.notifyDataSetChanged()
        }
    }

    private fun updateDistributorSpinner(distributorMap: Map<String, DcDistributor>) {
        requireActivity().runOnUiThread {
            distributorList.clear()
            distributorAdapter.clear()

            // Tambah item default
            distributorAdapter.add("Pilih Distributor")

            // Tambah semua distributor dari Firestore
            distributorMap.forEach { (id, distributor) ->
                val nama = distributor.nama ?: "Distributor $id"
                distributorList.add(Pair(id, nama))
                distributorAdapter.add(nama)
            }

            distributorAdapter.notifyDataSetChanged()
        }
    }

    private fun calculateHargaJual() {
        try {
            val hargaBeli = etHargaBeli.text.toString().toLongOrNull() ?: 0L
            val persentase = etPersentaseKeuntungan.text.toString().toDoubleOrNull() ?: 0.0

            val keuntungan = hargaBeli * (persentase / 100)
            val hargaJual = hargaBeli + keuntungan.toLong()

            // Format dengan NumberFormat
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatter.maximumFractionDigits = 0
            tvHargaJual.text = formatter.format(hargaJual)
        } catch (e: Exception) {
            tvHargaJual.text = "Rp 0"
            Log.e("FragmentFormProduk", "Error calculating harga jual", e)
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                etTanggal.setText(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun addNewProduct() {
        if (!validateInput()) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                btnAdd.isEnabled = false
                btnAdd.text = "Memproses..."

                // Validasi pilihan dropdown
                if (selectedKategoriId == null) {
                    Toast.makeText(requireContext(), "Pilih kategori terlebih dahulu", Toast.LENGTH_SHORT).show()
                    resetAddButton()
                    return@launch
                }

                if (selectedDistributorId == null) {
                    Toast.makeText(requireContext(), "Pilih distributor terlebih dahulu", Toast.LENGTH_SHORT).show()
                    resetAddButton()
                    return@launch
                }

                // Ambil nama produk untuk pengecekan
                val namaProduk = etNama.text.toString().trim()

                // Cek apakah produk dengan nama yang sama sudah ada
                val existingProductInfo = withContext(Dispatchers.IO) {
                    getExistingProductInfo(namaProduk)
                }

                // Jika ada gambar, upload ke Cloudinary
                val imageUrl = if (imageUri != null) {
                    uploadImageToCloudinary(imageUri!!)
                } else {
                    ""
                }

                // Buat objek DcProduk dengan data baru
                val newProduk = DcProduk(
                    nama = namaProduk,
                    harga_beli = etHargaBeli.text.toString().toLongOrNull(),
                    persentase_keuntungan = etPersentaseKeuntungan.text.toString().toDoubleOrNull(),
                    harga_jual = parseHargaJualFromText(),
                    stok = etStok.text.toString().toLongOrNull(),
                    kategori_id = selectedKategoriId,
                    distributor_id = selectedDistributorId,
                    tanggal = Timestamp(calendar.time),
                    image = imageUrl
                )

                if (existingProductInfo != null) {
                    // Produk sudah ada, tampilkan dialog konfirmasi update
                    showUpdateConfirmationDialog(existingProductInfo, newProduk)
                } else {
                    // Produk belum ada, tambah produk baru
                    addNewProductToDatabase(newProduk)
                }

            } catch (e: Exception) {
                Log.e("FragmentFormProduk", "Error adding new product", e)
                Toast.makeText(requireContext(), "Gagal memproses produk: ${e.message}", Toast.LENGTH_SHORT).show()
                resetAddButton()
            }
        }
    }

    private suspend fun getExistingProductInfo(namaProduk: String): Pair<String, DcProduk>? {
        return try {
            // Ambil semua produk dengan ID
            val allProductsWithIds = produkService.getAllProductsWithIds()

            // Cari produk dengan nama yang sama (case-insensitive)
            val existingEntry = allProductsWithIds.entries.firstOrNull {
                it.value.nama?.trim()?.equals(namaProduk, ignoreCase = true) == true
            }

            existingEntry?.let { Pair(it.key, it.value) }
        } catch (e: Exception) {
            Log.e("FragmentFormProduk", "Error checking product existence", e)
            null
        }
    }

    private fun showUpdateConfirmationDialog(existingProductInfo: Pair<String, DcProduk>, newProductData: DcProduk) {
        val (productId, existingProduct) = existingProductInfo

        // Hitung stok baru (stok lama + stok baru)
        val stokLama = existingProduct.stok ?: 0L
        val stokBaru = newProductData.stok ?: 0L
        val totalStok = stokLama + stokBaru

        // Buat dialog konfirmasi
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Produk Sudah Ada")
            .setMessage("""
            Produk "${existingProduct.nama}" sudah ada di database.
            
            Data saat ini:
            • Harga Beli: Rp${existingProduct.harga_beli}
            • Harga Jual: Rp${existingProduct.harga_jual}
            • Stok: ${existingProduct.stok}
            
            Data baru yang akan diupdate:
            • Harga Beli: Rp${newProductData.harga_beli}
            • Harga Jual: Rp${newProductData.harga_jual}
            • Stok: ${totalStok} (${existingProduct.stok} + ${newProductData.stok})
            • Kategori: Akan diupdate
            • Distributor: Akan diupdate
            
            Apakah Anda ingin mengupdate produk ini dengan data baru?
        """.trimIndent())
            .setPositiveButton("Ya, Update") { dialog, _ ->
                dialog.dismiss()
                CoroutineScope(Dispatchers.Main).launch {
                    updateExistingProduct(productId, existingProduct, newProductData)
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
                resetAddButton()
            }
            .setNeutralButton("Tambah sebagai Produk Baru") { dialog, _ ->
                dialog.dismiss()
                CoroutineScope(Dispatchers.Main).launch {
                    // Tambah produk baru dengan nama yang berbeda
                    addNewProductToDatabase(newProductData.copy(
                        nama = "${newProductData.nama} (2)" // Tambah penanda
                    ))
                }
            }
            .setCancelable(false)
            .show()
    }

    private suspend fun updateExistingProduct(productId: String, existingProduct: DcProduk, newProductData: DcProduk) {
        try {
            // Update semua field dengan data baru
            val updatedProduct = DcProduk(
                nama = newProductData.nama, // Nama tetap sama
                harga_beli = newProductData.harga_beli,
                persentase_keuntungan = newProductData.persentase_keuntungan,
                harga_jual = newProductData.harga_jual,
                // Tambah stok baru ke stok lama
                stok = (existingProduct.stok ?: 0L) + (newProductData.stok ?: 0L),
                kategori_id = newProductData.kategori_id,
                distributor_id = newProductData.distributor_id,
                // Update tanggal ke tanggal terbaru
                tanggal = newProductData.tanggal,
                // Gunakan gambar baru jika ada, jika tidak gunakan gambar lama
                image = newProductData.image!!.ifEmpty { existingProduct.image ?: "" }
            )

            val success = produkService.updateProduct(productId, updatedProduct)

            if (success) {
                Snackbar.make(requireView(), "Produk berhasil diupdate", Snackbar.LENGTH_SHORT).show()
                clearForm()
            } else {
                Toast.makeText(requireContext(), "Gagal mengupdate produk", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("FragmentFormProduk", "Error updating existing product", e)
            Toast.makeText(requireContext(), "Gagal mengupdate produk: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            resetAddButton()
        }
    }

    private suspend fun addNewProductToDatabase(product: DcProduk) {
        try {
            val productId = produkService.addProduct(product)

            if (productId != null) {
                Snackbar.make(requireView(), "Produk baru berhasil ditambahkan", Snackbar.LENGTH_SHORT).show()
                clearForm()
            } else {
                Toast.makeText(requireContext(), "Gagal menambahkan produk", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("FragmentFormProduk", "Error adding new product to database", e)
            Toast.makeText(requireContext(), "Gagal menambahkan produk: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            resetAddButton()
        }
    }

    private fun resetAddButton() {
        btnAdd.isEnabled = true
        btnAdd.text = "Add"
    }

    private suspend fun uploadImageToCloudinary(uri: Uri): String = suspendCoroutine { continuation ->
        val fileName = "product_${System.currentTimeMillis()}"

        MediaManager.get().upload(uri)
            .unsigned(UNSIGNED_UPLOAD_PRESET)
            .option("folder", "produk_images")
            .option("public_id", fileName)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary", "Upload Start : $requestId")
                    requireActivity().runOnUiThread {
                        progressBarUpload.visibility = View.VISIBLE
                        progressBarUpload.progress = 0
                        progressBarUpload.max = 100
                    }
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    val progress = (bytes * 100 / totalBytes).toInt()
                    requireActivity().runOnUiThread {
                        progressBarUpload.progress = progress
                    }
                }

                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val url = resultData?.get("secure_url")?.toString() ?: ""
                    Log.d("Cloudinary", "Upload success : $url")
                    requireActivity().runOnUiThread {
                        progressBarUpload.visibility = View.GONE
                    }
                    continuation.resume(url)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.d("Cloudinary", "Upload error : ${error.toString()}")
                    requireActivity().runOnUiThread {
                        progressBarUpload.visibility = View.GONE
                        Toast.makeText(requireContext(), "Gagal mengupload gambar", Toast.LENGTH_SHORT).show()
                    }
                    continuation.resume("")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.d("Cloudinary", "Upload reschedule : ${error.toString()}")
                }
            }).dispatch()
    }

    private fun parseHargaJualFromText(): Long {
        val text = tvHargaJual.text.toString()
        return try {
            // Hapus karakter mata uang dan titik pemisah ribuan
            val cleanString = text.replace("[^\\d]".toRegex(), "")

            // Jika string kosong, return 0
            if (cleanString.isEmpty()) return 0L

            // Konversi ke Long
            cleanString.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.e("FragmentFormProduk", "Error parsing harga jual: $text", e)
            0L
        }
    }

    private fun validateInput(): Boolean {
        if (etNama.text.toString().trim().isEmpty()) {
            etNama.error = "Nama produk harus diisi"
            etNama.requestFocus()
            return false
        }

        if (etHargaBeli.text.toString().toLongOrNull() == null) {
            etHargaBeli.error = "Harga beli harus berupa angka"
            etHargaBeli.requestFocus()
            return false
        }

        if (etPersentaseKeuntungan.text.toString().toDoubleOrNull() == null) {
            etPersentaseKeuntungan.error = "Persentase keuntungan harus berupa angka"
            etPersentaseKeuntungan.requestFocus()
            return false
        }

        if (etStok.text.toString().toLongOrNull() == null) {
            etStok.error = "Stok harus berupa angka"
            etStok.requestFocus()
            return false
        }

        return true
    }

    private fun clearForm() {
        etNama.text.clear()
        etHargaBeli.text.clear()
        etPersentaseKeuntungan.text.clear()
        etStok.text.clear()
        tvHargaJual.text = "0"

        // Reset dropdowns
        spinnerKategori.setSelection(0)
        spinnerDistributor.setSelection(0)

        // Reset tanggal ke hari ini
        calendar.time = Date()
        etTanggal.setText(dateFormat.format(calendar.time))

        // Reset gambar
        imageUri = null
        ivProduk.setImageDrawable(null)
        ivProduk.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))

        // Reset selected IDs
        selectedKategoriId = null
        selectedDistributorId = null
    }
}