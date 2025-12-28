package com.horclotapp.taska

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dayOfWeek: String = "",
    val isRecurring: Boolean = false,
    val isCompleted: Boolean = false,
    val userId: String = "",
    val createdAt: Long = 0,
    val priority: Int = 1,
    @ServerTimestamp
    val dueDate: Timestamp? = null
) {
    // Конструктор копирования с ID
    fun copyWithId(id: String): Task {
        return this.copy(id = id)
    }
}