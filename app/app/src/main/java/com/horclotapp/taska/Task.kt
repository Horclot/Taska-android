package com.horclotapp.taska

data class Task(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dayOfWeek: String = "",
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val createdAt: Long = 0L,
    val userId: String = "",
    val priority: Int = 1
) {
    // Копирование с новым ID
    fun copy(id: String = this.id): Task {
        return Task(id, title, description, dayOfWeek, isCompleted, isRecurring, createdAt, userId, priority)
    }
}