package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.adapters.DistributorAdapter
import com.example.tugas_proyek2.data_class.DcDistributor
import com.example.tugas_proyek2.service_layers.DistributorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentFormDistributor.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentFormDistributor : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var etNama : EditText
    private lateinit var etEmail : EditText
    private lateinit var btnCancel : Button
    private lateinit var btnAdd : Button

    private val distributorService = DistributorService()

    private val distributorList = mutableListOf<DcDistributor>()

    private lateinit var distributorAdapter: ArrayAdapter<String>

    companion object{
        fun newInstance() = FragmentFormDistributor()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_form_distributor, container, false)

        initViews(view)
        setupListeners()

        return view
    }

    private fun initViews(view: View){
        etNama = view.findViewById(R.id.et_nama_distributor_new)
        etEmail = view.findViewById(R.id.et_email_distributor_new)
        btnCancel = view.findViewById(R.id.btn_cancel_distributor_new)
        btnAdd = view.findViewById(R.id.btn_add_distributor_new)
    }

    private fun setupListeners(){
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnAdd.setOnClickListener {
            addNewDistributor()
        }
    }

    private fun addNewDistributor(){
        if(!validateInput()){
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try{
                btnAdd.isEnabled = false
                btnAdd.text = "Menambahkan..."

                val newDistributor = DcDistributor(
                    nama = etNama.text.toString(),
                    email = etEmail.text.toString()
                )
                val distributorId = distributorService.addDistributor(newDistributor)

                if(distributorId != null){
                    Toast.makeText(requireContext(), "Distributor berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    clearForm()
                }else{
                    Toast.makeText(requireContext(), "Gagal menambahkan distributor", Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception){
                Log.e("FragmentFormDistributor", "Error adding new distributor", e)
                Toast.makeText(requireContext(), "Gagal menambahkan distributor: ${e.message}", Toast.LENGTH_SHORT).show()
            }finally {
                btnAdd.isEnabled = true
                btnAdd.text = "Add"
            }
        }
    }

    private fun validateInput(): Boolean {
        if (etNama.text.toString().trim().isEmpty()) {
            etNama.error = "Nama distributor harus diisi"
            etNama.requestFocus()
            return false
        }

        if (etEmail.text.toString().trim().isEmpty()) {
            etEmail.error = "Email distributor harus diisi"
            etEmail.requestFocus()
            return false
        }

        return true
    }

    private fun clearForm(){
        etNama.text.clear()
        etEmail.text.clear()
    }

}