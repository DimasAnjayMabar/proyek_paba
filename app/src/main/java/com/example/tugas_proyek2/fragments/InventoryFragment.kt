package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.databinding.FragmentInventoryBinding

class InventoryFragment : Fragment() {
    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuItems()
    }

    private fun setupMenuItems() {


        // 1. Menu List Produk
        binding.menuListProduk.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_fragmentListProduk)
        }

        // 2. Menu Distributor
        binding.menuDistributor.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_fragmentListDistributor)
        }

        // 3. Menu Kategori
        binding.menuKategori.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_fragmentKategori)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}