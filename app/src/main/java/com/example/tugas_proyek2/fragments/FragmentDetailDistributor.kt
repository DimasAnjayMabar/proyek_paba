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
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.data_class.DcDistributor
import com.example.tugas_proyek2.repository.ProdukBackupRepository
import com.example.tugas_proyek2.service_layers.DistributorBackupRepository
import com.example.tugas_proyek2.service_layers.DistributorService
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
 * Use the [FragmentDetailDistributor.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentDetailDistributor : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var etNama: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnUpdate: Button
    private lateinit var btnDelete: Button

    private val distributorService = DistributorService()

    private var distributorId : String? = null

    private var distributorData : DcDistributor? = null

    private lateinit var distributorBackupRepository: DistributorBackupRepository

    companion object {
        private const val ARG_DISTRIBUTOR_ID = "distributor_id"

        fun newInstance(distributorId: String) = FragmentDetailDistributor().apply {
            arguments = Bundle().apply {
                putString(ARG_DISTRIBUTOR_ID, distributorId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        distributorId = arguments?.getString(ARG_DISTRIBUTOR_ID) ?: arguments?.getString("distributor_id")

        if (distributorId == null){
            Log.e("FragmentDetailDistributor", "distributorId is null! Arguments: ${arguments}")
        }else{
            Log.d("FragmentDetailDistributor", "Received distributorId: $distributorId")
        }

        distributorBackupRepository = DistributorBackupRepository(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detail_distributor, container, false)

        initViews(view)
        loadData()
        setupListeners()

        return view

    }

    private fun loadData(){
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val distributor = if (!distributorId.isNullOrEmpty()){
                    distributorService.getDistributorById(distributorId!!)
                }else null

                if (distributor != null){
                    distributorData = distributor
                    populateData(distributor)
                    Log.d("FragmentDetailDistributor", "Distributor loaded from Firestore")
                }else if (!distributorId.isNullOrEmpty()){
                    val backupDistributor = distributorBackupRepository.getBackupDistributor(distributorId!!)
                    if (backupDistributor != null){
                        distributorData = backupDistributor
                        populateData(backupDistributor)

                        Log.d("FragmentDetailDistributor", "Distributor loaded from Room backup")
                        Toast.makeText(requireContext(), "Menampilkan data dari backup lokal", Toast.LENGTH_LONG).show()
                    }else{
                        Log.d("FragmentDetailDistributor", "Distributor not found in Firestore or Room")
                    }
                }
            }catch (e: Exception){
                Log.e("FragmentDetailDistributor", "Error loading data", e)
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners(){
        btnUpdate.setOnClickListener {
            updateDistributor()
        }

        btnDelete.setOnClickListener {
            deleteDistributor()
        }
    }

    private fun initViews(view: View){
        etNama = view.findViewById(R.id.et_nama_distributor)
        etEmail = view.findViewById(R.id.et_email_distributor)
        btnUpdate = view.findViewById(R.id.btn_update_distributor)
        btnDelete = view.findViewById(R.id.btn_delete_distributor)
    }

    private fun updateDistributor(){
        if (!validateInput()){
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try{
                btnUpdate.isEnabled = false
                btnUpdate.text = "Mengupdate..."

                val updatedDistributor = DcDistributor(
                    nama = etNama.text.toString(),
                    email = etEmail.text.toString()
                )

                distributorId?.let { id ->
                    val success = distributorService.updateDistributor(id, updatedDistributor)

                    if (success){
                        Snackbar.make(requireView(), "Distributor berhasil diupdate", Snackbar.LENGTH_SHORT).show()
                        distributorData = updatedDistributor
                    }else{
                        Snackbar.make(requireView(), "Gagal mengupdate distributor", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }catch(e: Exception) {
                Log.e("FragmentDetailDistributor", "Error updating distributor", e)
                Toast.makeText(requireContext(), "Gagal mengupdate distributor: ${e.message}", Toast.LENGTH_SHORT).show()
            }finally {
                btnUpdate.isEnabled = true
                btnUpdate.text = "Update"
            }
        }
    }

    private fun validateInput(): Boolean{
        if (etNama.text.toString().trim().isEmpty()){
            etNama.error = "Nama distributor harus diisi"
            etNama.requestFocus()
            return false
        }

        if (etEmail.text.toString().trim().isEmpty()){
            etEmail.error = "Email distributor harus diisi"
            etEmail.requestFocus()
            return false
        }

        return true
    }

    private fun deleteDistributor(){
        distributorId?.let { id ->
            showConfirmationDialog(id)
        } ?: run {
            Toast.makeText(requireContext(), "ID distributor tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfirmationDialog(distributorId: String){
        AlertDialog.Builder(requireContext())
            .setTitle("Konfirmasi Hapus")
            .setMessage("Apakah Anda yakin ingin menghapus distributor ini?")
            .setPositiveButton("Ya"){ dialog, which ->
                proceedWithDelete(distributorId)
            }
            .setNegativeButton("Tidak", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .create()
            .show()
    }

    private fun proceedWithDelete(distributorId: String){
        CoroutineScope(Dispatchers.Main).launch {
            try {
                btnDelete.isEnabled = false
                btnDelete.text = "Menghapus..."

                distributorData?.let { distributor ->
                    distributorBackupRepository.backupDistributor(distributorId, distributor)
                    Log.d("FragmentDetailDistributor", "Backup saved to Room for distributorId: $distributorId")
                }

                val success = distributorService.deleteDistributor(distributorId)

                if (success){
                    showUndoableSnackbar(distributorId)
                }else{
                    Toast.makeText(requireContext(), "Gagal menghapus distributor", Toast.LENGTH_SHORT).show()
                    btnDelete.isEnabled = true
                    btnDelete.text = "Hapus"
                }
            }catch (e: Exception){
                Toast.makeText(requireContext(), "Gagal menghapus distributor: ${e.message}", Toast.LENGTH_SHORT).show()
                btnDelete.isEnabled = true
                btnDelete.text = "Hapus"
                Log.e("FragmentDetailDistributor", "Error deleting distributor", e)
            }
        }
    }

    private fun showUndoableSnackbar(distributorId: String){
        val snackbar = Snackbar.make(
            requireView(),
            "Distributor berhasil dihapus",
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction("BATAL"){
            if (isAdded && !isDetached){
                restoreDistributor(distributorId)
            }
        }

        snackbar.setActionTextColor(resources.getColor(android.R.color.holo_red_light, null))

        snackbar.addCallback(object : Snackbar.Callback(){
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

    private fun restoreDistributor(id: String){
        if (!isAdded || isDetached || activity == null) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try{
                btnDelete.isEnabled = false
                btnDelete.text = "Memulihkan..."

                val backupDistributor = distributorBackupRepository.getBackupDistributor(id)

                if (backupDistributor == null){
                    Toast.makeText(requireContext(), "Backup distributor tidak ditemukan", Toast.LENGTH_SHORT).show()
                    btnDelete.isEnabled = true
                    btnDelete.text = "Hapus"
                    return@launch
                }

                var restoreDistributorId = distributorId
                var success = false

                val distributorExist = distributorService.distributorExists(id)

                if (distributorExist) {
                    success = distributorService.updateDistributor(id, backupDistributor)
                    Log.d("FragmentDetailDistributor", "Updated existing distributor: $success")
                } else {
                    val newDistributorId = distributorService.addDistributor(backupDistributor)
                    success = newDistributorId != null
                    if(success){
                        restoreDistributorId = newDistributorId!!
                        Log.d("FragmentDetailDistributor", "Added new distributor with ID: $newDistributorId")
                    }
                }

                if(success){
                    distributorBackupRepository.deleteBackup(id)

                    distributorData = backupDistributor
                    distributorId = restoreDistributorId
                    populateData(backupDistributor)

                    Snackbar.make(requireView(), "Distributor berhasil dikembalikan", Snackbar.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(requireContext(), "Gagal mengembalikan distributor", Toast.LENGTH_SHORT).show()
                }

                btnDelete.isEnabled = true
                btnDelete.text = "Hapus"
            }catch (e: Exception){
                Toast.makeText(requireContext(), "Gagal memulihkan produk: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("FragmentDetailProduk", "Error restoring product", e)

                if (isAdded && !isDetached) {
                    btnDelete.isEnabled = true
                    btnDelete.text = "Hapus"
                }
            }
        }
    }

    private fun populateData(distributor: DcDistributor){
        etNama.setText(distributor.nama)
        etEmail.setText(distributor.email)
    }
}