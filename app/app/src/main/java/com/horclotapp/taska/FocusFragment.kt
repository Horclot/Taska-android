package com.horclotapp.taska

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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

    companion object {
        private val daysOfWeekShort = listOf(
            "ВС", "ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ"
        )

        private val daysOfWeekFull = listOf(
            "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
        )
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String

    // UI элементы
    private lateinit var loadingProgress: ProgressBar
    private lateinit var dateContainer: LinearLayout
    private lateinit var timelineContainer: LinearLayout
    private lateinit var timelineLine: View
    private lateinit var fabAddTask: com.google.android.material.floatingactionbutton.FloatingActionButton

    // Дата и задачи
    private var selectedDate: Calendar = Calendar.getInstance()
    private val dates = mutableListOf<Calendar>()
    private val dateViews = mutableListOf<View>()

    // Слушатель Firestore
    private var tasksListener: ListenerRegistration? = null

    // Иконки для приоритетов
    private val priorityIcons = mapOf(
        1 to R.drawable.ic_low_priority,
        2 to R.drawable.ic_medium_priority,
        3 to R.drawable.ic_high_priority
    )

    // Цвета для приоритетов
    private val priorityColors = mapOf(
        1 to "#4CAF50", // зеленый
        2 to "#FF9800", // оранжевый
        3 to "#F44336"  // красный
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_focus, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        initViews(view)
        setupInfiniteDateSelector()
        setupClickListeners()
        setupAnimations()
        loadTasksForDate(selectedDate, animate = true)
    }

    private fun initViews(view: View) {
        loadingProgress = view.findViewById(R.id.loadingProgress)
        dateContainer = view.findViewById(R.id.dateContainer)
        timelineContainer = view.findViewById(R.id.timelineContainer)
        timelineLine = view.findViewById(R.id.timelineLine)
        fabAddTask = view.findViewById(R.id.fabAddTask)
    }

    private fun setupInfiniteDateSelector() {
        val today = Calendar.getInstance()

        // Создаем 30 дней назад и 30 дней вперед (можно увеличить для бесконечности)
        for (i in -30..30) {
            val date = Calendar.getInstance()
            date.add(Calendar.DAY_OF_YEAR, i)
            dates.add(date)
        }

        // Находим индекс сегодняшней даты
        val todayIndex = dates.indexOfFirst {
            isSameDay(it, Calendar.getInstance())
        }

        updateDateViews()
        selectDate(todayIndex)

        // Прокручиваем к сегодняшней дате
        view?.post {
            val dateScrollView = view?.findViewById<HorizontalScrollView>(R.id.dateScrollView)
            val selectedView = if (todayIndex < dateViews.size) dateViews[todayIndex] else null
            selectedView?.let {
                dateScrollView?.scrollTo(it.left - 100, 0) // Прокрутка с небольшим отступом
            }
        }
    }

    private fun updateDateViews() {
        dateContainer.removeAllViews()
        dateViews.clear()

        dates.forEachIndexed { index, date ->
            val dateView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_date, dateContainer, false)

            val dayName = dateView.findViewById<TextView>(R.id.dayName)
            val indicator = dateView.findViewById<View>(R.id.selectionIndicator)

            // Устанавливаем короткое название дня недели
            val dayOfWeekIndex = date.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY = 1
            val dayNameText = daysOfWeekShort[dayOfWeekIndex]
            dayName.text = dayNameText

            // Проверяем, сегодня ли это
            if (isSameDay(date, Calendar.getInstance())) {
                dayName.setTextColor(Color.parseColor("#4CAF50")) // Зеленый цвет для сегодня
            } else {
                dayName.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            }

            dateView.setOnClickListener {
                selectDate(index)
                selectedDate = date
                loadTasksForDate(date, animate = true)
            }

            dateContainer.addView(dateView)
            dateViews.add(dateView)
        }
    }

    private fun selectDate(index: Int) {
        dateViews.forEachIndexed { i, view ->
            val dayName = view.findViewById<TextView>(R.id.dayName)
            val indicator = view.findViewById<View>(R.id.selectionIndicator)

            if (i == index) {
                // Выбранная дата
                dayName.setTextColor(resources.getColor(android.R.color.white, null))
                dayName.textSize = 16f
                indicator.isVisible = true

                // Анимация выбора
                view.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .start()
            } else {
                // Не выбранная дата
                if (isSameDay(dates[i], Calendar.getInstance())) {
                    // Сегодняшний день - зеленый
                    dayName.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    // Остальные дни - серые
                    dayName.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                }
                dayName.textSize = 14f
                indicator.isVisible = false

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    private fun loadTasksForDate(date: Calendar, animate: Boolean = false) {
        if (currentUserId.isEmpty()) {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date.time)
        val dayOfWeek = getDayName(date)

        tasksListener?.remove()

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
                        task.id = document.id
                        // Если время не установлено, устанавливаем по умолчанию
                        if (task.time.isEmpty()) {
                            task.time = "00:00"
                        }
                        tasks.add(task)
                    }
                }

                // Фильтруем задачи для выбранной даты
                val filteredTasks = tasks.filter { task ->
                    // Если задача повторяющаяся, показываем для любой даты
                    if (task.isRecurring) {
                        true
                    } else {
                        // Проверяем по дате или дню недели (для совместимости со старыми задачами)
                        task.date == dateString || task.dayOfWeek == dayOfWeek
                    }
                }

                // Сортируем по времени
                val sortedTasks = filteredTasks.sortedBy { task ->
                    val timeParts = task.time.split(":")
                    try {
                        timeParts[0].toInt() * 60 + timeParts[1].toInt()
                    } catch (e: Exception) {
                        0
                    }
                }

                updateTimeline(sortedTasks, animate)
            }
    }

    private fun updateTimeline(tasks: List<Task>, animate: Boolean = false) {
        // Очищаем временную ленту
        timelineContainer.removeAllViews()

        if (tasks.isEmpty()) {
            addEmptyTimelineMessage()
            return
        }

        // Показываем линию времени
        timelineLine.visibility = View.VISIBLE

        // Создаем контейнер для временной ленты
        val timelineContent = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Добавляем задачи
        tasks.forEachIndexed { index, task ->
            val taskView = createTimelineTaskView(task, index)
            timelineContent.addView(taskView)

            // Применяем анимацию если нужно
            if (animate) {
                animateTaskView(taskView, index)
            }
        }

        timelineContainer.addView(timelineContent)

        // Обновляем высоту линии в зависимости от количества задач
        updateTimelineLineHeight(tasks.size)
    }

    private fun createTimelineTaskView(task: Task, index: Int): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_timeline_task, null)

        val timeTextView = view.findViewById<TextView>(R.id.timeTextView)
        val iconCircle = view.findViewById<ImageView>(R.id.iconCircle)
        val taskTitle = view.findViewById<TextView>(R.id.taskTitle)
        val taskDescription = view.findViewById<TextView>(R.id.taskDescription)
        val taskCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.taskCard)

        // Устанавливаем время
        timeTextView.text = formatTime(task.time)

        // Устанавливаем иконку и цвет по приоритету
        val iconRes = priorityIcons[task.priority] ?: R.drawable.ic_default
        val color = priorityColors[task.priority] ?: "#4CAF50"

        iconCircle.setImageResource(iconRes)

        // Создаем круглый фон с нужным цветом
        val drawable = resources.getDrawable(R.drawable.bg_circle_blue, null).mutate()
        drawable.setTint(android.graphics.Color.parseColor(color))
        iconCircle.background = drawable

        // Настраиваем цвет иконки
        iconCircle.setColorFilter(android.graphics.Color.WHITE)

        taskTitle.text = task.title

        if (task.description.isNotEmpty()) {
            taskDescription.text = task.description
            taskDescription.isVisible = true
        }

        // Обработчик клика на карточку
        taskCard.setOnClickListener {
            showEditTaskDialog(task)
        }

        // Длинный клик для удаления
        taskCard.setOnLongClickListener {
            deleteTask(task.id)
            true
        }

        return view
    }

    private fun animateTaskView(taskView: View, index: Int) {
        // Начальные значения для анимации
        taskView.alpha = 0f
        taskView.translationY = 50f

        // Запускаем анимацию с задержкой для каждого элемента
        taskView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay((index * 100).toLong())
            .start()
    }

    private fun addEmptyTimelineMessage() {
        // Скрываем линию времени
        timelineLine.visibility = View.GONE

        val emptyView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 100, 0, 100)
            }

            addView(TextView(requireContext()).apply {
                text = "На этот день занятий нет"
                setTextColor(resources.getColor(android.R.color.white, null))
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 16)
            })

            addView(TextView(requireContext()).apply {
                text = "Добавьте первое занятие в график!"
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                textSize = 14f
                gravity = Gravity.CENTER
            })
        }

        // Анимация пустого состояния
        emptyView.alpha = 0f
        emptyView.translationY = 30f
        emptyView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(400)
            .start()

        timelineContainer.addView(emptyView)
    }

    private fun updateTimelineLineHeight(taskCount: Int) {
        if (taskCount == 0) return

        // Рассчитываем высоту линии: (кол-во задач * высота одного элемента) + отступы
        val density = resources.displayMetrics.density
        val taskHeight = (120 * density).toInt() // 120dp в пиксели
        val marginBetweenTasks = (32 * density).toInt() // 32dp в пиксели
        val totalHeight = (taskCount * taskHeight) + ((taskCount - 1) * marginBetweenTasks)

        val layoutParams = timelineLine.layoutParams
        layoutParams.height = totalHeight
        timelineLine.layoutParams = layoutParams
    }

    private fun setupClickListeners() {
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAnimations() {
        // Анимация для кнопки добавления
        fabAddTask.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(100)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
            }
            false
        }
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_task_modern, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val timeInput = dialogView.findViewById<EditText>(R.id.timeInput)
        val daySpinner = dialogView.findViewById<Spinner>(R.id.daySpinner)
        val isRecurringSwitch = dialogView.findViewById<Switch>(R.id.isRecurringSwitch)
        val priorityRadioGroup = dialogView.findViewById<RadioGroup>(R.id.priorityRadioGroup)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)
        val saveBtn = dialogView.findViewById<Button>(R.id.saveBtn)

        // Настройка спиннера дней
        val dayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Сегодня", "Завтра") + daysOfWeekFull
        )
        daySpinner.adapter = dayAdapter

        // Устанавливаем текущее время
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        timeInput.setText(currentTime)

        // Устанавливаем средний приоритет по умолчанию
        priorityRadioGroup.check(R.id.priorityMedium)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить занятие в график")
            .setView(dialogView)
            .create()

        cancelBtn.setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            if (title.isEmpty()) {
                titleInput.error = "Введите название занятия"
                titleInput.requestFocus()
                return@setOnClickListener
            }

            val description = descriptionInput.text.toString().trim()
            val time = timeInput.text.toString().trim()
            val isRecurring = isRecurringSwitch.isChecked
            val selectedDay = when(val selected = daySpinner.selectedItemPosition) {
                0 -> "Сегодня"
                1 -> "Завтра"
                else -> daysOfWeekFull[selected - 2]
            }

            // Получаем выбранный приоритет
            val priority = when(priorityRadioGroup.checkedRadioButtonId) {
                R.id.priorityLow -> 1
                R.id.priorityHigh -> 3
                else -> 2 // средний по умолчанию
            }

            if (!isValidTime(time)) {
                timeInput.error = "Введите время в формате HH:mm"
                timeInput.requestFocus()
                return@setOnClickListener
            }

            saveTask(title, description, time, selectedDay, isRecurring, priority)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun saveTask(title: String, description: String, time: String,
                         day: String, isRecurring: Boolean, priority: Int) {
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

        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)

        val task = hashMapOf(
            "title" to title,
            "description" to description,
            "dayOfWeek" to actualDay,
            "date" to dateString,
            "time" to time,
            "isRecurring" to isRecurring,
            "isCompleted" to false,
            "userId" to currentUserId,
            "createdAt" to System.currentTimeMillis(),
            "priority" to priority
        )

        db.collection("tasks")
            .add(task)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Занятие добавлено в график ✨", Toast.LENGTH_SHORT).show()
                animateTaskAdded()
                // Перезагружаем график для текущей даты с анимацией
                loadTasksForDate(selectedDate, animate = true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditTaskDialog(task: Task) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_task_modern, null)

        val titleInput = dialogView.findViewById<EditText>(R.id.titleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.descriptionInput)
        val timeInput = dialogView.findViewById<EditText>(R.id.timeInput)
        val isRecurringSwitch = dialogView.findViewById<Switch>(R.id.isRecurringSwitch)
        val priorityRadioGroup = dialogView.findViewById<RadioGroup>(R.id.priorityRadioGroup)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancelBtn)
        val saveBtn = dialogView.findViewById<Button>(R.id.saveBtn)

        titleInput.setText(task.title)
        descriptionInput.setText(task.description)
        timeInput.setText(task.time)
        isRecurringSwitch.isChecked = task.isRecurring

        // Устанавливаем приоритет задачи
        when(task.priority) {
            1 -> priorityRadioGroup.check(R.id.priorityLow)
            3 -> priorityRadioGroup.check(R.id.priorityHigh)
            else -> priorityRadioGroup.check(R.id.priorityMedium)
        }

        // Скрываем daySpinner, так как день задачи менять не будем
        val daySpinner = dialogView.findViewById<Spinner>(R.id.daySpinner)
        daySpinner.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Редактировать занятие")
            .setView(dialogView)
            .create()

        cancelBtn.setOnClickListener { dialog.dismiss() }

        saveBtn.setOnClickListener {
            val title = titleInput.text.toString().trim()
            if (title.isEmpty()) {
                titleInput.error = "Введите название занятия"
                return@setOnClickListener
            }

            val description = descriptionInput.text.toString().trim()
            val time = timeInput.text.toString().trim()
            val isRecurring = isRecurringSwitch.isChecked

            // Получаем выбранный приоритет
            val priority = when(priorityRadioGroup.checkedRadioButtonId) {
                R.id.priorityLow -> 1
                R.id.priorityHigh -> 3
                else -> 2 // средний по умолчанию
            }

            if (!isValidTime(time)) {
                timeInput.error = "Введите время в формате HH:mm"
                timeInput.requestFocus()
                return@setOnClickListener
            }

            updateTask(task.id, title, description, time, isRecurring, priority)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateTask(taskId: String, title: String, description: String,
                           time: String, isRecurring: Boolean, priority: Int) {
        val updates = hashMapOf<String, Any>(
            "title" to title,
            "description" to description,
            "time" to time,
            "isRecurring" to isRecurring,
            "priority" to priority
        )

        db.collection("tasks")
            .document(taskId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Занятие обновлено", Toast.LENGTH_SHORT).show()
                // Перезагружаем график для текущей даты с анимацией
                loadTasksForDate(selectedDate, animate = true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteTask(taskId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить занятие")
            .setMessage("Вы уверены, что хотите удалить это занятие из графика?")
            .setPositiveButton("Удалить") { dialog, _ ->
                db.collection("tasks")
                    .document(taskId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Занятие удалено", Toast.LENGTH_SHORT).show()
                        // Перезагружаем график для текущей даты с анимацией
                        loadTasksForDate(selectedDate, animate = true)
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

    // Вспомогательные функции
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getDayName(calendar: Calendar): String {
        return when(calendar.get(Calendar.DAY_OF_WEEK)) {
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

    private fun formatTime(time: String): String {
        return try {
            val parts = time.split(":")
            if (parts.size == 2) {
                "${parts[0]}:${parts[1]}"
            } else {
                "00:00"
            }
        } catch (e: Exception) {
            "00:00"
        }
    }

    private fun isValidTime(time: String): Boolean {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return false

            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()

            hours in 0..23 && minutes in 0..59
        } catch (e: Exception) {
            false
        }
    }

    private fun showLoading(show: Boolean) {
        loadingProgress.isVisible = show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.remove()
    }
}

// Обновленная модель Task (убрано поле isCompleted)
data class Task(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val dayOfWeek: String = "", // Понедельник, Вторник и т.д.
    val date: String = "", // yyyy-MM-dd
    var time: String = "", // HH:mm
    val isRecurring: Boolean = false,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Int = 2 // 1 - низкий, 2 - средний, 3 - высокий
)