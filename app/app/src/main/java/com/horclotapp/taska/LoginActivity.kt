package com.horclotapp.taska

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Инициализация Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Инициализация UI элементов
        googleSignInButton = findViewById(R.id.googleSignInButton)
        progressBar = findViewById(R.id.progressBar)
        errorTextView = findViewById(R.id.errorTextView)

        // Настройка Google Sign-In
        setupGoogleSignIn()

        // Обработчик нажатия на кнопку Google
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        // Проверяем, не вошел ли пользователь уже
        checkCurrentUser()
    }

    private fun setupGoogleSignIn() {
        // Настройка запроса на вход через Google
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun checkCurrentUser() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Обновляем статистику входа
            updateUserLoginStats(currentUser.uid)
            // Пользователь уже вошел, переходим на MainActivity
            navigateToMainActivity()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
        showLoading(true)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                showError("Ошибка входа через Google: ${e.statusCode}")
                showLoading(false)
            }
        } else {
            showError("Вход отменен")
            showLoading(false)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        // Создаем или обновляем пользователя в Firestore
                        createOrUpdateUser(user, account)
                        showError("")
                        navigateToMainActivity()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Неизвестная ошибка"
                    showError("Ошибка аутентификации: $errorMessage")
                    Toast.makeText(this, "Ошибка: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun createOrUpdateUser(firebaseUser: com.google.firebase.auth.FirebaseUser, googleAccount: GoogleSignInAccount) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val userData = hashMapOf<String, Any>(
            "userId" to firebaseUser.uid,
            "email" to (firebaseUser.email ?: ""),
            "displayName" to (firebaseUser.displayName ?: googleAccount.displayName ?: "Пользователь"),
            "photoUrl" to (firebaseUser.photoUrl?.toString() ?: googleAccount.photoUrl?.toString() ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "lastLogin" to System.currentTimeMillis(),
            "lastLoginDate" to todayDate,
            "totalTasksCompleted" to 0,
            "totalTasksCreated" to 0,
            "currentStreak" to 1, // Первый день
            "bestStreak" to 1,
            "totalFocusTime" to 0L,
            "totalFocusSessions" to 0,
            "level" to 1,
            "experience" to 0,
            "achievements" to emptyList<String>()
        )

        val premiumData = hashMapOf<String, Any>(
            "userId" to firebaseUser.uid,
            "isPremium" to false,
            "subscriptionType" to "free",
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        // Создаем настройки пользователя
        val userSettings = hashMapOf<String, Any>(
            "userId" to firebaseUser.uid,
            "notificationsEnabled" to true,
            "darkMode" to false,
            "followSystemTheme" to true,
            "language" to "ru",
            "soundEffects" to true,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        // Записываем в Firestore
        firestore.collection("premium_subscriptions")
            .document(firebaseUser.uid)
            .set(premiumData, SetOptions.merge())
            .addOnSuccessListener {
                // Premium данные сохранены
            }

        firestore.collection("user_settings")
            .document(firebaseUser.uid)
            .set(userSettings, SetOptions.merge())
            .addOnSuccessListener {
                // Настройки сохранены
            }

        // Используем merge, чтобы не перезаписывать существующие данные
        firestore.collection("users")
            .document(firebaseUser.uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                // Сохраняем настройки приложения локально
                saveUserSettingsLocally(firebaseUser.uid)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка сохранения данных: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserLoginStats(userId: String) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        val newStreak = calculateStreak(user.lastLoginDate, todayDate, user.currentStreak)

                        val updates = hashMapOf<String, Any>(
                            "lastLogin" to System.currentTimeMillis(),
                            "lastLoginDate" to todayDate,
                            "currentStreak" to newStreak
                        )

                        if (newStreak > user.bestStreak) {
                            updates["bestStreak"] = newStreak
                        }

                        firestore.collection("users")
                            .document(userId)
                            .update(updates as Map<String, Any>)
                    }
                }
            }
            .addOnFailureListener { e ->
                // Логируем ошибку, но не показываем пользователю
                println("Ошибка обновления статистики входа: ${e.message}")
            }
    }

    private fun calculateStreak(lastLoginDate: String, todayDate: String, currentStreak: Int): Int {
        if (lastLoginDate.isEmpty() || lastLoginDate == todayDate) {
            return currentStreak // Уже заходил сегодня
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val lastDate = sdf.parse(lastLoginDate)
            val today = sdf.parse(todayDate)

            val diff = (today.time - lastDate.time) / (1000 * 60 * 60 * 24)

            return if (diff == 1L) {
                // Вчерашний день - увеличиваем streak
                currentStreak + 1
            } else {
                // Пропустил день - сбрасываем до 1
                1
            }
        } catch (e: Exception) {
            return 1
        }
    }

    private fun saveUserSettingsLocally(userId: String) {
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putString("user_id", userId)
            putLong("last_login_date", System.currentTimeMillis())
            apply()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        googleSignInButton.isEnabled = !show
    }

    private fun showError(message: String) {
        if (message.isBlank()) {
            errorTextView.visibility = android.view.View.GONE
        } else {
            errorTextView.text = message
            errorTextView.visibility = android.view.View.VISIBLE
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}