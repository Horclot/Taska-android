package com.horclotapp.taska

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.horclotapp.taska.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
}
