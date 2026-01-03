package com.horclotapp.taska

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Task(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var dayOfWeek: String = "",
    var date: String = "",
    var time: String = "00:00",
    var isRecurring: Boolean = false,
    var isCompleted: Boolean = false,
    var userId: String = "",
    var createdAt: Long = 0L,
    var priority: Int = 1,
    var icon: String = "default" // Новая: иконка для задачи
) {
    // Конструктор копирования с ID
    fun copyWithId(id: String): Task {
        return this.copy(id = id)
    }
}