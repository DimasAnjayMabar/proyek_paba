package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.adapters.KategoriAdapter
import com.example.tugas_proyek2.data_class.DcKategori
import com.example.tugas_proyek2.databinding.FragmentListKategoriBinding
import com.example.tugas_proyek2.misc.GridSpacingItemDecoration
import com.example.tugas_proyek2.service_layers.KategoriService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.forEach

class FragmentListKategori : Fragment() {

    private var _binding: FragmentListKategoriBinding? = null
    private val binding get() = _binding!!

    private lateinit var kategoriAdapter: KategoriAdapter
    private lateinit var kategoriRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var fabAddKategori : FloatingActionButton
    private val kategoriMap = mutableMapOf<DcKategori, String>() // Simpan Map untuk akses ID
    private val kategoriList = mutableListOf<DcKategori>() // Hanya untuk adapter
    private val filteredList = mutableListOf<DcKategori>() // Untuk search

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListKategoriBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupSwipeRefresh()
        setupFAB()
        loadCategoriesFromFirestore(showProgressBar = true)
    }

    private fun setupRecyclerView() {
        kategoriRecyclerView = binding.recyclerViewKategori

        kategoriAdapter = KategoriAdapter(filteredList){ kategori ->
            navigateToDetailKategori(kategori)
        }

        val layoutManager = GridLayoutManager(requireContext(), 3)
        kategoriRecyclerView.layoutManager = layoutManager
        kategoriRecyclerView.adapter = kategoriAdapter

        val animation = android.view.animation.AnimationUtils.loadLayoutAnimation(
            requireContext(),
            R.anim.layout_animation_fall_down
        )
        kategoriRecyclerView.layoutAnimation = animation

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        kategoriRecyclerView.addItemDecoration(GridSpacingItemDecoration(3, spacingInPixels, true))

        kategoriRecyclerView.overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
    }

    private fun navigateToDetailKategori(kategori: DcKategori){
        val kategoriId = kategoriMap[kategori]

        if (kategoriId != null) {
            val bundle = Bundle().apply {
                putString("kategori_id", kategoriId)
            }

            try {
                findNavController().navigate(R.id.action_fragmentListKategori_to_fragmentDetailKategori, bundle)
            }catch(e: Exception){
                Log.e("FragmentListKategori", "Navigation error: ${e.message}")
                showSnackbar("Gagal membuka detail kategori")
            }
        }
    }

    private fun setupSearchBar() {
        binding.searchViewKategori.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterKategori(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterKategori(newText ?: "")
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout

        // Set warna loading indicator
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        // Set warna background
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.white)

        // Listener untuk refresh
        swipeRefreshLayout.setOnRefreshListener {
            Log.d("FragmentKategori", "Swipe refresh triggered")
            loadCategoriesFromFirestore(showProgressBar = false)
        }
    }

    private fun setupFAB() {
        fabAddKategori = binding.fabAddKategori
        fabAddKategori.setOnClickListener {
            navigateToFormKategori()
        }
    }

    private fun navigateToFormKategori(){
        try{
            findNavController().navigate(R.id.action_fragmentListKategori_to_fragmentFormKategori3)
        }catch (e: Exception){
            Log.e("FragmentListKategori", "Navigation to form error: ${e.message}")
            showSnackbar("Gagal membuka detai kategori")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).apply {
            setAction("OK") {
                // Aksi ketika tombol OK diklik
            }
            setActionTextColor(resources.getColor(android.R.color.white, null))
        }.show()
    }

    private fun filterKategori(query: String) {
        filteredList.clear()

        val filtered = if (query.isEmpty()) {
            kategoriList
        } else {
            kategoriList.filter {
                it.nama?.contains(query, ignoreCase = true) == true
            }
        }

        filteredList.clear()
        filteredList.addAll(filtered)
        kategoriAdapter.notifyDataSetChanged()

        // Tampilkan pesan jika tidak ada kategori
        binding.textEmptyKategori.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewKategori.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadCategoriesFromFirestore(showProgressBar: Boolean = true) {
        if (showProgressBar) {
            binding.progressBarKategori.visibility = View.VISIBLE
            binding.swipeRefreshLayout.isRefreshing = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val categoriesWithIds = KategoriService().getAllKategorisWithIds()

                withContext(Dispatchers.Main) {
                    kategoriList.clear()
                    filteredList.clear()
                    kategoriMap.clear()

                    categoriesWithIds.forEach { (documentId, kategori) ->
                        kategoriList.add(kategori)
                        kategoriMap[kategori] = documentId
                    }

                    filteredList.addAll(kategoriList)
                    kategoriAdapter.notifyDataSetChanged()

                    if (kategoriList.isNotEmpty()) {
                        kategoriRecyclerView.scheduleLayoutAnimation()
                    }

                    binding.progressBarKategori.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (kategoriList.isEmpty()){
                        binding.textEmptyKategori.text = "Belum ada kategori"
                        binding.textEmptyKategori.visibility = View.VISIBLE
                        binding.recyclerViewKategori.visibility = View.GONE
                        fabAddKategori.show()
                    }else{
                        binding.textEmptyKategori.visibility = View.GONE
                        binding.recyclerViewKategori.visibility = View.VISIBLE
                        fabAddKategori.show()

                        if (!showProgressBar){
                            showSnackbar("${kategoriList.size} kategori dimuat ulang")
                        }

                        Log.d("FragmentKategori", "Loaded ${kategoriList.size} categories")
                    }
                }


            } catch (e: Exception) {
                Log.e("FragmentKategori", "Error loading categories: ${e.message}")

                withContext(Dispatchers.Main) {
                    binding.progressBarKategori.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    binding.textEmptyKategori.text = "Gagal memuat kategori: ${e.message}"
                    binding.textEmptyKategori.visibility = View.VISIBLE
                    binding.recyclerViewKategori.visibility = View.GONE

                    // Show error snackbar
                    showSnackbar("Gagal memuat kategori")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}