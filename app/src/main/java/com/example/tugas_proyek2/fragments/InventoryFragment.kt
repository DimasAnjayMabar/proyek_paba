package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.databinding.FragmentInventoryBinding
import com.example.tugas_proyek2.databinding.ItemMainMenuBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class InventoryFragment : Fragment() {
    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!
    private var param1: String? = null
    private var param2: String? = null

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
        // Inflate the layout for this fragment dan inisialisasi binding
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuItems()
    }

    private fun setupMenuItems() {
        // Menu List Produk
        val listProdukMenuView = binding.menuListProduk.root
        val listProdukBinding = ItemMainMenuBinding.bind(listProdukMenuView)
        listProdukBinding.ivMenuIcon.setImageResource(R.drawable.product)
        listProdukBinding.tvMenuTitle.text = "List Produk"

        // Menu Distributor (diperbaiki ID-nya)
        val distributorMenuView = binding.menuDistributor.root
        val distributorBinding = ItemMainMenuBinding.bind(distributorMenuView)
        distributorBinding.ivMenuIcon.setImageResource(R.drawable.distributor)
        distributorBinding.tvMenuTitle.text = "List Distributor"

        // Menu Kategori (diperbaiki: gunakan binding.menuKategori, bukan menuDistributor)
        val kategoriMenuView = binding.menuKategori.root
        val kategoriBinding = ItemMainMenuBinding.bind(kategoriMenuView)
        kategoriBinding.ivMenuIcon.setImageResource(R.drawable.kategori) // Ganti dengan drawable kategori jika ada
        kategoriBinding.tvMenuTitle.text = "Kategori"

        // Tambahkan click listener dengan Navigation Component
        listProdukMenuView.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_fragmentListProduk)
        }

        distributorMenuView.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_fragmentListDistributor)
        }

        kategoriMenuView.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_fragmentKategori)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment InventoryFragment.
         */
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            InventoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}