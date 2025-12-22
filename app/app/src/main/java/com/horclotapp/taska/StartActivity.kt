package com.horclotapp.taska

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Применяем тему сплеш-скрина
        setTheme(R.style.Theme_Taska_Splash)
        setContentView(R.layout.activity_start)

        // Настройка отступов под системные панели
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Настраиваем обработку кнопки "Назад"
        setupBackPressHandler()

        // Скрываем элементы перед анимацией
        hideElementsBeforeAnimation()

        // Запускаем анимации
        Handler(Looper.getMainLooper()).postDelayed({
            setupAnimations()
        }, 300)

        setupStartButton()
    }

    private fun setupBackPressHandler() {
        // Создаем callback для обработки нажатия кнопки "Назад"
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Показываем диалог выхода или выполняем другую логику
                showExitDialog()
            }
        }

        // Регистрируем callback
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun showExitDialog() {
        // Здесь можно показать диалог подтверждения выхода
        // Пока просто выходим из приложения
        finishAffinity()
    }

    private fun hideElementsBeforeAnimation() {
        val logoContainer = findViewById<ConstraintLayout>(R.id.logoContainer)
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val appName = findViewById<TextView>(R.id.appName)
        val subtitle = findViewById<TextView>(R.id.appSubtitle)
        val startButton = findViewById<MaterialButton>(R.id.startButton)

        logoContainer.alpha = 0f
        logoContainer.scaleX = 0.8f
        logoContainer.scaleY = 0.8f

        logoImage.alpha = 0f
        logoImage.scaleX = 0.5f
        logoImage.scaleY = 0.5f

        appName.alpha = 0f
        appName.translationY = 50f

        subtitle.alpha = 0f
        subtitle.translationY = 50f

        startButton.alpha = 0f
        startButton.translationY = 50f
    }

    private fun setupAnimations() {
        val logoContainer = findViewById<ConstraintLayout>(R.id.logoContainer)
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val appName = findViewById<TextView>(R.id.appName)
        val subtitle = findViewById<TextView>(R.id.appSubtitle)
        val startButton = findViewById<MaterialButton>(R.id.startButton)

        // Анимация логотипа контейнера
        logoContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(600)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .start()

        // Анимация самого логотипа (SVG)
        logoImage.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(600)
            .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
            .start()

        // Анимация названия приложения
        Handler(Looper.getMainLooper()).postDelayed({
            appName.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .start()
        }, 200)

        // Анимация подзаголовка
        Handler(Looper.getMainLooper()).postDelayed({
            subtitle.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .start()
        }, 400)

        // Анимация кнопки
        Handler(Looper.getMainLooper()).postDelayed({
            startButton.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .start()
        }, 600)
    }

    private fun setupStartButton() {
        val startButton = findViewById<MaterialButton>(R.id.startButton)

        startButton.setOnClickListener {
            // Отключаем возможность повторного нажатия
            startButton.isEnabled = false

            // Анимация нажатия
            startButton.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(150)
                .withEndAction {
                    startButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()

                    // Анимация перехода
                    val rootView = findViewById<ConstraintLayout>(R.id.main)
                    rootView.animate()
                        .alpha(0f)
                        .setDuration(400)
                        .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                        .withEndAction {
                            // Переход на MainActivity
                            val intent = Intent(this@StartActivity, MainActivity::class.java)
                            startActivity(intent)
                            // Плавный переход
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            finish()
                        }
                        .start()
                }
                .start()
        }
    }
}