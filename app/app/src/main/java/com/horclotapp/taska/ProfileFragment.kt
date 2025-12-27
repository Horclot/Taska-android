package com.horclotapp.taska

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.horclotapp.taska.databinding.FragmentProfileBinding
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences

    private var isAnimating = false
    private val floatingParticles = mutableListOf<View>()
    private val particleAnimators = mutableListOf<Animator>()
    private var userListener: ListenerRegistration? = null

    // Конфигурация частиц
    private data class ParticleConfig(
        val size: Int, // dp
        val alpha: Float,
        val iconRes: Int,
        val minDuration: Long,
        val maxDuration: Long,
        val minDistance: Float,
        val maxDistance: Float
    )

    private val particleConfigs = listOf(
        ParticleConfig(12, 0.3f, R.drawable.ic_star_border_black_24dp, 3000L, 6000L, 50f, 200f),
        ParticleConfig(8, 0.4f, R.drawable.ic_star_border_black_24dp, 4000L, 7000L, 30f, 150f),
        ParticleConfig(16, 0.2f, R.drawable.ic_star_border_black_24dp, 5000L, 8000L, 80f, 250f)
    )

    // Достижения
    private val achievementsMap = mapOf(
        "first_10_tasks" to Achievement(
            name = "Первые 10 задач",
            description = "Выполните 10 задач",
            icon = "🔥",
            type = AchievementType.BASIC
        ),
        "task_master" to Achievement(
            name = "Мастер задач",
            description = "Выполните 100 задач",
            icon = "⚡",
            type = AchievementType.SPECIAL
        ),
        "weekly_streak" to Achievement(
            name = "Недельная серия",
            description = "Входите в приложение 7 дней подряд",
            icon = "🏆",
            type = AchievementType.BASIC
        ),
        "first_focus" to Achievement(
            name = "Первая фокусировка",
            description = "Завершите первую сессию фокуса",
            icon = "🎯",
            type = AchievementType.BASIC
        ),
        "pro_planner" to Achievement(
            name = "Про планировщик",
            description = "Создайте 50 задач",
            icon = "📝",
            type = AchievementType.SPECIAL
        ),
        "monthly_champion" to Achievement(
            name = "Месячный чемпион",
            description = "Входите в приложение 30 дней подряд",
            icon = "👑",
            type = AchievementType.SPECIAL
        ),
        "focus_master" to Achievement(
            name = "Мастер фокуса",
            description = "Накопите 10 часов фокуса",
            icon = "🧠",
            type = AchievementType.SPECIAL
        ),
        "early_bird" to Achievement(
            name = "Жаворонок",
            description = "Выполните задачу до 8 утра",
            icon = "🐦",
            type = AchievementType.HIDDEN
        )
    )

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

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        sharedPreferences = requireContext().getSharedPreferences("user_prefs", 0)

        setupParticles()
        setupAnimations()
        setupClickListeners()

        // Загружаем данные пользователя
        loadUserData()

        startBackgroundAnimations()
        startParticleAnimations()
    }

    private fun setupParticles() {
        val particlesContainer = binding.particlesContainer
        val random = Random(System.currentTimeMillis())

        // Создаем 15-20 частиц
        for (i in 0 until 18) {
            val config = particleConfigs.random()

            val particle = android.widget.ImageView(requireContext()).apply {
                layoutParams = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                    dpToPx(config.size),
                    dpToPx(config.size)
                )
                setImageResource(config.iconRes)
                alpha = config.alpha
                id = View.generateViewId()

                // Случайное позиционирование
                val xPercentage = random.nextFloat()
                val yPercentage = random.nextFloat()

                val params = layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                params.leftToLeft = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.rightToRight = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID

                // Устанавливаем горизонтальное смещение
                params.horizontalBias = xPercentage
                params.verticalBias = yPercentage

                // Небольшое случайное смещение от точной позиции
                translationX = random.nextInt(-20, 20).toFloat()
                translationY = random.nextInt(-20, 20).toFloat()
            }

            particlesContainer.addView(particle)
            floatingParticles.add(particle)
        }
    }

    private fun setupClickListeners() {
        binding.logoutButton.setOnClickListener {
            animateButtonPress(binding.logoutButton)
            showLogoutConfirmation()
        }

        binding.profileAvatar.setOnClickListener {
            animateAvatarClick()
        }

        // Находим элементы настроек по ID
        val settingsNotifications = binding.settingsList.findViewById<LinearLayout>(R.id.settings_notifications)
        val settingsTheme = binding.settingsList.findViewById<LinearLayout>(R.id.settings_theme)
        val settingsLanguage = binding.settingsList.findViewById<LinearLayout>(R.id.settings_language)
        val settingsPrivacy = binding.settingsList.findViewById<LinearLayout>(R.id.settings_privacy)
        val notificationsSwitch = binding.settingsList.findViewById<Switch>(R.id.notifications_switch)

        settingsNotifications.setOnClickListener {
            animateButtonPress(settingsNotifications)
            notificationsSwitch.isChecked = !notificationsSwitch.isChecked
        }

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveSettingLocally("notifications_enabled", isChecked)
            updateUserSettingInFirestore("notificationsEnabled", isChecked)
        }

        settingsTheme.setOnClickListener {
            animateButtonPress(settingsTheme)
            toggleDarkMode()
        }

        settingsLanguage.setOnClickListener {
            animateButtonPress(settingsLanguage)
            showLanguageSelectionDialog()
        }

        settingsPrivacy.setOnClickListener {
            animateButtonPress(settingsPrivacy)
            showPrivacyDialog()
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти из аккаунта?")
            .setPositiveButton("Выйти") { dialog, _ ->
                logoutUser()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun toggleDarkMode() {
        val isDarkMode = sharedPreferences.getBoolean("dark_mode", false)
        val newDarkMode = !isDarkMode

        saveSettingLocally("dark_mode", newDarkMode)
        updateUserSettingInFirestore("darkMode", newDarkMode)

        // Показываем уведомление
        Toast.makeText(
            requireContext(),
            if (newDarkMode) "Темная тема включена" else "Светлая тема включена",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("Русский", "English", "Español", "Deutsch")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Выберите язык")
            .setItems(languages) { dialog, which ->
                val selectedLanguage = when (which) {
                    0 -> "ru"
                    1 -> "en"
                    2 -> "es"
                    3 -> "de"
                    else -> "ru"
                }

                saveSettingLocally("language", selectedLanguage)
                updateUserSettingInFirestore("language", selectedLanguage)

                Toast.makeText(
                    requireContext(),
                    "Язык изменен. Перезапустите приложение",
                    Toast.LENGTH_SHORT
                ).show()

                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showPrivacyDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Конфиденциальность")
            .setMessage(
                """
                Настройки конфиденциальности:
                
                1. Ваши данные синхронизируются с сервером
                2. Вы можете удалить свои данные в любое время
                3. Мы не передаем ваши данные третьим лицам
                
                Для управления данными перейдите в настройки вашего аккаунта.
                """.trimIndent()
            )
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showLoginRequired()
            return
        }

        showLoading(true)

        // Удаляем старый слушатель
        userListener?.remove()

        // Устанавливаем слушатель для реального обновления данных
        userListener = firestore.collection("users")
            .document(currentUser.uid)
            .addSnapshotListener { document, error ->
                showLoading(false)

                if (error != null) {
                    Log.e("ProfileFragment", "Error listening to user data: ${error.message}", error)
                    updateUIWithFirebaseUser(currentUser)
                    loadDefaultSettings()
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val user = document.toObject(com.horclotapp.taska.User::class.java)
                    user?.let {
                        Log.d("ProfileFragment", "User data updated: ${it.displayName}, completed tasks: ${it.totalTasksCompleted}")
                        updateUIWithUserData(it, currentUser)
                        loadUserSettings(it.settings)
                        updateAchievementsUI(it.achievements)
                    }
                } else {
                    // Если пользователь не найден в Firestore, создаем запись
                    Log.d("ProfileFragment", "Creating new user document")
                    createUserDocument(currentUser)
                    updateUIWithFirebaseUser(currentUser)
                    loadDefaultSettings()
                }
            }
    }

    private fun createUserDocument(user: com.google.firebase.auth.FirebaseUser) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val userData = hashMapOf<String, Any>(
            "userId" to user.uid,
            "email" to (user.email ?: ""),
            "displayName" to (user.displayName ?: "Пользователь"),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "lastLogin" to System.currentTimeMillis(),
            "lastLoginDate" to todayDate,
            "totalTasksCompleted" to 0,
            "totalTasksCreated" to 0,
            "currentStreak" to 1,
            "bestStreak" to 1,
            "totalFocusTime" to 0L,
            "totalFocusSessions" to 0,
            "level" to 1,
            "experience" to 0,
            "achievements" to emptyList<String>(),
            "settings" to hashMapOf(
                "notificationsEnabled" to true,
                "darkMode" to false,
                "language" to "ru",
                "soundEffects" to true
            )
        )

        firestore.collection("users")
            .document(user.uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("ProfileFragment", "User document created successfully")
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error creating user document: ${e.message}", e)
                Toast.makeText(requireContext(), "Ошибка создания пользователя: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUIWithUserData(user: com.horclotapp.taska.User, firebaseUser: com.google.firebase.auth.FirebaseUser) {
        // Имя пользователя
        binding.userName.text = user.displayName.ifEmpty {
            firebaseUser.displayName ?: "Пользователь"
        }

        // Email
        binding.userEmail.text = user.email.ifEmpty {
            firebaseUser.email ?: ""
        }

        // Аватар
        val photoUrl = user.photoUrl.ifEmpty {
            firebaseUser.photoUrl?.toString() ?: ""
        }

        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_profile_large)
                .into(binding.profileAvatar)
        }

        // Статистика
        startStatsCounterAnimations(
            completedTasks = user.totalTasksCompleted,
            currentStreak = user.currentStreak,
            focusTime = (user.totalFocusTime / 60).toInt(), // Конвертируем минуты в часы
            tasksCreated = user.totalTasksCreated,
            bestStreak = user.bestStreak,
            focusSessions = user.totalFocusSessions
        )

        // Уровень и прогресс
        binding.levelBadge.text = user.level.toString()
        calculateAndSetProgress(user.experience)

        // Дополнительная статистика (показываем сразу)
        binding.completedTasks.text = user.totalTasksCompleted.toString()
        binding.currentStreak.text = user.currentStreak.toString()
        binding.focusTime.text = (user.totalFocusTime / 60).toString()
    }

    private fun updateAchievementsUI(achievements: List<String>) {
        // Получаем контейнер с достижениями
        val achievementsScroll = binding.achievementsScroll
        val achievementsContainer = achievementsScroll.getChildAt(0) as LinearLayout

        // Убираем все старые достижения
        achievementsContainer.removeAllViews()

        // Показываем только разблокированные достижения
        val unlockedAchievements = achievements.mapNotNull { achievementId ->
            achievementsMap[achievementId]
        }

        // Добавляем разблокированные достижения
        unlockedAchievements.forEachIndexed { index, achievement ->
            val cardView = androidx.cardview.widget.CardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(80),
                    dpToPx(100)
                ).apply {
                    marginEnd = if (index < unlockedAchievements.size - 1) dpToPx(16) else 0
                }
                radius = dpToPx(16).toFloat()
                elevation = dpToPx(4).toFloat()
                setCardBackgroundColor(resources.getColor(android.R.color.white))
                alpha = 1.0f // Полностью видимые
            }

            val innerLayout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }

            val iconTextView = android.widget.TextView(requireContext()).apply {
                text = achievement.icon
                textSize = 24f
                gravity = android.view.Gravity.CENTER
            }

            val nameTextView = android.widget.TextView(requireContext()).apply {
                text = achievement.name
                setTextColor(resources.getColor(android.R.color.black))
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                maxLines = 2
                setPadding(0, dpToPx(8), 0, 0)
            }

            innerLayout.addView(iconTextView)
            innerLayout.addView(nameTextView)
            cardView.addView(innerLayout)
            achievementsContainer.addView(cardView)
        }

        // Если нет достижений, показываем сообщение
        if (unlockedAchievements.isEmpty()) {
            val message = android.widget.TextView(requireContext()).apply {
                text = "Выполняйте задачи для получения достижений!"
                setTextColor(resources.getColor(android.R.color.white))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                alpha = 0.7f
                setPadding(0, dpToPx(20), 0, dpToPx(20))
            }
            achievementsContainer.addView(message)
        }
    }

    private fun updateUIWithFirebaseUser(user: com.google.firebase.auth.FirebaseUser) {
        binding.userName.text = user.displayName ?: "Пользователь"
        binding.userEmail.text = user.email ?: ""

        user.photoUrl?.let { photoUrl ->
            Glide.with(this)
                .load(photoUrl)
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.ic_profile_large)
                .into(binding.profileAvatar)
        }

        // Значения по умолчанию
        startStatsCounterAnimations(0, 0, 0, 0, 0, 0)
        binding.levelBadge.text = "1"
        binding.levelProgress.progress = 0
    }

    private fun loadUserSettings(settings: com.horclotapp.taska.UserSettings) {
        val notificationsSwitch = binding.settingsList.findViewById<Switch>(R.id.notifications_switch)
        notificationsSwitch.isChecked = settings.notificationsEnabled

        saveSettingLocally("notifications_enabled", settings.notificationsEnabled)
        saveSettingLocally("dark_mode", settings.darkMode)
        saveSettingLocally("language", settings.language)
        saveSettingLocally("sound_effects", settings.soundEffects)
    }

    private fun loadDefaultSettings() {
        val notificationsSwitch = binding.settingsList.findViewById<Switch>(R.id.notifications_switch)
        notificationsSwitch.isChecked = true
    }

    private fun calculateAndSetProgress(experience: Int) {
        val levelThreshold = 1000
        val progress = if (experience > 0) {
            ((experience % levelThreshold) * 100) / levelThreshold
        } else {
            0
        }
        binding.levelProgress.progress = progress
    }

    private fun startStatsCounterAnimations(
        completedTasks: Int = 0,
        currentStreak: Int = 0,
        focusTime: Int = 0,
        tasksCreated: Int = 0,
        bestStreak: Int = 0,
        focusSessions: Int = 0
    ) {
        // Проверяем текущие значения
        val currentCompletedTasks = binding.completedTasks.text.toString().toIntOrNull() ?: 0
        val currentStreakValue = binding.currentStreak.text.toString().toIntOrNull() ?: 0
        val currentFocusTime = binding.focusTime.text.toString().toIntOrNull() ?: 0

        // Анимация только если значения изменились
        if (completedTasks != currentCompletedTasks) {
            animateCounter(binding.completedTasks, currentCompletedTasks, completedTasks, 1500L)
        } else {
            binding.completedTasks.text = completedTasks.toString()
        }

        if (currentStreak != currentStreakValue) {
            animateCounter(binding.currentStreak, currentStreakValue, currentStreak, 1500L)
        } else {
            binding.currentStreak.text = currentStreak.toString()
        }

        if (focusTime != currentFocusTime) {
            animateCounter(binding.focusTime, currentFocusTime, focusTime, 1500L)
        } else {
            binding.focusTime.text = focusTime.toString()
        }
    }

    private fun animateCounter(textView: android.widget.TextView, start: Int, end: Int, duration: Long) {
        val animator = ValueAnimator.ofInt(start, end)
        animator.duration = duration
        animator.interpolator = OvershootInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            textView.text = value.toString()
        }
        animator.start()
    }

    private fun saveSettingLocally(key: String, value: Any) {
        with(sharedPreferences.edit()) {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
            }
            apply()
        }
    }

    private fun updateUserSettingInFirestore(settingKey: String, value: Any) {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(userId)
            .update("settings.$settingKey", value)
            .addOnFailureListener {
                Log.e("ProfileFragment", "Error updating user setting: $settingKey")
            }
    }

    private fun showLoginRequired() {
        binding.userName.text = "Требуется вход"
        binding.userEmail.text = "Войдите в аккаунт"
        binding.logoutButton.text = "Войти"

        binding.logoutButton.setOnClickListener {
            requireActivity().finish()
            val intent = android.content.Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun logoutUser() {
        auth.signOut()

        // Очищаем локальные настройки
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }

        // Переходим на экран входа
        requireActivity().finish()
        val intent = android.content.Intent(requireContext(), LoginActivity::class.java)
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        binding.loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.profileCard.alpha = if (show) 0.5f else 1f
    }

    private fun setupAnimations() {
        animateEntrance()
    }

    private fun animateEntrance() {
        val animatorSet = AnimatorSet()

        val cardAnimator = ObjectAnimator.ofFloat(binding.profileCard, "translationY", 100f, 0f)
        cardAnimator.duration = 800
        cardAnimator.interpolator = OvershootInterpolator()

        val alphaAnimator = ObjectAnimator.ofFloat(binding.profileCard, "alpha", 0f, 1f)
        alphaAnimator.duration = 600

        animatorSet.playTogether(cardAnimator, alphaAnimator)
        animatorSet.start()
    }

    private fun startBackgroundAnimations() {
        val circle1Animator = createPulseAnimation(binding.animatedBgCircle1, 15000L)
        val circle2Animator = createPulseAnimation(binding.animatedBgCircle2, 18000L)

        circle1Animator.start()
        circle2Animator.start()
    }

    private fun createPulseAnimation(view: View, duration: Long): ValueAnimator {
        return ValueAnimator.ofFloat(0.1f, 0.25f, 0.1f).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                view.alpha = value
                view.scaleX = 1 + (value * 0.1f)
                view.scaleY = 1 + (value * 0.1f)
            }
        }
    }

    private fun startParticleAnimations() {
        val random = Random(System.currentTimeMillis())

        floatingParticles.forEachIndexed { index, particle ->
            val config = particleConfigs[index % particleConfigs.size]

            // Случайные параметры для каждой частицы
            val duration = random.nextLong(config.minDuration, config.maxDuration)
            val distance = random.nextFloat() * (config.maxDistance - config.minDistance) + config.minDistance

            // Анимация плавания
            val floatAnimator = ObjectAnimator.ofFloat(
                particle,
                "translationY",
                0f,
                -distance,
                0f,
                distance * 0.5f,
                0f
            )
            floatAnimator.duration = duration
            floatAnimator.repeatCount = ValueAnimator.INFINITE
            floatAnimator.repeatMode = ValueAnimator.REVERSE
            floatAnimator.interpolator = AccelerateDecelerateInterpolator()
            floatAnimator.startDelay = index * 100L

            // Анимация вращения
            val rotationDuration = duration * 2
            val rotateAnimator = ObjectAnimator.ofFloat(
                particle,
                "rotation",
                0f,
                if (random.nextBoolean()) 360f else -360f
            )
            rotateAnimator.duration = rotationDuration
            rotateAnimator.repeatCount = ValueAnimator.INFINITE

            // Анимация альфа-канала (мерцание)
            val alphaAnimator = ValueAnimator.ofFloat(config.alpha * 0.5f, config.alpha, config.alpha * 0.5f)
            alphaAnimator.duration = duration + 2000L
            alphaAnimator.repeatCount = ValueAnimator.INFINITE
            alphaAnimator.repeatMode = ValueAnimator.REVERSE
            alphaAnimator.addUpdateListener { animation ->
                particle.alpha = animation.animatedValue as Float
            }
            alphaAnimator.startDelay = index * 150L

            // Горизонтальное движение
            val horizontalDistance = random.nextFloat() * 30f - 15f
            val horizontalAnimator = ObjectAnimator.ofFloat(
                particle,
                "translationX",
                0f,
                horizontalDistance,
                0f,
                -horizontalDistance,
                0f
            )
            horizontalAnimator.duration = duration + 3000L
            horizontalAnimator.repeatCount = ValueAnimator.INFINITE
            horizontalAnimator.repeatMode = ValueAnimator.REVERSE

            // Собираем все анимации для частицы
            val particleAnimatorSet = AnimatorSet()
            particleAnimatorSet.playTogether(floatAnimator, rotateAnimator, alphaAnimator, horizontalAnimator)
            particleAnimatorSet.start()

            particleAnimators.add(particleAnimatorSet)
        }
    }

    private fun animateButtonPress(button: View) {
        if (isAnimating) return
        isAnimating = true

        val scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 0.95f)
        val scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 0.95f)
        val scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 1f)
        val scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 1f)

        scaleDownX.duration = 100
        scaleDownY.duration = 100
        scaleUpX.duration = 200
        scaleUpY.duration = 200
        scaleUpX.interpolator = BounceInterpolator()
        scaleUpY.interpolator = BounceInterpolator()

        val animatorSet = AnimatorSet()
        animatorSet.play(scaleDownX).with(scaleDownY)
        animatorSet.play(scaleUpX).with(scaleUpY).after(scaleDownX)

        animatorSet.doOnEnd { isAnimating = false }
        animatorSet.start()
    }

    private fun animateAvatarClick() {
        val scaleX = ObjectAnimator.ofFloat(binding.profileAvatar, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.profileAvatar, "scaleY", 1f, 1.1f, 1f)

        scaleX.duration = 500
        scaleY.duration = 500
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()

        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }

        val rotation = ObjectAnimator.ofFloat(binding.levelProgress, "rotation", 0f, 360f)
        rotation.duration = 800
        rotation.interpolator = AccelerateDecelerateInterpolator()
        rotation.start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        startBackgroundAnimations()
        // Перезапускаем анимации частиц
        particleAnimators.forEach { animator ->
            if (animator is AnimatorSet && !animator.isRunning) {
                animator.start()
            }
        }
        // Обновляем данные пользователя
        loadUserData()
    }

    override fun onPause() {
        super.onPause()
        isAnimating = false
        // Останавливаем анимации частиц для экономии батареи
        particleAnimators.forEach { animator ->
            if (animator is AnimatorSet && animator.isRunning) {
                animator.cancel()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем слушатель
        userListener?.remove()
        userListener = null
        // Очищаем все анимации
        particleAnimators.forEach { it.cancel() }
        particleAnimators.clear()
        floatingParticles.clear()
        _binding = null
    }
}