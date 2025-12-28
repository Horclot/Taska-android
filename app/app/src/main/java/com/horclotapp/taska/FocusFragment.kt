package com.horclotapp.taska

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class FocusFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String

    // UI элементы
    private lateinit var loadingProgress: ProgressBar
    private lateinit var tasksContainer: LinearLayout
    private lateinit var fabAddTask: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var currentDate: TextView
    private lateinit var todayTasksCount: TextView
    private lateinit var totalTasksCount: TextView
    private lateinit var completedPercent: TextView

    // Слушатель Firestore
    private var tasksListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    // Аниматоры
    private val floatingAnimators = mutableListOf<ValueAnimator>()

    companion object {
        private val daysOfWeek = listOf(
            "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
        )
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
        setupAnimations()
        setupClickListeners()
        updateDateLabel()

        // Загружаем задачи
        loadTasks()
    }

    private fun initViews(view: View) {
        loadingProgress = view.findViewById(R.id.loadingProgress)
        tasksContainer = view.findViewById(R.id.tasksContainer)
        fabAddTask = view.findViewById(R.id.fabAddTask)
        currentDate = view.findViewById(R.id.currentDate)
        todayTasksCount = view.findViewById(R.id.todayTasksCount)
        totalTasksCount = view.findViewById(R.id.totalTasksCount)
        completedPercent = view.findViewById(R.id.completedPercent)
    }

    private fun setupAnimations() {
        // Анимация неоновых эффектов
        startFloatingAnimations()

        // Анимация FAB
        fabAddTask.scaleX = 0f
        fabAddTask.scaleY = 0f
        fabAddTask.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setStartDelay(300)
            .start()
    }

    private fun startFloatingAnimations() {
        // Пульсация неоновых эффектов
        val neonGlow1 = requireView().findViewById<View>(R.id.neonGlow1)
        val neonAnimator1 = ObjectAnimator.ofFloat(neonGlow1, "alpha", 0.15f, 0.25f)
        neonAnimator1.duration = 1500
        neonAnimator1.repeatCount = ValueAnimator.INFINITE
        neonAnimator1.repeatMode = ValueAnimator.REVERSE
        neonAnimator1.interpolator = AccelerateDecelerateInterpolator()
        neonAnimator1.start()
        floatingAnimators.add(neonAnimator1)
    }

    private fun setupClickListeners() {
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    private fun updateDateLabel() {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ru"))
        val today = Calendar.getInstance()
        currentDate.text = dateFormat.format(today.time)
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
                        // Используем конструктор копирования с ID
                        tasks.add(task.copy(id = document.id))
                    }
                }

                // Обновляем UI с задачами
                updateTaskViews(tasks)

                // Обновляем статистику
                updateStatistics(tasks)
            }
    }

    private fun updateTaskViews(tasks: List<Task>) {
        // Очищаем контейнер
        tasksContainer.removeAllViews()

        // Получаем сегодняшний день
        val calendar = Calendar.getInstance()
        val todayDayName = getDayName(calendar)

        // Фильтруем задачи: сегодняшние + ежедневные
        val todayTasks = tasks.filter {
            (it.dayOfWeek == todayDayName && !it.isRecurring) ||
                    (it.isRecurring)
        }

        // Добавляем задачи в UI
        if (todayTasks.isEmpty()) {
            addEmptyMessage()
        } else {
            for ((index, task) in todayTasks.withIndex()) {
                val taskView = createTaskItemView(task)
                tasksContainer.addView(taskView)

                // Добавляем разделитель если не последняя задача
                if (index < todayTasks.size - 1) {
                    val divider = View(requireContext())
                    divider.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    divider.setBackgroundColor(resources.getColor(android.R.color.white))
                    divider.alpha = 0.1f
                    tasksContainer.addView(divider)
                }
            }
        }
    }

    private fun createTaskItemView(task: Task): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_task_modern, null)

        val title = view.findViewById<TextView>(R.id.taskTitle)
        val description = view.findViewById<TextView>(R.id.taskDescription)
        val checkbox = view.findViewById<CheckBox>(R.id.taskCheckbox)
        val priorityIndicator = view.findViewById<View>(R.id.priorityIndicator)
        val deleteBtn = view.findViewById<ImageView>(R.id.deleteBtn)

        title.text = task.title
        if (task.description.isNotEmpty()) {
            description.text = task.description
            description.visibility = View.VISIBLE
        } else {
            description.visibility = View.GONE
        }

        checkbox.isChecked = task.isCompleted

        // Цвет приоритета
        when(task.priority) {
            3 -> priorityIndicator.setBackgroundColor(requireContext().getColor(android.R.color.holo_red_light))
            2 -> priorityIndicator.setBackgroundColor(requireContext().getColor(android.R.color.holo_orange_light))
            else -> priorityIndicator.setBackgroundColor(requireContext().getColor(android.R.color.holo_green_light))
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

    private fun addEmptyMessage() {
        val emptyView = LayoutInflater.from(requireContext())
            .inflate(R.layout.empty_tasks_state, tasksContainer, false)
        tasksContainer.addView(emptyView)
    }

    private fun updateStatistics(tasks: List<Task>) {
        val calendar = Calendar.getInstance()
        val todayDayName = getDayName(calendar)

        val todayTasks = tasks.filter {
            (it.dayOfWeek == todayDayName && !it.isRecurring) ||
                    (it.isRecurring)
        }

        val completedTasks = tasks.count { it.isCompleted }
        val totalTasks = tasks.size

        todayTasksCount.text = todayTasks.size.toString()
        totalTasksCount.text = totalTasks.toString()

        val percent = if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks * 100).roundToInt()
        } else {
            0
        }

        completedPercent.text = "$percent%"

        // Анимируем изменение чисел
        animateCounter(todayTasksCount, todayTasks.size)
        animateCounter(totalTasksCount, totalTasks)
        animateCounter(completedPercent, percent, "$percent%")
    }

    private fun animateCounter(textView: TextView, targetValue: Int, prefix: String = "") {
        val currentValue = textView.text.toString().toIntOrNull() ?: 0
        val animator = ValueAnimator.ofInt(currentValue, targetValue)
        animator.duration = 500
        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            textView.text = if (prefix.isNotEmpty()) "$prefix$value" else value.toString()
        }
        animator.start()
    }

    private fun loadUserStats() {
        if (currentUserId.isEmpty()) return

        userListener = db.collection("users")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                snapshot?.let { document ->
                    val user = document.toObject(User::class.java)
                    user?.let {
                        // Здесь можно обновить дополнительные статистики
                    }
                }
            }
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_task_modern, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val daySpinner = dialogView.findViewById<Spinner>(R.id.daySpinner)
        val isRecurringSwitch = dialogView.findViewById<Switch>(R.id.isRecurringSwitch)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)
        val saveBtn = dialogView.findViewById<Button>(R.id.saveBtn)

        // Настройка спиннера дней
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Сегодня", "Завтра") + daysOfWeek
        )
        daySpinner.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Новая задача")
            .setView(dialogView)
            .create()

        cancelBtn.setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            if (title.isEmpty()) {
                titleInput.error = "Введите название"
                titleInput.requestFocus()
                return@setOnClickListener
            }

            val description = descriptionInput.text.toString().trim()
            val isRecurring = isRecurringSwitch.isChecked
            val selectedDay = when(val selected = daySpinner.selectedItemPosition) {
                0 -> "Сегодня"
                1 -> "Завтра"
                else -> daysOfWeek[selected - 2]
            }

            saveTask(title, description, selectedDay, isRecurring)
            animateTaskAdded()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveTask(title: String, description: String, day: String, isRecurring: Boolean) {
        if (currentUserId.isEmpty()) return

        // Преобразуем "Сегодня" и "Завтра" в конкретные дни
        val actualDay = when(day) {
            "Сегодня" -> getDayName(Calendar.getInstance())
            "Завтра" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                getDayName(calendar)
            }
            else -> day
        }

        val task = hashMapOf(
            "title" to title,
            "description" to description,
            "dayOfWeek" to actualDay,
            "isRecurring" to isRecurring,
            "isCompleted" to false,
            "userId" to currentUserId,
            "createdAt" to System.currentTimeMillis(),
            "priority" to 1
        )

        db.collection("tasks")
            .add(task)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Задача добавлена ✨", Toast.LENGTH_SHORT).show()
                updateUserTaskStats(incrementCreated = true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun animateTaskAdded() {
        fabAddTask.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction {
                fabAddTask.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun updateTaskStatus(taskId: String, isCompleted: Boolean, task: Task) {
        db.collection("tasks")
            .document(taskId)
            .update("isCompleted", isCompleted)
            .addOnSuccessListener {
                if (isCompleted && !task.isCompleted) {
                    updateUserTaskStats(incrementCompleted = true)
                    showConfettiAnimation()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showConfettiAnimation() {
        // Простая анимация без Lottie
        val confettiView = TextView(requireContext())
        confettiView.text = "🎉"
        confettiView.textSize = 48f
        confettiView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        confettiView.x = requireView().width / 2f
        confettiView.y = requireView().height / 2f

        (requireView() as ViewGroup).addView(confettiView)

        confettiView.animate()
            .translationY(-100f)
            .alpha(0f)
            .setDuration(1000)
            .withEndAction {
                (requireView() as ViewGroup).removeView(confettiView)
            }
            .start()
    }

    private fun updateUserTaskStats(
        incrementCompleted: Boolean = false,
        incrementCreated: Boolean = false,
        decrementTotal: Boolean = false
    ) {
        val updates = hashMapOf<String, Any>()

        if (incrementCompleted) {
            updates["totalTasksCompleted"] = FieldValue.increment(1)
        }

        if (incrementCreated) {
            updates["totalTasksCreated"] = FieldValue.increment(1)
        }

        if (decrementTotal) {
            updates["totalTasksCreated"] = FieldValue.increment(-1)
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

    private fun showEditTaskDialog(task: Task) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_task_modern, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val daySpinner = dialogView.findViewById<Spinner>(R.id.daySpinner)
        val isRecurringSwitch = dialogView.findViewById<Switch>(R.id.isRecurringSwitch)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)
        val saveBtn = dialogView.findViewById<Button>(R.id.saveBtn)

        titleInput.setText(task.title)
        descriptionInput.setText(task.description)
        isRecurringSwitch.isChecked = task.isRecurring

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Сегодня", "Завтра") + daysOfWeek
        )
        daySpinner.adapter = adapter

        // Устанавливаем выбранный день
        val dayIndex = daysOfWeek.indexOf(task.dayOfWeek)
        if (dayIndex != -1) {
            daySpinner.setSelection(dayIndex + 2)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Редактировать задачу")
            .setView(dialogView)
            .create()

        cancelBtn.setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            if (title.isEmpty()) {
                titleInput.error = "Введите название"
                return@setOnClickListener
            }

            val description = descriptionInput.text.toString().trim()
            val isRecurring = isRecurringSwitch.isChecked
            val selectedDay = when(val selected = daySpinner.selectedItemPosition) {
                0 -> "Сегодня"
                1 -> "Завтра"
                else -> daysOfWeek[selected - 2]
            }

            updateTask(task.id, title, description, selectedDay, isRecurring)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateTask(taskId: String, title: String, description: String, day: String, isRecurring: Boolean) {
        val actualDay = when(day) {
            "Сегодня" -> getDayName(Calendar.getInstance())
            "Завтра" -> {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                getDayName(calendar)
            }
            else -> day
        }

        val updates = hashMapOf<String, Any>(
            "title" to title,
            "description" to description,
            "dayOfWeek" to actualDay,
            "isRecurring" to isRecurring
        )

        db.collection("tasks")
            .document(taskId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Задача обновлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
            }
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

    private fun showLoading(show: Boolean) {
        loadingProgress.isVisible = show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.remove()
        userListener?.remove()
        floatingAnimators.forEach { it.cancel() }
        floatingAnimators.clear()
    }
}