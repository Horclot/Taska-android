package com.horclotapp.taska

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.horclotapp.taska.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleInvite(intent)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Устанавливаем ActionBar
        setSupportActionBar(binding.toolbar)

        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Конфигурация для AppBar
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_spaces,
                R.id.navigation_focus,
                R.id.navigation_activity,
                R.id.navigation_profile
            )
        )

        // Настраиваем ActionBar с NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleInvite(intent)
    }
    private fun handleInvite(intent: Intent?) {
        val data = intent?.data ?: return
        val code = data.getQueryParameter("code") ?: return

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return

        db.collection("room_invites")
            .document(code)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                val roomId = doc.getString("roomId") ?: return@addOnSuccessListener

                val member = mapOf(
                    "roomId" to roomId,
                    "userId" to userId,
                    "systemRoles" to listOf("member"),
                    "status" to "pending",
                    "joinedAt" to System.currentTimeMillis()
                )

                db.collection("room_members")
                    .document("${roomId}_$userId")
                    .set(member)
            }
    }
}
