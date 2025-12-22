package com.example.tugas_proyek2.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager // Ganti ke Linear
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.tugas_proyek2.R
import com.example.tugas_proyek2.adapters.DistributorAdapter
import com.example.tugas_proyek2.data_class.DcDistributor
import com.example.tugas_proyek2.databinding.FragmentListDistributorBinding
// GridSpacingItemDecoration dihapus
import com.example.tugas_proyek2.service_layers.DistributorService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentListDistributor : Fragment() {

    private var _binding: FragmentListDistributorBinding? = null
    private val binding get() = _binding!!

    private lateinit var distributorAdapter: DistributorAdapter
    private lateinit var distributorRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var fabAddDistributor: FloatingActionButton
    private val distributorIdMap = mutableMapOf<DcDistributor, String>()
    private val distributorList = mutableListOf<DcDistributor>()
    private val filteredList = mutableListOf<DcDistributor>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListDistributorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupSwipeRefresh()
        setupFAB()
        loadDistributorsFromFirestore(showProgressBar = true)
    }

    private fun setupRecyclerView() {
        distributorRecyclerView = binding.recyclerViewDistributor

        distributorAdapter = DistributorAdapter(filteredList) { distributor ->
            navigateToDetailDistributor(distributor)
        }

        val layoutManager = LinearLayoutManager(requireContext())
        distributorRecyclerView.layoutManager = layoutManager
        distributorRecyclerView.adapter = distributorAdapter

        val animation = android.view.animation.AnimationUtils.loadLayoutAnimation(
            requireContext(),
            R.anim.layout_animation_fall_down
        )
        distributorRecyclerView.layoutAnimation = animation


        distributorRecyclerView.overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
    }

    private fun navigateToDetailDistributor(distributor: DcDistributor){
        val distributorId = distributorIdMap[distributor]

        if (distributorId != null) {
            val bundle = Bundle().apply {
                putString("distributor_id", distributorId)
            }

            try{
                findNavController().navigate(R.id.action_fragmentListDistributor_to_fragmentDetailDistributor, bundle)
            }catch (e: Exception) {
                Log.e("FragmentListDistributor", "Navigation error: ${e.message}")
                showSnackbar("Gagal membuka detail distributor")
            }
        }else{
            showSnackbar("Tidak dapat menemukan ID distributor")
        }
    }

    private fun setupSearchBar() {
        binding.searchViewDistributor.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterDistributors(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterDistributors(newText ?: "")
                return true
            }
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout = binding.swipeRefreshLayout

        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )

        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(android.R.color.white)

        swipeRefreshLayout.setOnRefreshListener {
            loadDistributorsFromFirestore(showProgressBar = false)
        }
    }

    private fun setupFAB() {
        fabAddDistributor = binding.fabAddDistributor
        fabAddDistributor.setOnClickListener {
            navigateToFormDistributor()
        }
    }

    private fun navigateToFormDistributor(){
        try{
            findNavController().navigate(R.id.action_fragmentListDistributor_to_fragmentFormDistributor)
        }catch (e: Exception){
            Log.e("FragmentListDistributor", "Navigation to form error: ${e.message}")
            showSnackbar("Gagal membuka form distributor")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).apply {
            setAction("OK") {}
            setActionTextColor(resources.getColor(android.R.color.white, null))
        }.show()
    }

    private fun filterDistributors(query: String) {
        filteredList.clear()

        val filtered = if (query.isEmpty()) {
            distributorList
        } else {
            distributorList.filter {
                it.nama?.contains(query, ignoreCase = true) == true
            }
        }

        filteredList.clear()
        filteredList.addAll(filtered)
        distributorAdapter.notifyDataSetChanged()

        binding.textEmptyDistributor.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewDistributor.visibility = if (filteredList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun loadDistributorsFromFirestore(showProgressBar: Boolean = true) {
        if (showProgressBar) {
            binding.progressBarDistributor.visibility = View.VISIBLE
            binding.swipeRefreshLayout.isRefreshing = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val distributorsWithIds = DistributorService().getAllDistributorsWithIds()

                withContext(Dispatchers.Main) {
                    distributorList.clear()
                    filteredList.clear()
                    distributorIdMap.clear()

                    distributorsWithIds.forEach { (documentId, distributor) ->
                        distributorList.add(distributor)
                        distributorIdMap[distributor] = documentId
                    }

                    filteredList.addAll(distributorList)
                    distributorAdapter.notifyDataSetChanged()

                    if (distributorList.isNotEmpty()) {
                        distributorRecyclerView.scheduleLayoutAnimation()
                    }

                    binding.progressBarDistributor.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false

                    if (distributorList.isEmpty()){
                        binding.textEmptyDistributor.text = "Belum ada distributor"
                        binding.textEmptyDistributor.visibility = View.VISIBLE
                        binding.recyclerViewDistributor.visibility = View.GONE
                        fabAddDistributor.show()
                    }else{
                        binding.textEmptyDistributor.visibility = View.GONE
                        binding.recyclerViewDistributor.visibility = View.VISIBLE
                        fabAddDistributor.show()
                    }
                }
            } catch (e: Exception) {
                Log.e("FragmentListDistributor", "Error loading distributors: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.progressBarDistributor.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.textEmptyDistributor.text = "Gagal memuat distributor: ${e.message}"
                    binding.textEmptyDistributor.visibility = View.VISIBLE
                    binding.recyclerViewDistributor.visibility = View.GONE
                    showSnackbar("Gagal memuat distributor")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}