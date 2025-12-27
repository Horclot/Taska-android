package com.horclotapp.taska

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class FocusFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String

    // UI элементы
    private lateinit var loadingProgress: ProgressBar
    private lateinit var todayTasksContainer: LinearLayout
    private lateinit var tomorrowTasksContainer: LinearLayout
    private lateinit var dailyTasksContainer: LinearLayout
    private lateinit var fabAddTask: com.google.android.material.floatingactionbutton.FloatingActionButton

    private lateinit var addTodayTaskBtn: MaterialButton
    private lateinit var addTomorrowTaskBtn: MaterialButton
    private lateinit var addDailyTaskBtn: MaterialButton

    // Слушатель Firestore
    private var tasksListener: ListenerRegistration? = null

    companion object {
        private val daysOfWeek = listOf(
            "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
        )
        private val shortDays = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_focus, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        // Инициализация UI
        initViews(view)
        setupClickListeners()
        updateDateLabels()

        // Загружаем задачи
        loadTasks()
    }

    private fun initViews(view: View) {
        loadingProgress = view.findViewById(R.id.loadingProgress)
        todayTasksContainer = view.findViewById(R.id.todayTasksContainer)
        tomorrowTasksContainer = view.findViewById(R.id.tomorrowTasksContainer)
        dailyTasksContainer = view.findViewById(R.id.dailyTasksContainer)
        fabAddTask = view.findViewById(R.id.fabAddTask)

        addTodayTaskBtn = view.findViewById(R.id.addTodayTaskBtn)
        addTomorrowTaskBtn = view.findViewById(R.id.addTomorrowTaskBtn)
        addDailyTaskBtn = view.findViewById(R.id.addDailyTaskBtn)

        // Обновляем даты
        val todayDate = view.findViewById<TextView>(R.id.todayDate)
        val tomorrowDate = view.findViewById<TextView>(R.id.tomorrowDate)

        val dateFormat = SimpleDateFormat("d MMMM, EEEE", Locale("ru"))
        val calendar = Calendar.getInstance()

        todayDate.text = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        tomorrowDate.text = dateFormat.format(calendar.time)
    }

    private fun setupClickListeners() {
        addTodayTaskBtn.setOnClickListener { showAddTaskDialog("today") }
        addTomorrowTaskBtn.setOnClickListener { showAddTaskDialog("tomorrow") }
        addDailyTaskBtn.setOnClickListener { showAddTaskDialog("daily") }
        fabAddTask.setOnClickListener { showAddTaskDialog("any") }
    }

    private fun updateDateLabels() {
        // Можно добавить анимацию обновления дат
    }

    private fun loadTasks() {
        if (currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        tasksListener = db.collection("tasks")
            .whereEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                showLoading(false)

                if (error != null) {
                    Toast.makeText(requireContext(), "Ошибка загрузки задач: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val tasks = mutableListOf<Task>()
                snapshot?.documents?.forEach { document ->
                    val task = document.toObject(Task::class.java)
                    if (task != null) {
                        // Создаем копию задачи с ID документа
                        val taskWithId = task.copy(id = document.id)
                        tasks.add(taskWithId)
                    }
                }

                updateTaskViews(tasks)
            }
    }

    private fun updateTaskViews(tasks: List<Task>) {
        // Очищаем контейнеры
        todayTasksContainer.removeAllViews()
        tomorrowTasksContainer.removeAllViews()
        dailyTasksContainer.removeAllViews()

        // Получаем названия дней
        val calendar = Calendar.getInstance()
        val todayDayName = getDayName(calendar)

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowDayName = getDayName(calendar)

        // Фильтруем задачи
        val todayTasks = tasks.filter { it.dayOfWeek == todayDayName && !it.isRecurring }
        val tomorrowTasks = tasks.filter { it.dayOfWeek == tomorrowDayName && !it.isRecurring }
        val dailyTasks = tasks.filter { it.isRecurring }

        // Добавляем задачи в UI
        addTasksToView(todayTasks, todayTasksContainer)
        addTasksToView(tomorrowTasks, tomorrowTasksContainer)
        addTasksToView(dailyTasks, dailyTasksContainer)

        // Показываем сообщения если нет задач
        if (todayTasks.isEmpty()) {
            addEmptyMessage(todayTasksContainer, "Нет задач на сегодня")
        }
        if (tomorrowTasks.isEmpty()) {
            addEmptyMessage(tomorrowTasksContainer, "Нет задач на завтра")
        }
        if (dailyTasks.isEmpty()) {
            addEmptyMessage(dailyTasksContainer, "Нет ежедневных задач")
        }
    }

    private fun addTasksToView(tasks: List<Task>, container: LinearLayout) {
        for ((index, task) in tasks.withIndex()) {
            val taskView = createTaskItemView(task)
            container.addView(taskView)

            // Добавляем разделитель если не последняя задача
            if (index < tasks.size - 1) {
                val divider = View(requireContext())
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                divider.setBackgroundColor(resources.getColor(android.R.color.white))
                divider.alpha = 0.1f
                container.addView(divider)
            }
        }
    }

    private fun createTaskItemView(task: Task): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_task, null)

        val title = view.findViewById<TextView>(R.id.taskTitle)
        val description = view.findViewById<TextView>(R.id.taskDescription)
        val checkbox = view.findViewById<CheckBox>(R.id.taskCheckbox)
        val dayLabel = view.findViewById<TextView>(R.id.taskDay)
        val deleteBtn = view.findViewById<ImageView>(R.id.deleteBtn)

        title.text = task.title
        if (task.description.isNotEmpty()) {
            description.text = task.description
            description.visibility = View.VISIBLE
        }

        checkbox.isChecked = task.isCompleted

        // Получаем короткое название дня
        val shortDay = when(task.dayOfWeek) {
            "Понедельник" -> "ПН"
            "Вторник" -> "ВТ"
            "Среда" -> "СР"
            "Четверг" -> "ЧТ"
            "Пятница" -> "ПТ"
            "Суббота" -> "СБ"
            "Воскресенье" -> "ВС"
            else -> task.dayOfWeek.take(2)
        }

        if (task.isRecurring) {
            dayLabel.text = "ЕЖЕ"
        } else {
            dayLabel.text = shortDay
        }

        // Обработчик изменения статуса задачи
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            updateTaskStatus(task.id, isChecked, task)
        }

        // Обработчик удаления
        deleteBtn.setOnClickListener {
            deleteTask(task.id)
        }

        // Обработчик клика для редактирования
        view.setOnClickListener {
            showEditTaskDialog(task)
        }

        return view
    }

    private fun addEmptyMessage(container: LinearLayout, message: String) {
        val textView = TextView(requireContext()).apply {
            text = message
            setTextColor(resources.getColor(android.R.color.white))
            textSize = 14f
            alpha = 0.6f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        container.addView(textView)
    }

    private fun getDayName(calendar: Calendar): String {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return when(dayOfWeek) {
            Calendar.MONDAY -> "Понедельник"
            Calendar.TUESDAY -> "Вторник"
            Calendar.WEDNESDAY -> "Среда"
            Calendar.THURSDAY -> "Четверг"
            Calendar.FRIDAY -> "Пятница"
            Calendar.SATURDAY -> "Суббота"
            Calendar.SUNDAY -> "Воскресенье"
            else -> ""
        }
    }

    private fun showAddTaskDialog(dayType: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_task, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val dayRadioGroup = dialogView.findViewById<RadioGroup>(R.id.dayRadioGroup)
        val dailyCheckbox = dialogView.findViewById<CheckBox>(R.id.dailyCheckbox)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)
        val saveBtn = dialogView.findViewById<Button>(R.id.saveBtn)

        // Добавляем радио-кнопки для дней недели
        for (i in daysOfWeek.indices) {
            val radioButton = RadioButton(requireContext()).apply {
                text = daysOfWeek[i]
                setTextColor(resources.getColor(android.R.color.white))
                id = i
            }
            dayRadioGroup.addView(radioButton)
        }

        // Устанавливаем значение по умолчанию
        val calendar = Calendar.getInstance()
        when(dayType) {
            "today" -> {
                val todayIndex = calendar.get(Calendar.DAY_OF_WEEK)
                val adjustedIndex = if (todayIndex == 1) 6 else todayIndex - 2
                dayRadioGroup.check(adjustedIndex)
            }
            "tomorrow" -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrowIndex = calendar.get(Calendar.DAY_OF_WEEK)
                val adjustedIndex = if (tomorrowIndex == 1) 6 else tomorrowIndex - 2
                dayRadioGroup.check(adjustedIndex)
            }
            "daily" -> {
                dailyCheckbox.isChecked = true
                dayRadioGroup.check(0)
            }
            else -> {
                // По умолчанию выбираем сегодня
                val todayIndex = calendar.get(Calendar.DAY_OF_WEEK)
                val adjustedIndex = if (todayIndex == 1) 6 else todayIndex - 2
                dayRadioGroup.check(adjustedIndex)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cancelBtn.setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Введите название задачи", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val description = descriptionInput.text.toString().trim()
            val isDaily = dailyCheckbox.isChecked
            val selectedDay = if (!isDaily) {
                val checkedId = dayRadioGroup.checkedRadioButtonId
                if (checkedId == -1) {
                    Toast.makeText(requireContext(), "Выберите день", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                daysOfWeek[checkedId]
            } else {
                "Ежедневно"
            }

            saveTask(title, description, selectedDay, isDaily)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveTask(title: String, description: String, day: String, isRecurring: Boolean) {
        if (currentUserId.isEmpty()) return

        val task = hashMapOf(
            "title" to title,
            "description" to description,
            "dayOfWeek" to day,
            "isRecurring" to isRecurring,
            "isCompleted" to false,
            "userId" to currentUserId,
            "createdAt" to System.currentTimeMillis(),
            "priority" to 1
        )

        db.collection("tasks")
            .add(task)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Задача добавлена", Toast.LENGTH_SHORT).show()
                // Обновляем статистику пользователя
                updateUserTaskStats(incrementCreated = true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTaskStatus(taskId: String, isCompleted: Boolean, task: Task) {
        db.collection("tasks")
            .document(taskId)
            .update("isCompleted", isCompleted)
            .addOnSuccessListener {
                if (isCompleted && !task.isCompleted) {
                    // Задача была только что выполнена
                    updateUserTaskStats(incrementCompleted = true)
                    checkAchievements()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteTask(taskId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить задачу")
            .setMessage("Вы уверены, что хотите удалить эту задачу?")
            .setPositiveButton("Удалить") { dialog, _ ->
                db.collection("tasks")
                    .document(taskId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Задача удалена", Toast.LENGTH_SHORT).show()
                        updateUserTaskStats(incrementCreated = false, decrementTotal = true)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateUserTaskStats(
        incrementCompleted: Boolean = false,
        incrementCreated: Boolean = false,
        decrementTotal: Boolean = false
    ) {
        val updates = hashMapOf<String, Any>()

        if (incrementCompleted) {
            updates["totalTasksCompleted"] = com.google.firebase.firestore.FieldValue.increment(1)
        }

        if (incrementCreated) {
            updates["totalTasksCreated"] = com.google.firebase.firestore.FieldValue.increment(1)
        }

        if (decrementTotal) {
            updates["totalTasksCreated"] = com.google.firebase.firestore.FieldValue.increment(-1)
        }

        if (updates.isNotEmpty()) {
            db.collection("users")
                .document(currentUserId)
                .update(updates)
                .addOnFailureListener { e ->
                    println("Ошибка обновления статистики задач: ${e.message}")
                }
        }
    }

    private fun checkAchievements() {
        // Проверяем достижения на основе статистики пользователя
        db.collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        val newAchievements = mutableListOf<String>()

                        // Проверка достижений
                        if (user.totalTasksCompleted >= 10 && !user.achievements.contains("first_10_tasks")) {
                            newAchievements.add("first_10_tasks")
                        }

                        if (user.totalTasksCompleted >= 50 && !user.achievements.contains("task_master")) {
                            newAchievements.add("task_master")
                        }

                        if (user.currentStreak >= 7 && !user.achievements.contains("weekly_streak")) {
                            newAchievements.add("weekly_streak")
                        }

                        if (newAchievements.isNotEmpty()) {
                            // Добавляем новые достижения
                            val allAchievements = user.achievements + newAchievements
                            db.collection("users")
                                .document(currentUserId)
                                .update("achievements", allAchievements)
                        }
                    }
                }
            }
    }

    private fun showEditTaskDialog(task: Task) {
        // TODO: Реализовать редактирование задачи
        Toast.makeText(requireContext(), "Редактирование задачи: ${task.title}", Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(show: Boolean) {
        loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.remove()
    }
}