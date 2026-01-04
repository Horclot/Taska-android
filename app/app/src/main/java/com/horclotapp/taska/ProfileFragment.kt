package com.horclotapp.taska

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.horclotapp.taska.databinding.FragmentProfileBinding
import com.horclotapp.taska.databinding.ItemProfileActionBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var userListener: ListenerRegistration? = null
    private var premiumListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null

    // Модели данных
    private var user: User? = null
    private var premiumData: PremiumData? = null
    private var userSettings: UserSettings? = null

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

        setupQuickActions()
        setupListeners()
        setupAccountInfo()
        setupAnimations()
        setupRealTimeListeners()
        animateEntrance()
        loadInitialData()
    }

    override fun onResume() {
        super.onResume()
        // Обновляем данные при возвращении на экран
        loadInitialData()
    }

    override fun onPause() {
        super.onPause()
        // Сохраняем локальные настройки при уходе с экрана
        saveSettingsLocally()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Отписываемся от слушателей
        userListener?.remove()
        premiumListener?.remove()
        settingsListener?.remove()
        _binding = null
    }

    // ===================== ОСНОВНЫЕ МЕТОДЫ ЗАГРУЗКИ =====================

    private fun loadInitialData() {
        val user = auth.currentUser
        user?.let {
            // Загружаем все данные асинхронно
            lifecycleScope.launch {
                try {
                    loadUserProfile(it.uid)
                    loadPremiumStatus(it.uid)
                    loadUserSettings(it.uid)
                } catch (e: Exception) {
                    // Обработка ошибок
                }
            }
        }
    }

    private suspend fun loadUserProfile(userId: String) {
        try {
            val document = firestore.collection("users").document(userId).get().await()
            if (document.exists()) {
                user = document.toObject(User::class.java)
                user?.let { updateProfileUI(it) }
            }
        } catch (e: Exception) {
            // Используем данные из Firebase Auth как fallback
            auth.currentUser?.let { firebaseUser ->
                updateProfileUIFromAuth(firebaseUser)
            }
        }
    }

    private suspend fun loadPremiumStatus(userId: String) {
        try {
            val document = firestore.collection("premium_subscriptions")
                .document(userId)
                .get()
                .await()

            premiumData = if (document.exists()) {
                document.toObject(PremiumData::class.java)
            } else {
                // Создаем запись о бесплатном статусе
                PremiumData(
                    userId = userId,
                    isPremium = false,
                    subscriptionType = "free",
                    createdAt = System.currentTimeMillis()
                )
            }

            premiumData?.let { updatePremiumUI(it) }
        } catch (e: Exception) {
            // Fallback на бесплатный статус
            updatePremiumUI(PremiumData(userId = userId, isPremium = false))
        }
    }

    private suspend fun loadUserSettings(userId: String) {
        try {
            val document = firestore.collection("user_settings")
                .document(userId)
                .get()
                .await()

            userSettings = if (document.exists()) {
                document.toObject(UserSettings::class.java)
            } else {
                // Создаем настройки по умолчанию
                UserSettings().apply {
                    saveUserSettings(userId, this)
                }
            }

            userSettings?.let { updateSettingsUI(it) }
        } catch (e: Exception) {
            // Настройки по умолчанию
            updateSettingsUI(UserSettings())
        }
    }

    // ===================== REAL-TIME LISTENERS =====================

    private fun setupRealTimeListeners() {
        val userId = auth.currentUser?.uid ?: return

        // Слушатель данных пользователя
        userListener = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                error?.let {
                    // Обработка ошибки
                    return@addSnapshotListener
                }

                snapshot?.let { doc ->
                    if (doc.exists()) {
                        user = doc.toObject(User::class.java)
                        user?.let { updateProfileUI(it) }
                    }
                }
            }

        // Слушатель Premium статуса
        premiumListener = firestore.collection("premium_subscriptions")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                error?.let { return@addSnapshotListener }

                snapshot?.let { doc ->
                    premiumData = if (doc.exists()) {
                        doc.toObject(PremiumData::class.java)
                    } else {
                        PremiumData(userId = userId, isPremium = false)
                    }
                    premiumData?.let { updatePremiumUI(it) }
                }
            }

        // Слушатель настроек
        settingsListener = firestore.collection("user_settings")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                error?.let { return@addSnapshotListener }

                snapshot?.let { doc ->
                    userSettings = doc.toObject(UserSettings::class.java)
                    userSettings?.let { updateSettingsUI(it) }
                }
            }
    }

    // ===================== ОБНОВЛЕНИЕ UI =====================

    private fun updateProfileUI(user: User) {
        binding.profileName.text = user.displayName.ifEmpty { "Пользователь" }
        binding.profileEmail.text = user.email.ifEmpty { "user@gmail.com" }
        binding.userId.text = "ID: ${user.userId.take(8)}..."

        // Загрузка аватара
        user.photoUrl.takeIf { it.isNotEmpty() }?.let { photoUrl ->
            Glide.with(this)
                .load(photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_google)
                .into(binding.profileAvatar)
        }

        // Обновляем статистику если есть
        updateStatisticsUI(user)
    }

    private fun updateProfileUIFromAuth(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        binding.profileName.text = firebaseUser.displayName ?: "Пользователь"
        binding.profileEmail.text = firebaseUser.email ?: "user@gmail.com"
        binding.userId.text = "ID: ${firebaseUser.uid.take(8)}..."

        firebaseUser.photoUrl?.let { photoUrl ->
            Glide.with(this)
                .load(photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_google)
                .into(binding.profileAvatar)
        }
    }

    private fun updatePremiumUI(premiumData: PremiumData) {
        if (premiumData.isPremium) {
            binding.profileStatus.text = "Premium"
            binding.profileStatus.setChipIconResource(R.drawable.ic_star)
            binding.profileStatus.chipIconTint = resources.getColorStateList(R.color.accent_yellow)

            // Показываем дату окончания подписки если есть
            premiumData.expiresAt?.let { expiresAt ->
                val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(expiresAt))
                binding.profileStatus.text = "Premium до $date"
            }

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

    private fun updateStatisticsUI(user: User) {
        // Здесь можно добавить отображение статистики
        // Например, в подзаголовках элементов
    }

    private fun updateSettingsUI(settings: UserSettings) {
        // Обновляем UI в соответствии с настройками
        setupActions(settings)
    }

    // ===================== НАСТРОЙКИ =====================

    private fun setupActions(settings: UserSettings) {

        // Язык
        setupItem(binding.actionLanguage, "Язык",
            when (settings.language) {
                "ru" -> "Русский"
                "en" -> "English"
                else -> "Русский"
            },
            R.drawable.ic_language
        ) {
            showLanguageDialog(settings)
        }

        // Тема
        setupItem(binding.actionTheme, "Тема",
            if (settings.darkMode) "Тёмная" else "Светлая",
            R.drawable.ic_moon
        ) {
            showThemeDialog(settings)
        }

        // Уведомления
        setupItem(binding.actionNotifications, "Уведомления",
            if (settings.notificationsEnabled) "Включены" else "Выключены",
            R.drawable.ic_notifications_black_24dp
        ) {
            toggleNotifications(settings)
        }

        // Поддержка
        setupItem(binding.actionHelp, "Помощь", "FAQ и инструкции", R.drawable.ic_help) {
            navigateToHelp()
        }

        setupItem(binding.actionFeedback, "Обратная связь", "Напишите нам", R.drawable.ic_feedback) {
            navigateToFeedback()
        }

        setupItem(binding.actionRate, "Оценить приложение", "В магазине", R.drawable.ic_star, true) {
            rateApp()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupItem(
        binding: ItemProfileActionBinding,
        title: String,
        subtitle: String,
        icon: Int,
        accent: Boolean = false,
        onClick: () -> Unit
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

        binding.root.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            false
        }

        binding.root.setOnClickListener {
            animateClick(it) { onClick() }
        }
    }

    // ===================== ОБРАБОТКА НАСТРОЕК =====================

    private fun showLanguageDialog(settings: UserSettings) {
        val languages = arrayOf("Русский", "English")
        val currentIndex = if (settings.language == "en") 1 else 0

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите язык")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val newLanguage = if (which == 1) "en" else "ru"
                updateLanguage(newLanguage)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showThemeDialog(settings: UserSettings) {
        val themes = arrayOf("Светлая", "Тёмная", "Системная")
        val currentIndex = when {
            !settings.darkMode -> 0
            settings.darkMode && !settings.followSystemTheme -> 1
            else -> 2
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите тему")
            .setSingleChoiceItems(themes, currentIndex) { dialog, which ->
                when (which) {
                    0 -> updateTheme(false, false) // Светлая
                    1 -> updateTheme(true, false)  // Тёмная
                    2 -> updateTheme(true, true)   // Системная
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toggleNotifications(settings: UserSettings) {
        val newValue = !settings.notificationsEnabled
        updateNotifications(newValue)
    }

    // ===================== ОБНОВЛЕНИЕ НАСТРОЕК В FIRESTORE =====================

    private fun updateLanguage(language: String) {
        val userId = auth.currentUser?.uid ?: return
        userSettings = userSettings?.copy(language = language)

        firestore.collection("user_settings")
            .document(userId)
            .update("language", language)
            .addOnSuccessListener {
                // Успешно обновлено
                binding.actionLanguage.actionSubtitle.text =
                    when (language) {
                        "en" -> "English"
                        else -> "Русский"
                    }
            }
            .addOnFailureListener {
                // Ошибка, можно показать сообщение
            }
    }

    private fun updateTheme(darkMode: Boolean, followSystem: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        userSettings = userSettings?.copy(
            darkMode = darkMode,
            followSystemTheme = followSystem
        )

        firestore.collection("user_settings")
            .document(userId)
            .update(
                mapOf(
                    "darkMode" to darkMode,
                    "followSystemTheme" to followSystem
                )
            )
            .addOnSuccessListener {
                // Меняем тему приложения
                applyTheme(darkMode, followSystem)
                binding.actionTheme.actionSubtitle.text =
                    if (followSystem) "Системная" else if (darkMode) "Тёмная" else "Светлая"
            }
    }

    private fun updateNotifications(enabled: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        userSettings = userSettings?.copy(notificationsEnabled = enabled)

        firestore.collection("user_settings")
            .document(userId)
            .update("notificationsEnabled", enabled)
            .addOnSuccessListener {
                // Обновляем UI
                binding.actionNotifications.actionSubtitle.text =
                    if (enabled) "Включены" else "Выключены"
            }
    }

    private suspend fun saveUserSettings(userId: String, settings: UserSettings) {
        try {
            firestore.collection("user_settings")
                .document(userId)
                .set(settings)
                .await()
        } catch (e: Exception) {
            // Обработка ошибки
        }
    }

    // ===================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====================

    private fun saveSettingsLocally() {
        // Сохраняем настройки локально для офлайн-работы
        userSettings?.let { settings ->
            val prefs = requireContext().getSharedPreferences("user_settings", 0)
            with(prefs.edit()) {
                putBoolean("darkMode", settings.darkMode)
                putBoolean("notificationsEnabled", settings.notificationsEnabled)
                putString("language", settings.language)
                apply()
            }
        }
    }

    private fun applyTheme(darkMode: Boolean, followSystem: Boolean) {
        // Применяем тему в приложении
        // Реализация зависит от вашей архитектуры приложения
        if (followSystem) {
            // Следуем системной теме
        } else {
            // Применяем выбранную тему
        }
    }

    // ===================== ОСТАЛЬНЫЕ МЕТОДЫ =====================

    @SuppressLint("ClickableViewAccessibility")
    private fun setupQuickActions() {
        listOf(binding.btnEditProfile, binding.btnStatistics, binding.btnSecurity).forEach { view ->
            view.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
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
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
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
            binding.appVersion.text = "Версия ${packageInfo.versionName}"

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
            binding.root.findViewById<MaterialCardView>(R.id.supportCard),
            binding.root.findViewById<MaterialCardView>(R.id.logoutCard)
        )

        cards.forEach { card ->
            card?.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            v.animate().translationZ(12f).setDuration(100).start()
                        } else {
                            card.cardElevation = 8f
                        }
                        v.animate().scaleX(0.99f).scaleY(0.99f).setDuration(100).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            v.animate().translationZ(0f).setDuration(100).start()
                        } else {
                            card.cardElevation = 2f
                        }
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }
                }
                false
            }
        }
    }

    private fun animateEntrance() {
        val views = listOf(
            binding.profileCard,
            binding.quickActionsCard,
            binding.actionsCard,
            binding.supportCard,
            binding.logoutCard
        ).filterNotNull()

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay((index * 100).toLong())
                .start()
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

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                auth.signOut()
                // Навигация на экран авторизации
                navigateToLogin()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ===================== НАВИГАЦИЯ =====================

    private fun navigateToLogin() {
        // Реализация навигации на экран входа
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

    private fun navigateToHelp() {
        // Навигация на помощь
    }

    private fun navigateToFeedback() {
        // Навигация на обратную связь
    }

    private fun rateApp() {
        // Открытие магазина приложений
    }
}

// ===================== МОДЕЛИ ДАННЫХ =====================

data class PremiumData(
    val userId: String = "",
    val isPremium: Boolean = false,
    val subscriptionType: String = "free", // "free", "monthly", "yearly", "lifetime"
    val subscriptionId: String? = null,
    val purchaseToken: String? = null,
    val purchasedAt: Long? = null,
    val expiresAt: Long? = null,
    val autoRenewing: Boolean = false,
    val features: List<String> = emptyList(), // Список доступных фич
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Обновленная модель User (упрощенная)
data class User(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis(),
    val lastLoginDate: String = "", // yyyy-MM-dd
    val totalTasksCompleted: Int = 0,
    val totalTasksCreated: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalFocusTime: Long = 0, // в минутах
    val totalFocusSessions: Int = 0,
    val level: Int = 1,
    val experience: Int = 0,
    val achievements: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

// Обновленная модель UserSettings (упрощенная, без 2FA и устройств)
data class UserSettings(
    val userId: String = "",
    val notificationsEnabled: Boolean = true,
    val darkMode: Boolean = false,
    val followSystemTheme: Boolean = true,
    val language: String = "ru",
    val soundEffects: Boolean = true,
    var vibrationEnabled: Boolean = true,
    var dailyReminders: Boolean = false,
    var reminderTime: String = "20:00",
    var weekStart: String = "monday", // "monday" или "sunday"
    var dateFormat: String = "dd.MM.yyyy",
    var timeFormat: String = "24", // "24" или "12"
    var emailNotifications: Boolean = true,
    var pushNotifications: Boolean = true,
    var privacyShowStats: Boolean = true,
    var privacyShowAchievements: Boolean = true,
    var autoBackup: Boolean = false,
    var backupFrequency: String = "weekly", // "daily", "weekly", "monthly"
    var lastBackup: Long? = null,
    var syncEnabled: Boolean = true,
    var dataSavingMode: Boolean = false,
    var accessibilityHighContrast: Boolean = false,
    var accessibilityLargeText: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
)