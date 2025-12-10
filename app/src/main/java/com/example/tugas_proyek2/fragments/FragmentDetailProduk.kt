package com.example.tugas_proyek2.fragments

import android.app.AlertDialog
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
import com.example.tugas_proyek2.repository.ProdukBackupRepository
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

class FragmentDetailProduk : Fragment() {

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
    private lateinit var btnHapus: Button
    private lateinit var btnUpdate: Button
    private lateinit var progressBarUpload: ProgressBar

    private val produkService = ProdukService()
    private val kategoriService = KategoriService()
    private val distributorService = DistributorService()

    private var produkId: String? = null
    private var produkData: DcProduk? = null
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

    private val kategoriMap = mutableMapOf<String, DcKategori>() // (id -> DcKategori)
    private val distributorMap = mutableMapOf<String, DcDistributor>() // (id -> DcDistributor)

    // List untuk menampung nama kategori/distributor (untuk adapter)
    private val kategoriNamaList = mutableListOf<String>()
    private val distributorNamaList = mutableListOf<String>()

    private lateinit var produkBackupRepository: ProdukBackupRepository

    companion object {
        private const val ARG_PRODUK_ID = "produk_id"

        fun newInstance(produkId: String) =
            FragmentDetailProduk().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRODUK_ID, produkId)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        produkId = arguments?.getString(ARG_PRODUK_ID) ?: arguments?.getString("produk_id")

        if (produkId == null) {
            Log.e("FragmentDetailProduk", "produkId is null! Arguments: ${arguments}")
        } else {
            Log.d("FragmentDetailProduk", "Received produkId: $produkId")
        }

