package com.horclotapp.taska

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.horclotapp.taska.databinding.FragmentProfileBinding
import com.horclotapp.taska.databinding.ItemProfileActionBinding
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActions()
    }

    private fun setupActions() {
        setupItem(binding.actionLanguage, "Язык", "Русский", R.drawable.ic_language)
        setupItem(binding.actionTheme, "Тема", "Тёмная", R.drawable.ic_moon)
        setupItem(binding.actionNotifications, "Уведомления", "Включены", R.drawable.ic_notifications_black_24dp)
        setupItem(binding.actionPremium, "Premium", "Расширенные возможности", R.drawable.ic_star, true)
    }

    private fun setupItem(
        binding: ItemProfileActionBinding,
        title: String,
        subtitle: String,
        icon: Int,
        accent: Boolean = false
    ) {
        binding.actionIcon.setImageResource(icon)
        binding.actionTitle.text = title
        binding.actionSubtitle.text = subtitle
        binding.actionSubtitle.visibility = View.VISIBLE

        if (accent) {
            binding.actionIcon.setColorFilter(
                requireContext().getColor(R.color.accent_yellow)
            )
        }
    }


}