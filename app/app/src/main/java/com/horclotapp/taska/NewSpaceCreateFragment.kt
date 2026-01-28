package com.horclotapp.taska

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.InputFilter
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast

class NewSpaceCreateFragment : Fragment(R.layout.fragment_new_space_create) {

    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etCode: EditText
    private lateinit var btnCopy: ImageButton
    private lateinit var btnGenerate: ImageButton
    private lateinit var btnContinue: Button

    // Переменные для дальнейшей работы
    private var spaceName: String = ""
    private var spaceDescription: String = ""
    private var spaceCode: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etName = view.findViewById(R.id.etName)
        etDescription = view.findViewById(R.id.etDescription)
        etCode = view.findViewById(R.id.etCode)
        btnCopy = view.findViewById(R.id.btnCopy)
        btnGenerate = view.findViewById(R.id.btnGenerate)
        btnContinue = view.findViewById(R.id.btnContinue)

        setupFilters()
        setupActions()
    }

    private fun setupFilters() {
        // Ограничения длины
        etName.filters = arrayOf(InputFilter.LengthFilter(60))
        etDescription.filters = arrayOf(InputFilter.LengthFilter(300))

        // Код: 7–20 символов, только [A-Za-z0-9]
        etCode.filters = arrayOf(
            InputFilter.LengthFilter(20),
            InputFilter { source, _, _, _, _, _ ->
                if (source.matches(Regex("[A-Za-z0-9]*"))) source else ""
            }
        )
    }

    private fun setupActions() {
        btnGenerate.setOnClickListener {
            etCode.setText(generateCode())
        }

        btnCopy.setOnClickListener {
            val cm = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("code", etCode.text.toString()))
            Toast.makeText(requireContext(), "Код скопирован", Toast.LENGTH_SHORT).show()
        }

        btnContinue.setOnClickListener {
            if (validate()) {
                spaceName = etName.text.toString().trim()
                spaceDescription = etDescription.text.toString().trim()
                spaceCode = etCode.text.toString().trim()

                requireParentFragment()
                    .childFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.spacesContainer,
                        NewSpaceInviteFragment.newInstance(
                            spaceName, spaceDescription, spaceCode
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }


    }

    private fun validate(): Boolean {
        val name = etName.text.toString().trim()
        val code = etCode.text.toString().trim()

        when {
            name.length < 3 -> {
                etName.error = "Минимум 3 символа"
                return false
            }
            code.length !in 7..20 -> {
                etCode.error = "Код должен быть от 7 до 20 символов"
                return false
            }
        }
        return true
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val len = (7..12).random()
        return (1..len)
            .map { chars.random() }
            .joinToString("")
    }
}
