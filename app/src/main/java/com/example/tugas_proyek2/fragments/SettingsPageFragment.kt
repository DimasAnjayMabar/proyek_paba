package com.example.tugas_proyek2.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tugas_proyek2.R

class SettingsPageFragment : Fragment() {

    private lateinit var tvCurrentMinStock: TextView
    private var currentMinStock = 10 // Nilai default
    private lateinit var sharedPreferences: SharedPreferences

    // Key untuk SharedPreferences
    companion object {
        private const val PREF_NAME = "app_settings"
        private const val KEY_MIN_STOCK = "minimal_stock"
        private const val DEFAULT_MIN_STOCK = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inisialisasi SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        // Baca nilai stok minimal dari SharedPreferences
        currentMinStock = sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings_page, container, false)

        // Inisialisasi view
        tvCurrentMinStock = view.findViewById(R.id.tvCurrentMinStock)
        val cardMinStock = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardMinStock)

        // Set click listener untuk card
        cardMinStock.setOnClickListener {
            showMinStockDialog()
        }

        // Update tampilan dengan nilai saat ini
        updateMinStockDisplay()

        return view
    }

    private fun updateMinStockDisplay() {
        tvCurrentMinStock.text = "Stok minimal saat ini: $currentMinStock"
    }

    private fun showMinStockDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_min_stock, null)

        val editTextMinStock = dialogView.findViewById<EditText>(R.id.editTextMinStock)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        // Set nilai saat ini ke EditText
        editTextMinStock.setText(currentMinStock.toString())

        // Buat dialog
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Atur Stok Minimal")
            .setMessage("Masukkan jumlah stok minimal untuk notifikasi:")
            .setView(dialogView)
            .create()

        // Setup button listeners
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val input = editTextMinStock.text.toString().trim()

            if (input.isNotEmpty()) {
                try {
                    val newValue = input.toInt()
                    if (newValue > 0 && newValue <= 9999) { // Batas maksimal 9999
                        // Simpan ke SharedPreferences
                        saveMinStockToPrefs(newValue)

                        Toast.makeText(
                            requireContext(),
                            "Stok minimal berhasil diubah menjadi $newValue",
                            Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    } else if (newValue <= 0) {
                        Toast.makeText(
                            requireContext(),
                            "Masukkan angka lebih besar dari 0",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Maksimal angka adalah 9999",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(
                        requireContext(),
                        "Masukkan angka yang valid",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Masukkan jumlah stok minimal",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialog.show()
    }

    private fun saveMinStockToPrefs(value: Int) {
        with(sharedPreferences.edit()) {
            putInt(KEY_MIN_STOCK, value)
            apply() // Async save, gunakan commit() jika perlu synchronous
        }
        currentMinStock = value
        updateMinStockDisplay()
    }

    // Fungsi untuk mendapatkan nilai stok minimal dari luar fragment
    fun getCurrentMinStock(): Int {
        return sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)
    }
}