package com.horclotapp.taska

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.fragment.app.Fragment
import com.horclotapp.taska.R
import com.horclotapp.taska.databinding.FragmentProfileBinding
import kotlin.random.Random

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var isAnimating = false

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

        setupAnimations()
        setupClickListeners()

        startBackgroundAnimations()
        startStatsCounterAnimations()

        // Правильная проверка
        _binding?.let { binding ->
            binding.floatingParticle1?.let {
                animateFloatingParticle(it, 4000L, 200f)
            }
            binding.floatingParticle2?.let {
                animateFloatingParticle(it, 5000L, 150f)
            }
        }
    }

    private fun setupClickListeners() {
        binding.logoutButton.setOnClickListener {
            // Анимация нажатия на кнопку выхода
            animateButtonPress(binding.logoutButton)
            // Здесь будет логика выхода
        }

        // Анимация при клике на аватар
        binding.profileAvatar.setOnClickListener {
            animateAvatarClick()
        }
    }

    private fun setupAnimations() {
        // Начальная анимация появления элементов
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
        // Пульсирующая анимация фоновых кругов
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
        // Анимация плавающих частиц
        animateFloatingParticle(binding.floatingParticle1, 4000L, 200f)
        animateFloatingParticle(binding.floatingParticle2, 5000L, 150f)
    }

    private fun animateFloatingParticle(view: View, duration: Long, distance: Float) {
        val floatAnimator = ObjectAnimator.ofFloat(view, "translationY", 0f, -distance, 0f)
        floatAnimator.duration = duration
        floatAnimator.repeatCount = ValueAnimator.INFINITE
        floatAnimator.repeatMode = ValueAnimator.REVERSE
        floatAnimator.interpolator = AccelerateDecelerateInterpolator()

        val rotateAnimator = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
        rotateAnimator.duration = duration * 2
        rotateAnimator.repeatCount = ValueAnimator.INFINITE

        AnimatorSet().apply {
            playTogether(floatAnimator, rotateAnimator)
            start()
        }
    }

    private fun startStatsCounterAnimations() {
        // Анимация счетчиков статистики
        animateCounter(binding.completedTasks, 0, 128, 2000L)
        animateCounter(binding.currentStreak, 0, 7, 1500L)
        animateCounter(binding.focusTime, 0, 42, 2500L)
    }

    private fun animateCounter(textView: android.widget.TextView, start: Int, end: Int, duration: Long) {
        val animator = ValueAnimator.ofInt(start, end)
        animator.duration = duration
        animator.interpolator = OvershootInterpolator()
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            textView.text = if (textView.id == R.id.focusTime) "$value" else value.toString()
        }
        animator.start()
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
        // Анимация пульсации аватара
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

        // Анимация вращения прогресс-бара
        val rotation = ObjectAnimator.ofFloat(binding.levelProgress, "rotation", 0f, 360f)
        rotation.duration = 800
        rotation.interpolator = AccelerateDecelerateInterpolator()
        rotation.start()
    }

    override fun onResume() {
        super.onResume()
        // Возобновляем анимации при возвращении на экран
        startBackgroundAnimations()
    }

    override fun onPause() {
        super.onPause()
        // Останавливаем анимации при уходе с экрана
        isAnimating = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}