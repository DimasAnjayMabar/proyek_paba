package com.example.tugas_proyek2.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tugas_proyek2.R
import com.google.android.material.textfield.TextInputEditText

class SettingsPageFragment : Fragment() {

    private lateinit var tvCurrentMinStock: TextView
    private var currentMinStock = 10
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREF_NAME = "app_settings"
        private const val KEY_MIN_STOCK = "minimal_stock"
        private const val DEFAULT_MIN_STOCK = 10
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        currentMinStock = sharedPreferences.getInt(KEY_MIN_STOCK, DEFAULT_MIN_STOCK)

        val view = inflater.inflate(R.layout.fragment_settings_page, container, false)

        tvCurrentMinStock = view.findViewById(R.id.tvCurrentMinStock)
        val cardMinStock = view.findViewById<View>(R.id.cardMinStock)

        val btnChangeMinStock = view.findViewById<Button>(R.id.btnChangeMinStock)

        btnChangeMinStock.setOnClickListener {
            showMinStockDialog()
        }

        cardMinStock.setOnClickListener {
            showMinStockDialog()
        }

        updateMinStockDisplay()

        return view
    }

    private fun updateMinStockDisplay() {
        // Tampilkan angka saja
        tvCurrentMinStock.text = currentMinStock.toString()
    }

    private fun showMinStockDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_min_stock, null)

        val etMinStock = dialogView.findViewById<TextInputEditText>(R.id.etMinStock)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        etMinStock.setText(currentMinStock.toString())

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Background transparan agar sudut rounded terlihat
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val input = etMinStock.text.toString().trim()

            if (input.isNotEmpty()) {
                try {
                    val newValue = input.toInt()
                    if (newValue > 0 && newValue <= 9999) {
                        saveMinStockToPrefs(newValue)
                        Toast.makeText(requireContext(), "Stok minimal diperbarui", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        etMinStock.error = "Angka harus 1 - 9999"
                    }
                } catch (e: NumberFormatException) {
                    etMinStock.error = "Input tidak valid"
                }
            } else {
                etMinStock.error = "Wajib diisi"
            }
        }

        dialog.show()
    }

    private fun saveMinStockToPrefs(value: Int) {
        with(sharedPreferences.edit()) {
            putInt(KEY_MIN_STOCK, value)
            apply()
        }
        currentMinStock = value
        updateMinStockDisplay()

        (activity as? com.example.tugas_proyek2.MainActivity)?.checkLowStockGlobally()
    }
}