package com.horclotapp.taska

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Инициализация Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

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
            .requestIdToken(getString(R.string.default_web_client_id)) // Используйте ваш client_id
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun checkCurrentUser() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Пользователь уже вошел, переходим на MainActivity
            navigateToMainActivity()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
        showLoading(true)
    }

    // Регистрируем лаунчер для получения результата
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                // Успешный вход через Google
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                // Ошибка входа через Google
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
                    // Успешный вход в Firebase
                    showError("") // Очищаем ошибку
                    navigateToMainActivity()
                } else {
                    // Ошибка входа в Firebase
                    val errorMessage = task.exception?.message ?: "Неизвестная ошибка"
                    showError("Ошибка аутентификации: $errorMessage")

                    // Показываем Toast с деталями ошибки
                    Toast.makeText(
                        this,
                        "Ошибка: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }
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