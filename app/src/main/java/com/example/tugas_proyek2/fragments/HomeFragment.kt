package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.databinding.FragmentHomeBinding
import com.example.tugas_proyek2.databinding.ItemMainMenuBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuItems()
    }

    private fun setupMenuItems() {
        // Menu Inventory
        val inventoryMenuView = binding.menuInventory.root
        val inventoryBinding = ItemMainMenuBinding.bind(inventoryMenuView)
        inventoryBinding.ivMenuIcon.setImageResource(R.drawable.inventory)
        inventoryBinding.tvMenuTitle.text = "Inventory"

        // Menu Cashier
        val cashierMenuView = binding.menuCashier.root
        val cashierBinding = ItemMainMenuBinding.bind(cashierMenuView)
        cashierBinding.ivMenuIcon.setImageResource(R.drawable.cashier)
        cashierBinding.tvMenuTitle.text = "Kasir"

        // Tambahkan click listener dengan Navigation Component
        inventoryMenuView.setOnClickListener {
            // Navigasi ke FragmentListProduk menggunakan action
            findNavController().navigate(R.id.action_homeFragment_to_inventoryFragment)
        }

        cashierMenuView.setOnClickListener {
            // Navigasi ke FragmentCashier menggunakan action
            findNavController().navigate(R.id.action_homeFragment_to_fragmentCashier)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}