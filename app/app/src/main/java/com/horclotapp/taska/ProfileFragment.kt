package com.horclotapp.taska

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.horclotapp.taska.databinding.FragmentProfileBinding
import com.horclotapp.taska.databinding.ItemProfileActionBinding

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

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sharedPreferences = requireContext().getSharedPreferences("app_prefs", 0)

        loadUserProfile()
        setupActions()
        setupQuickActions()
        setupListeners()
        setupAccountInfo()
        setupAnimations()
        animateEntrance()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        user?.let {
            binding.profileName.text = it.displayName ?: "Пользователь"
            binding.profileEmail.text = it.email ?: "user@gmail.com"

            Glide.with(this)
                .load(it.photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_google)
                .into(binding.profileAvatar)

            // Проверяем статус Premium
            checkPremiumStatus()

            // Устанавливаем ID пользователя
            binding.userId.text = "ID: ${it.uid.take(8)}..."
        }
    }

    private fun checkPremiumStatus() {
        val isPremium = sharedPreferences.getBoolean("is_premium", false)

        if (isPremium) {
            binding.profileStatus.text = "Premium"
            binding.profileStatus.setChipIconResource(R.drawable.ic_star)
            binding.profileStatus.chipIconTint = resources.getColorStateList(R.color.accent_yellow)

            // Анимация для Premium статуса
            binding.profileStatus.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction {
                    binding.profileStatus.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .start()
                }
                .start()
        } else {
            binding.profileStatus.text = "Бесплатный"
            binding.profileStatus.setChipIconResource(R.drawable.ic_free)
            binding.profileStatus.chipIconTint = resources.getColorStateList(R.color.text_secondary)
        }
    }

    private fun setupActions() {
        setupItem(binding.actionLanguage, "Язык", "Русский", R.drawable.ic_language)
        setupItem(binding.actionTheme, "Тема", getCurrentTheme(), R.drawable.ic_moon)
        setupItem(binding.actionNotifications, "Уведомления", "Включены", R.drawable.ic_notifications_black_24dp)

        // Безопасность
        setupItem(binding.actionChangePassword, "Сменить пароль", "Последнее изменение: 01.01.24", R.drawable.ic_key)
        setupItem(binding.actionTwoFactor, "2FA", "Выключена", R.drawable.ic_shield)
        setupItem(binding.actionDevices, "Устройства", "Активных: 1", R.drawable.ic_device)

        // Поддержка
        setupItem(binding.actionHelp, "Помощь", "FAQ и инструкции", R.drawable.ic_help)
        setupItem(binding.actionFeedback, "Обратная связь", "Напишите нам", R.drawable.ic_feedback)
        setupItem(binding.actionRate, "Оценить приложение", "В магазине", R.drawable.ic_star, true)
    }

    private fun getCurrentTheme(): String {
        return when (sharedPreferences.getString("theme", "dark")) {
            "dark" -> "Тёмная"
            "light" -> "Светлая"
            else -> "Системная"
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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

        // Анимация при нажатии
        binding.root.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(100)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
            }
            false
        }

        binding.root.setOnClickListener {
            // Анимация клика
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    handleActionClick(title)
                }
                .start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupQuickActions() {
        listOf(binding.btnEditProfile, binding.btnStatistics, binding.btnSecurity).forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.animate()
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .setDuration(100)
                            .start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                }
                false
            }
        }

        binding.btnEditProfile.setOnClickListener {
            animateClick(it) { navigateToEditProfile() }
        }

        binding.btnStatistics.setOnClickListener {
            animateClick(it) { navigateToStatistics() }
        }

        binding.btnSecurity.setOnClickListener {
            animateClick(it) { navigateToSecurity() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.logout.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
            }
            false
        }

        binding.logout.setOnClickListener {
            animateClick(it) { showLogoutDialog() }
        }
    }

    private fun setupAccountInfo() {
        try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            binding.appVersion.text = "Версия $versionName"

            // Анимация появления информации
            binding.appVersion.alpha = 0f
            binding.userId.alpha = 0f
            binding.appVersion.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(300)
                .start()
            binding.userId.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(400)
                .start()
        } catch (e: Exception) {
            binding.appVersion.text = "Версия 1.0.0"
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAnimations() {
        val cards = listOf(
            binding.root.findViewById<MaterialCardView>(R.id.profileCard),
            binding.root.findViewById<MaterialCardView>(R.id.quickActionsCard),
            binding.root.findViewById<MaterialCardView>(R.id.actionsCard),
            binding.root.findViewById<MaterialCardView>(R.id.securityCard),
            binding.root.findViewById<MaterialCardView>(R.id.supportCard),
            binding.root.findViewById<MaterialCardView>(R.id.logoutCard)
        )

        cards.forEach { card ->
            card?.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            v.animate()
                                .translationZ(12f)
                                .setDuration(100)
                                .start()
                        } else {
                            card.cardElevation = 8f
                        }
                        v.animate()
                            .scaleX(0.99f)
                            .scaleY(0.99f)
                            .setDuration(100)
                            .start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            v.animate()
                                .translationZ(0f)
                                .setDuration(100)
                                .start()
                        } else {
                            card.cardElevation = 2f
                        }
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                    }
                }
                false
            }
        }
    }

    private fun animateEntrance() {
        // Анимация появления элементов
        val views = listOf(
            binding.profileCard,
            binding.quickActionsCard,
            binding.actionsCard,
            binding.securityCard,
            binding.supportCard,
            binding.logoutCard
        )

        views.forEachIndexed { index, view ->
            view?.alpha = 0f
            view?.translationY = 50f
            view?.animate()
                ?.alpha(1f)
                ?.translationY(0f)
                ?.setDuration(300)
                ?.setStartDelay((index * 100).toLong())
                ?.start()
        }
    }

    private fun animateClick(view: View, action: () -> Unit) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
                action()
            }
            .start()
    }

    private fun handleActionClick(action: String) {
        when (action) {
            "Язык" -> showLanguageDialog()
            "Тема" -> showThemeDialog()
            "Уведомления" -> navigateToNotifications()
            "Сменить пароль" -> navigateToChangePassword()
            "2FA" -> navigateToTwoFactorAuth()
            "Устройства" -> navigateToDevices()
            "Помощь" -> navigateToHelp()
            "Обратная связь" -> navigateToFeedback()
            "Оценить приложение" -> rateApp()
        }
    }

    private fun showLogoutDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                auth.signOut()
                // Навигация на экран авторизации
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun navigateToEditProfile() {
        // Реализация навигации
    }

    private fun navigateToStatistics() {
        // Реализация навигации
    }

    private fun navigateToSecurity() {
        // Реализация навигации
    }

    private fun showLanguageDialog() {
        // Диалог выбора языка
    }

    private fun showThemeDialog() {
        // Диалог выбора темы
    }

    private fun navigateToNotifications() {
        // Навигация на уведомления
    }

    private fun navigateToChangePassword() {
        // Навигация на смену пароля
    }

    private fun navigateToTwoFactorAuth() {
        // Навигация на 2FA
    }

    private fun navigateToDevices() {
        // Навигация на устройства
    }

    private fun navigateToHelp() {
        // Навигация на помощь
    }

    private fun navigateToFeedback() {
        // Навигация на обратную связь
    }

    private fun rateApp() {
        // Открытие магазина приложений
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}