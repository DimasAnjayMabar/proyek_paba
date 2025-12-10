package com.example.tugas_proyek2.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.room.Delete
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.data_class.DcKategori
import com.example.tugas_proyek2.service_layers.KategoriBackupRepository
import com.example.tugas_proyek2.service_layers.KategoriService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentDetailKategori.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentDetailKategori : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var etNama : EditText

    private lateinit var btnUpdate : Button

    private lateinit var btnDelete: Button

    private val kategoriService = KategoriService()

    private var kategoriId : String? = null

    private var kategoriData : DcKategori? = null

    private lateinit var kategoriBackupRepository: KategoriBackupRepository


    companion object{
        private const val ARG_KATEGORI_ID = "kategori_id"

        fun newInstance(kategoriId: String) = FragmentDetailKategori().apply {
            arguments = Bundle().apply {
                putString(ARG_KATEGORI_ID, kategoriId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        kategoriId = arguments?.getString(ARG_KATEGORI_ID) ?: arguments?.getString("kategori_id")

        if(kategoriId == null){
            Log.e("FragmentDetailKategori", "kategoriId is null! Arguments: ${arguments}")
        }else{
            Log.d("FragmentDetailKategori", "Received kategoriId: $kategoriId")
        }

        kategoriBackupRepository = KategoriBackupRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_detail_kategori, container, false)

        initViews(view)
        loadData()
        setupListeners()

        return view
    }

    private fun loadData(){
        CoroutineScope(Dispatchers.Main).launch {
            try{
                val kategori = if (!kategoriId.isNullOrEmpty()){
                    kategoriService.getKategoriById(kategoriId!!)
                }else null

                if (kategori != null){
                    kategoriData = kategori
                    populateData(kategori)
                    Log.d("FragmentDetailKategori", "Kategori loaded from Firestore")
                }else if(!kategoriId.isNullOrEmpty()){
                    val backupKategori = kategoriBackupRepository.getBackupKategori(kategoriId!!)
                    if (backupKategori != null){
                        kategoriData = backupKategori
                        populateData(backupKategori)

                        Log.d("FragmentDetailKategori", "Kategori loaded from Room backup")
                        Toast.makeText(requireContext(), "Menampilkan data dari backup lokal", Toast.LENGTH_LONG).show()
                    }else{
                        Log.d("FragmentDetailKategori", "Kategori not found in Firestore or Room")
                    }
                }
            }catch(e: Exception){
                Log.e("FragmentDetailKategori", "Error loading data", e)
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners(){
        btnUpdate.setOnClickListener {
            updateKategori()
        }

        btnDelete.setOnClickListener {
            deleteKategori()
        }
    }

    private fun initViews(view: View){
        etNama = view.findViewById(R.id.et_nama_kategori)
        btnUpdate = view.findViewById(R.id.btn_update_kategori)
        btnDelete = view.findViewById(R.id.btn_delete_kategori)
    }

    private fun updateKategori(){
        if (!validateInput()){
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try{
                btnUpdate.isEnabled = false
                btnUpdate.text = "Mengupdate..."

                val updatedKategori = DcKategori(
                    nama = etNama.text.toString()
                )

                kategoriId?.let { id ->
                    val success = kategoriService.updateKategori(id, updatedKategori)

                    if (success){
                        Snackbar.make(requireView(), "Kategori berhasil diupdate", Snackbar.LENGTH_SHORT).show()
                        kategoriData = updatedKategori
                    }else{
                        Snackbar.make(requireView(), "Gagal mengupdate kategori", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }catch (e: Exception){
                Log.e("FragmentDetailKategori", "Error updating kategori", e)
                Toast.makeText(requireContext(), "Gagal mengupdate kategori: ${e.message}", Toast.LENGTH_SHORT).show()
            }finally {
                btnUpdate.isEnabled = true
                btnUpdate.text = "Update"
            }
        }
    }

    private fun validateInput(): Boolean{
        if (etNama.text.toString().trim().isEmpty()){
            etNama.error = "Nama kategori harus diisi"
            etNama.requestFocus()
            return false
        }

        return true
    }

    private fun deleteKategori(){
        kategoriId?.let { id ->
            showConfirmationDialog(id)
        } ?: run {
            Toast.makeText(requireContext(), "ID kategori tidak ditemukan", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun showConfirmationDialog(id: String){
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus kategori ini?")
            .setPositiveButton("Ya") { dialog, which ->
                proceedWithDelete(id)
            }
            .setNegativeButton("Tidak", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .create()
            .show()
    }

    private fun proceedWithDelete(id: String){
        CoroutineScope(Dispatchers.Main).launch {
            try{
                btnDelete.isEnabled = false
                btnDelete.text = "Menghapus..."

                kategoriData?.let { kategori ->
                    kategoriBackupRepository.backupKategori(id, kategori)
                    Log.d("FragmentDetailKategori", "Backup saved to Room for kategoriId: $id")
                }

                val success = kategoriService.deleteKategori(id)

                if (success){
                    showUndoableSnackbar(id)
                }else{
                    Toast.makeText(requireContext(), "Gagal menghapus kategori", Toast.LENGTH_SHORT)
                    btnDelete.isEnabled = true
                    btnDelete.text = "Hapus"
                }
            }catch(e: Exception){
                Toast.makeText(requireContext(), "Gagal menghapus kategori: ${e.message}", Toast.LENGTH_SHORT)
                btnDelete.isEnabled = true
                btnDelete.text = "Hapus"
                Log.e("FragmentDetailKategori", "Error deleting kategori", e)
            }
        }
    }

    private fun showUndoableSnackbar(id: String){
        val snackbar = Snackbar.make(
            requireView(),
            "Kategori berhasil dihapus",
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction("BATAL"){
            if (isAdded && !isDetached){
                restoreKategori(id)
            }
        }

        snackbar.setActionTextColor(resources.getColor(android.R.color.holo_red_light, null))

        snackbar.addCallback(object: Snackbar.Callback(){
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)

                if(event != DISMISS_EVENT_ACTION){
                    if(isAdded && !isDetached && activity != null){
                        activity?.runOnUiThread {
                            parentFragmentManager.popBackStack()
                        }
                    }
                }
            }
        })

        snackbar.show()
    }

    private fun restoreKategori(id: String){
        if(isAdded || isDetached || activity == null){
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try{
                btnDelete.isEnabled = false
                btnDelete.text = "Memulihkan..."

                val backupKategori = kategoriBackupRepository.getBackupKategori(id)

                if (backupKategori == null){
                    Toast.makeText(requireContext(), "Backup kategori tidak ditemukan", Toast.LENGTH_SHORT)
                    btnDelete.isEnabled = true
                    btnDelete.text = "Hapus"
                    return@launch
                }

                var restoreKategoriId = kategoriId
                var success = false

                val kategoriExist = kategoriService.kategoriExists(id)

                if (kategoriExist){
                    success = kategoriService.updateKategori(id, backupKategori)
                    Log.d("FragmentDetailKategori", "Updated existing kategori: $success")
                }else{
                    val newKategoriId = kategoriService.addKategori(backupKategori)
                    success = newKategoriId != null
                    if(success){
                        restoreKategoriId = newKategoriId!!
                        Log.d("FragmentDetailKategori", "Added new kategori with ID: $newKategoriId")
                    }
                }

                if(success){
                    kategoriBackupRepository.deleteBackup(id)

                    kategoriData = backupKategori
                    kategoriId = restoreKategoriId
                    populateData(backupKategori)

                    Snackbar.make(requireView(), "Kategori berhasil dikembalikan", Snackbar.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(requireContext(), "Gagal mengembalikan kategori", Toast.LENGTH_SHORT)
                }

                btnDelete.isEnabled = true
                btnDelete.text = "Hapus"

            }catch(e: Exception){
                Toast.makeText(requireContext(), "Gagal memulihkan kategori: ${e.message}", Toast.LENGTH_SHORT)
                Log.e("FragmentDetailKategori", "Error restoring kategori", e)

                if(isAdded && !isDetached){
                    btnDelete.isEnabled = true
                    btnDelete.text = "Hapus"
                }
            }
        }
    }

    private fun populateData(kategori: DcKategori) {
        etNama.setText(kategori.nama)
    }
}
