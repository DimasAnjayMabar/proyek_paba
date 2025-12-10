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
import com.example.tugas_proyek2.data_class.DcKategori
import com.example.tugas_proyek2.service_layers.KategoriService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentFormKategori.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentFormKategori : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var etNama : EditText
    private lateinit var btnCancel : Button
    private lateinit var btnAdd : Button

    private val kategoriService = KategoriService()

    private val kategoriList = mutableListOf<DcKategori>()

    private lateinit var kategoriAdapter: ArrayAdapter<String>

    companion object{
        fun newInstance() = FragmentFormKategori()
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
        val view = inflater.inflate(R.layout.fragment_form_kategori, container, false)

        initViews(view)
        setupListeners()

        return view
    }

    private fun initViews(view: View){
        etNama = view.findViewById(R.id.et_nama_kategori_new)
        btnAdd = view.findViewById(R.id.btn_add_kategori_new)
        btnCancel = view.findViewById(R.id.btn_cancel_kategori_new)
    }

    private fun setupListeners(){
        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnAdd.setOnClickListener {
            addNewKategori()
        }
    }

    private fun addNewKategori() {
        if (!validateInput()) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                btnAdd.isEnabled = false
                btnAdd.text = "Menambahkan..."

                val newKategori = DcKategori(
                    nama = etNama.text.toString()
                )

                val kategoriId = kategoriService.addKategori(newKategori)

                if (kategoriId != null) {
                    Toast.makeText(requireContext(), "Kategori berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    clearForm()
                }else {
                    Toast.makeText(requireContext(), "Gagal menambahkan kategori", Toast.LENGTH_SHORT).show()
                }
            }catch(e: Exception){
                Log.e("FragmentFormKategori", "Error adding new kategori", e)
                Toast.makeText(requireContext(), "Gagal menambahkan kategori: ${e.message}", Toast.LENGTH_SHORT).show()
            }finally {
                btnAdd.isEnabled = true
                btnAdd.text = "Add"
            }
        }
    }

    private fun validateInput(): Boolean{
        if(etNama.text.toString().trim().isEmpty()){
            etNama.error = "Nama kategori harus diisi"
            etNama.requestFocus()
            return false
        }

        return true
    }

    private fun clearForm(){
        etNama.text.clear()
    }
}