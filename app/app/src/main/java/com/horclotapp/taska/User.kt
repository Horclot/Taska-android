// User.kt
package com.horclotapp.taska

import com.google.firebase.firestore.PropertyName

data class User(
    @PropertyName("userId")
    val userId: String = "",

    @PropertyName("email")
    val email: String = "",

    @PropertyName("displayName")
    val displayName: String = "",

    @PropertyName("photoUrl")
    val photoUrl: String = "",

    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @PropertyName("lastLogin")
    val lastLogin: Long = System.currentTimeMillis(),

    @PropertyName("lastLoginDate") // Для отслеживания дней подряд
    val lastLoginDate: String = "",

    @PropertyName("totalTasksCompleted")
    val totalTasksCompleted: Int = 0,

    @PropertyName("totalTasksCreated")
    val totalTasksCreated: Int = 0,

    @PropertyName("currentStreak")
    val currentStreak: Int = 0,

    @PropertyName("bestStreak")
    val bestStreak: Int = 0,

    @PropertyName("totalFocusTime")
    val totalFocusTime: Long = 0, // в минутах

    @PropertyName("totalFocusSessions")
    val totalFocusSessions: Int = 0,

    @PropertyName("level")
    val level: Int = 1,

    @PropertyName("experience")
    val experience: Int = 0,

    @PropertyName("achievements")
    val achievements: List<String> = emptyList(),

    @PropertyName("settings")
    val settings: UserSettings = UserSettings()
)

data class UserSettings(
    @PropertyName("notificationsEnabled")
    val notificationsEnabled: Boolean = true,

    @PropertyName("darkMode")
    val darkMode: Boolean = false,

    @PropertyName("language")
    val language: String = "ru",

    @PropertyName("soundEffects")
    val soundEffects: Boolean = true
)

// Класс для достижений
data class Achievement(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val unlockedAt: Long = 0,
    val type: AchievementType = AchievementType.BASIC
)

enum class AchievementType {
    BASIC, SPECIAL, HIDDEN
}