        produkBackupRepository = ProdukBackupRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detail_produk, container, false)

        initViews(view)
        setupAdapters()
        setupListeners()
        loadData()

        return view
    }

    private fun initViews(view: View) {
        etNama = view.findViewById(R.id.et_nama_produk)
        etHargaBeli = view.findViewById(R.id.et_harga_beli)
        etPersentaseKeuntungan = view.findViewById(R.id.et_persentase_keuntungan)
        tvHargaJual = view.findViewById(R.id.tv_harga_jual)
        etStok = view.findViewById(R.id.et_stok)
        spinnerKategori = view.findViewById(R.id.spinner_kategori)
        spinnerDistributor = view.findViewById(R.id.spinner_distributor)
        etTanggal = view.findViewById(R.id.et_tanggal)
        btnPickDate = view.findViewById(R.id.btn_pick_date)
        ivProduk = view.findViewById(R.id.iv_produk)
        btnUploadImage = view.findViewById(R.id.btn_upload_image)
        btnHapus = view.findViewById(R.id.btn_hapus)
        btnUpdate = view.findViewById(R.id.btn_update)
        progressBarUpload = view.findViewById(R.id.progress_bar_upload)
    }

    private fun setupAdapters() {
        // Setup adapter untuk spinner
        kategoriAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf("Loading kategori...")
        )
        kategoriAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = kategoriAdapter

        distributorAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf("Loading distributor...")
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

        // Setup spinner listeners (akan di-reset setelah data di-load)
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

        // Tombol update
        btnUpdate.setOnClickListener {
            updateProduct()
        }

        // Tombol hapus
        btnHapus.setOnClickListener {
            deleteProduct()
        }
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Load kategori dan distributor secara paralel
                val kategoriDeferred = async { kategoriService.getAllKategoris() }
                val distributorDeferred = async { distributorService.getAllDistributors() }

                // Tunggu kategori dan distributor selesai dulu
                val kategoriMap = kategoriDeferred.await()
                val distributorMap = distributorDeferred.await()

                // Update spinner dengan data
                updateKategoriSpinner(kategoriMap)
                updateDistributorSpinner(distributorMap)

                // Load produk setelah spinner siap
                val produk = if (!produkId.isNullOrEmpty()) {
                    produkService.getProductById(produkId!!)
                } else null

                if (produk != null) {
                    // Produk ditemukan di Firestore
                    produkData = produk
                    populateData(produk)
                    Log.d("FragmentDetailProduk", "Product loaded from Firestore")
                } else if (!produkId.isNullOrEmpty()) {
                    // Produk tidak ditemukan di Firestore, CEK APAKAH ADA DI BACKUP ROOM
                    val backupProduk = produkBackupRepository.getBackupProduk(produkId!!)
                    if (backupProduk != null) {
                        // Konversi backup ke DcProduk dan populate data
                        produkData = backupProduk
                        populateData(backupProduk)

                        Log.d("FragmentDetailProduk", "Product loaded from Room backup")

                        // Tampilkan notifikasi bahwa ini data backup
                        Toast.makeText(
                            requireContext(),
                            "Menampilkan data dari backup lokal",
                            Toast.LENGTH_LONG
                        ).show()

                        // Tampilkan tombol "Restore to Cloud"
//                        showRestoreButton()
                    } else {
                        Log.d("FragmentDetailProduk", "Product not found in Firestore or Room")
                    }
                }

                // Reset spinner listeners setelah data di-populate
                setupSpinnerListeners()

            } catch (e: Exception) {
                Log.e("FragmentDetailProduk", "Error loading data", e)
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpinnerListeners() {
        // Spinner kategori listener
        spinnerKategori.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && kategoriList.isNotEmpty()) {
                    selectedKategoriId = kategoriList[position - 1].first
                    Log.d("FragmentDetailProduk", "Selected kategori ID: $selectedKategoriId")
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
                    Log.d("FragmentDetailProduk", "Selected distributor ID: $selectedDistributorId")
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

            // Format dengan NumberFormat yang benar
            val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            formatter.maximumFractionDigits = 0 // Tidak ada desimal
            tvHargaJual.text = formatter.format(hargaJual)
        } catch (e: Exception) {
            tvHargaJual.text = "Rp 0"
            Log.e("FragmentDetailProduk", "Error calculating harga jual", e)
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

    private fun populateData(produk: DcProduk) {
        etNama.setText(produk.nama)
        etHargaBeli.setText(produk.harga_beli.toString())
        etPersentaseKeuntungan.setText(produk.persentase_keuntungan.toString())
        etStok.setText(produk.stok.toString())

        // Tanggal
        produk.tanggal?.let { timestamp ->
            val date = timestamp.toDate()
            etTanggal.setText(dateFormat.format(date))
            calendar.time = date
        }

        // Simpan ID kategori dan distributor untuk validasi
        selectedKategoriId = produk.kategori_id
        selectedDistributorId = produk.distributor_id

        // Set spinner selection setelah adapter di-update
        produk.kategori_id?.let { kategoriId ->
            setSpinnerSelection(spinnerKategori, kategoriList, kategoriId)
        }

        produk.distributor_id?.let { distributorId ->
            setSpinnerSelection(spinnerDistributor, distributorList, distributorId)
        }

        // Gambar
        produk.image?.let { imageUrl ->
            if (imageUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(ivProduk)
            }
        }

        // Hitung harga jual awal
        calculateHargaJual()
    }

    private fun setSpinnerSelection(spinner: Spinner, dataList: List<Pair<String, String>>, selectedId: String) {
        // Cari posisi ID dalam list
        val position = dataList.indexOfFirst { it.first == selectedId }

        // Jika ditemukan (posisi + 1 karena ada item default "Pilih ...")
        if (position != -1) {
            // Tunggu sampai UI thread selesai update
            spinner.post {
                if (spinner.adapter.count > position + 1) {
                    spinner.setSelection(position + 1)
                    Log.d("FragmentDetailProduk", "Set spinner selection to position: ${position + 1}")
                }
            }
        }
    }

    private fun updateProduct() {
        if (!validateInput()) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                btnUpdate.isEnabled = false
                btnUpdate.text = "Mengupdate..."

                // Validasi pilihan dropdown
                if (selectedKategoriId == null) {
                    Toast.makeText(requireContext(), "Pilih kategori terlebih dahulu", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (selectedDistributorId == null) {
                    Toast.makeText(requireContext(), "Pilih distributor terlebih dahulu", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Jika ada gambar baru, upload ke Cloudinary
                val imageUrl = if (imageUri != null) {
                    uploadImageToCloudinary(imageUri!!)
                } else {
                    produkData?.image ?: ""
                }

                // Buat objek DcProduk
                val updatedProduk = DcProduk(
                    nama = etNama.text.toString(),
                    harga_beli = etHargaBeli.text.toString().toLongOrNull(),
                    persentase_keuntungan = etPersentaseKeuntungan.text.toString().toDoubleOrNull(),
                    harga_jual = parseHargaJualFromText(),
                    stok = etStok.text.toString().toLongOrNull(),
                    kategori_id = selectedKategoriId,
                    distributor_id = selectedDistributorId,
                    tanggal = Timestamp(calendar.time),
                    image = imageUrl
                )

                // Update ke database
                produkId?.let { id ->
                    val success = produkService.updateProduct(id, updatedProduk)

                    if (success) {
                        Snackbar.make(requireView(), "Produk berhasil diupdate", Snackbar.LENGTH_SHORT).show()
                        produkData = updatedProduk
                    } else {
                        Toast.makeText(requireContext(), "Gagal mengupdate produk", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("FragmentDetailProduk", "Error updating product", e)
                Toast.makeText(requireContext(), "Gagal mengupdate produk: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                btnUpdate.isEnabled = true
                btnUpdate.text = "Update"
            }
        }
    }

    private suspend fun uploadImageToCloudinary(uri: Uri): String = suspendCoroutine { continuation ->
        val fileName = produkId ?: "product_${System.currentTimeMillis()}"

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
                    continuation.resume(produkData?.image ?: "")
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
            Log.e("FragmentDetailProduk", "Error parsing harga jual: $text", e)
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

        // Validasi dropdown
        if (selectedKategoriId == null) {
            Toast.makeText(requireContext(), "Pilih kategori terlebih dahulu", Toast.LENGTH_SHORT).show()
            return false
        }

        if (selectedDistributorId == null) {
            Toast.makeText(requireContext(), "Pilih distributor terlebih dahulu", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun deleteProduct() {
        produkId?.let { id ->
            // Tampilkan dialog konfirmasi
            showDeleteConfirmationDialog(id)
        } ?: run {
            Toast.makeText(requireContext(), "ID produk tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmationDialog(productId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus produk ini?")
            .setPositiveButton("Ya") { dialog, which ->
                // Jika user klik Ya, lanjutkan proses hapus
                proceedWithDelete(productId)
            }
            .setNegativeButton("Tidak", null) // Jika Tidak, tutup dialog saja
            .setIcon(android.R.drawable.ic_dialog_alert)
            .create()
            .show()
    }

    private fun proceedWithDelete(productId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Tampilkan progress indicator jika perlu
                btnHapus.isEnabled = false
                btnHapus.text = "Menghapus..."

                // 1. SIMPAN BACKUP KE ROOM SEBELUM MENGHAPUS
                produkData?.let { produk ->
                    produkBackupRepository.backupProduk(productId, produk)
                    Log.d("FragmentDetailProduk", "Backup saved to Room for productId: $productId")
                }

                // 2. HAPUS DARI FIRESTORE
                val success = produkService.deleteProduct(productId)

                if (success) {
                    // 3. Tampilkan snackbar dengan action undo
                    showUndoableSnackbar(productId)
                } else {
                    Toast.makeText(requireContext(), "Gagal menghapus produk", Toast.LENGTH_SHORT).show()
                    btnHapus.isEnabled = true
                    btnHapus.text = "Hapus"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal menghapus produk: ${e.message}", Toast.LENGTH_SHORT).show()
                btnHapus.isEnabled = true
                btnHapus.text = "Hapus"
                Log.e("FragmentDetailProduk", "Error deleting product", e)
            }
        }
    }

    private fun showUndoableSnackbar(productId: String) {
        val snackbar = Snackbar.make(
            requireView(),
            "Produk berhasil dihapus",
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction("BATAL") {
            // Jika user klik BATAL, restore produk
            if (isAdded && !isDetached) {
                restoreProduct(productId)
            }
        }

        snackbar.setActionTextColor(resources.getColor(android.R.color.holo_red_light, null))

        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)

                // Jika snackbar ditutup tanpa klik BATAL, kembali ke fragment sebelumnya
                if (event != DISMISS_EVENT_ACTION) {
                    // Cek apakah fragment masih terattach sebelum melakukan operasi UI
                    if (isAdded && !isDetached && activity != null) {
                        activity?.runOnUiThread {
                            parentFragmentManager.popBackStack()
                        }
                    }
                }
            }
        })

        snackbar.show()
    }

    private fun restoreProduct(productId: String) {
        if (!isAdded || isDetached || activity == null) return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                btnHapus.isEnabled = false
                btnHapus.text = "Memulihkan..."

                val backupProduk = produkBackupRepository.getBackupProduk(productId)
                if (backupProduk == null) {
                    Toast.makeText(requireContext(), "Backup produk tidak ditemukan", Toast.LENGTH_SHORT).show()
                    btnHapus.isEnabled = true
                    btnHapus.text = "Hapus"
                    return@launch
                }

                var restoredProductId = productId
                var success = false

                // Cek apakah produk masih ada di Firestore
                val productExists = produkService.productExists(productId)

                if (productExists) {
                    // Produk masih ada, lakukan update
                    success = produkService.updateProduct(productId, backupProduk)
                    Log.d("FragmentDetailProduk", "Updated existing product: $success")
                } else {
                    // Produk sudah dihapus, buat baru
                    val newProductId = produkService.addProduct(backupProduk)
                    success = newProductId != null
                    if (success) {
                        restoredProductId = newProductId!!
                        Log.d("FragmentDetailProduk", "Created new product with ID: $restoredProductId")
                    }
                }

                if (success) {
                    // Hapus backup dari Room
                    produkBackupRepository.deleteBackup(productId)

                    // Update UI
                    produkData = backupProduk
                    produkId = restoredProductId
                    populateData(backupProduk)

                    Snackbar.make(requireView(), "Produk berhasil dipulihkan", Snackbar.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal memulihkan produk", Toast.LENGTH_SHORT).show()
                }

                btnHapus.isEnabled = true
                btnHapus.text = "Hapus"

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memulihkan produk: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("FragmentDetailProduk", "Error restoring product", e)

                if (isAdded && !isDetached) {
                    btnHapus.isEnabled = true
                    btnHapus.text = "Hapus"
                }
            }
        }
    }
}