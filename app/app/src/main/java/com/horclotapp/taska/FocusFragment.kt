package com.horclotapp.taska

import android.animation.Animator
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class FocusFragment : Fragment() {

    companion object {
        private val daysOfWeekShort = listOf(
            "ВС", "ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ"
        )

        private val daysOfWeekFull = listOf(
            "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
        )

        // Градиенты для приоритетов
        private val priorityGradients = mapOf(
            1 to R.drawable.gradient_line_green,   // низкий
            2 to R.drawable.gradient_line_orange,  // средний
            3 to R.drawable.gradient_line_red      // высокий
        )

        // Базовые цвета для градиентов
        private val priorityBaseColors = mapOf(
            1 to "#4CAF50", // зеленый
            2 to "#FF9800", // оранжевый
            3 to "#F44336"  // красный
        )
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUserId: String

    // UI элементы
    private lateinit var loadingProgress: ProgressBar
    private lateinit var dateContainer: LinearLayout
    private lateinit var timelineContainer: LinearLayout
    private lateinit var fabAddTask: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var currentTimeLine: View
    private lateinit var hourMarkersContainer: LinearLayout
    private lateinit var backgroundTimeline: View

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

    // Хэндлер для обновления текущей временной линии
    private val handler = Handler(Looper.getMainLooper())
    private var updateTimeLineRunnable: Runnable? = null

    // Высота одного часа в пикселях
    private val hourHeightPx = 120

    // Аниматоры для плавных переходов
    private val colorAnimators = mutableMapOf<View, Animator>()

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
        startCurrentTimeLineUpdates()
    }

    private fun initViews(view: View) {
        loadingProgress = view.findViewById(R.id.loadingProgress)
        dateContainer = view.findViewById(R.id.dateContainer)
        timelineContainer = view.findViewById(R.id.timelineContainer)
        fabAddTask = view.findViewById(R.id.fabAddTask)
        currentTimeLine = view.findViewById(R.id.currentTimeLine)
        hourMarkersContainer = view.findViewById(R.id.hourMarkersContainer)
        backgroundTimeline = view.findViewById(R.id.backgroundTimeline)
    }

    private fun setupInfiniteDateSelector() {
        val today = Calendar.getInstance()

        // Создаем 30 дней назад и 30 дней вперед
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
                dateScrollView?.scrollTo(it.left - 100, 0)
            }
        }
    }

    private fun startCurrentTimeLineUpdates() {
        updateTimeLineRunnable = object : Runnable {
            override fun run() {
                if (isSameDay(selectedDate, Calendar.getInstance())) {
                    updateCurrentTimeLine()
                    currentTimeLine.isVisible = true

                    // Анимация свечения текущей линии
                    animateCurrentTimeLineGlow()
                } else {
                    currentTimeLine.isVisible = false
                }

                // Обновляем каждую минуту
                handler.postDelayed(this, 60000)
            }
        }

        // Первый запуск
        updateTimeLineRunnable?.run()
    }

    private fun animateCurrentTimeLineGlow() {
        // Останавливаем предыдущую анимацию
        currentTimeLine.clearAnimation()

        // Анимация пульсации с изменением цвета
        val glowAnim = ObjectAnimator.ofFloat(currentTimeLine, "alpha", 0.4f, 0.9f, 0.4f)
        glowAnim.duration = 3000
        glowAnim.repeatCount = ObjectAnimator.INFINITE
        glowAnim.interpolator = FastOutSlowInInterpolator()
        glowAnim.start()
    }

    private fun updateCurrentTimeLine() {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        // Вычисляем позицию на временной линии
        val totalMinutes = currentHour * 60 + currentMinute
        val position = (totalMinutes * hourHeightPx / 60).toFloat()

        // Анимируем движение линии
        currentTimeLine.animate()
            .translationY(position)
            .setDuration(500)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun updateDateViews() {
        dateContainer.removeAllViews()
        dateViews.clear()

        dates.forEachIndexed { index, date ->
            val dateView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_date, dateContainer, false)

            val dayName = dateView.findViewById<TextView>(R.id.dayName)
            val indicator = dateView.findViewById<View>(R.id.selectionIndicator)

            val dayOfWeekIndex = date.get(Calendar.DAY_OF_WEEK) - 1
            val dayNameText = daysOfWeekShort[dayOfWeekIndex]
            dayName.text = dayNameText

            if (isSameDay(date, Calendar.getInstance())) {
                dayName.setTextColor(Color.parseColor("#4CAF50"))
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
                dayName.setTextColor(resources.getColor(android.R.color.white, null))
                dayName.textSize = 16f
                indicator.isVisible = true

                view.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(200)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            } else {
                if (isSameDay(dates[i], Calendar.getInstance())) {
                    dayName.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    dayName.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                }
                dayName.textSize = 14f
                indicator.isVisible = false

                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(FastOutSlowInInterpolator())
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
                        if (task.time.isEmpty()) {
                            task.time = "00:00"
                        }
                        tasks.add(task)
                    }
                }

                val filteredTasks = tasks.filter { task ->
                    if (task.isRecurring) {
                        true
                    } else {
                        task.date == dateString || task.dayOfWeek == dayOfWeek
                    }
                }

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
        timelineContainer.removeAllViews()

        if (tasks.isEmpty()) {
            addEmptyTimelineMessage()
            return
        }

        val timelineContent = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Добавляем задачи и пустые слоты между ними
        var lastEndTime = 0 // время окончания последней задачи в минутах
        var lastTaskColor = "#2A2E38" // начальный цвет серый

        // Собираем все свободные окна
        val emptySlots = mutableListOf<EmptySlot>()

        tasks.forEachIndexed { index, task ->
            val taskTimeInMinutes = timeToMinutes(task.time)

            // Если это не первая задача и есть промежуток с предыдущей
            if (index > 0 && taskTimeInMinutes > lastEndTime + 30) { // если разрыв больше 30 минут
                val emptySlot = EmptySlot(
                    startTime = lastEndTime,
                    endTime = taskTimeInMinutes,
                    duration = taskTimeInMinutes - lastEndTime,
                    previousColor = lastTaskColor
                )
                emptySlots.add(emptySlot)
            }

            val taskView = createTimelineTaskView(task, index, lastTaskColor)
            timelineContent.addView(taskView)

            // Обновляем цвет для следующего элемента
            lastTaskColor = priorityBaseColors[task.priority] ?: "#4CAF50"

            // Предполагаем длительность задачи 60 минут
            lastEndTime = taskTimeInMinutes + 60

            if (animate) {
                animateTaskView(taskView, index)
            }
        }


        // Добавляем только 2 самых больших свободных окна
        val topEmptySlots = emptySlots
            .sortedByDescending { it.duration }
            .take(2)
            .sortedBy { it.startTime } // Сортируем по времени для правильного порядка

        // Вставляем пустые слоты в нужные места
        var addedSlots = 0
        topEmptySlots.forEach { emptySlot ->
            // Находим позицию для вставки (после задачи, которая заканчивается в startTime)
            val insertPosition = (addedSlots * 2 + 1) // +1 потому что задачи уже добавлены
            val emptySlotView = createEmptySlotView(
                emptySlot.startTime,
                emptySlot.endTime,
                emptySlot.previousColor
            )

            // Вставляем пустой слот после соответствующей задачи
            if (insertPosition < timelineContent.childCount) {
                timelineContent.addView(emptySlotView, insertPosition)
            } else {
                timelineContent.addView(emptySlotView)
            }
            addedSlots++

            // Анимируем появление пустого слота
            if (animate) {
                animateEmptySlotView(emptySlotView, addedSlots)
            }
        }

        timelineContainer.addView(timelineContent)
        updateCurrentTimeLine()

        // Запускаем плавные переходы между цветами
        animateColorTransitions()
    }

    private fun animateEmptySlotView(view: View, index: Int) {
        view.alpha = 0f
        view.translationY = 20f
        view.scaleX = 0.95f
        view.scaleY = 0.95f

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(300)
            .setStartDelay((index * 100 + 200).toLong()) // Немного задержки после задач
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }



    private fun createEmptySlotView(startTime: Int, endTime: Int, previousColor: String): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_empty_slot, null)

        val emptySlotTime = view.findViewById<TextView>(R.id.emptySlotTime)
        val emptySlotTitle = view.findViewById<TextView>(R.id.emptySlotTitle)
        val emptySlotHint = view.findViewById<TextView>(R.id.emptySlotHint)
        val emptySlotCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.emptySlotCard)
        val emptySlotLine = view.findViewById<View>(R.id.emptySlotLine)

        // Устанавливаем время начала промежутка
        emptySlotTime.text = minutesToTime(startTime)

        // Рассчитываем длительность промежутка
        val duration = endTime - startTime
        val durationHours = duration / 60
        val durationMinutes = duration % 60

        // Улучшенные тексты для пустых слотов
        emptySlotTitle.text = "Свободное время"
        emptySlotHint.text = if (durationHours >= 1) {
            "${durationHours}ч ${durationMinutes}мин · Нажмите чтобы добавить занятие"
        } else {
            "${durationMinutes}мин · Добавить короткое занятие"
        }

        // Анимируем переход цвета линии от предыдущего цвета к серому
        animateLineColorTransition(emptySlotLine, previousColor, "#2A2E38")

        // Обработчик клика - открывает диалог добавления задачи на это время
        emptySlotCard.setOnClickListener {
            showAddTaskDialogForTimeSlot(minutesToTime(startTime), minutesToTime(endTime))
        }

        // Добавляем эффект свечения при наведении
        emptySlotCard.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.98f)
                        .scaleY(0.98f)
                        .alpha(0.9f)
                        .setDuration(150)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(150)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
            }
            false
        }

        return view
    }



    private fun showAddTaskDialogForTimeSlot(startTime: String, endTime: String) {
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

        val dayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Сегодня", "Завтра") + daysOfWeekFull
        )
        daySpinner.adapter = dayAdapter

        // Устанавливаем время начала как предзаполненное
        timeInput.setText(startTime)

        // Добавляем подсказку о доступном времени
        timeInput.hint = "Начало (свободно до $endTime)"

        // Фокусируемся на названии
        titleInput.requestFocus()

        priorityRadioGroup.check(R.id.priorityMedium)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить занятие")
            .setMessage("Свободное время: $startTime - $endTime")
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

            val priority = when(priorityRadioGroup.checkedRadioButtonId) {
                R.id.priorityLow -> 1
                R.id.priorityHigh -> 3
                else -> 2
            }

            if (!isValidTime(time)) {
                timeInput.error = "Введите время в формате HH:mm"
                timeInput.requestFocus()
                return@setOnClickListener
            }

            // Проверяем, что выбранное время в пределах свободного окна
            val selectedMinutes = timeToMinutes(time)
            val startMinutes = timeToMinutes(startTime)
            val endMinutes = timeToMinutes(endTime)

            if (selectedMinutes < startMinutes || selectedMinutes >= endMinutes) {
                timeInput.error = "Выберите время в пределах $startTime - $endTime"
                timeInput.requestFocus()
                return@setOnClickListener
            }

            saveTask(title, description, time, selectedDay, isRecurring, priority)
            dialog.dismiss()
        }

        dialog.show()

        // Показываем клавиатуру
        titleInput.postDelayed({
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(titleInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }
    private fun createTimelineTaskView(task: Task, index: Int, previousColor: String): View {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_timeline_task, null)

        val timeTextView = view.findViewById<TextView>(R.id.timeTextView)
        val iconCircle = view.findViewById<ImageView>(R.id.iconCircle)
        val taskTitle = view.findViewById<TextView>(R.id.taskTitle)
        val taskDescription = view.findViewById<TextView>(R.id.taskDescription)
        val taskCard = view.findViewById<androidx.cardview.widget.CardView>(R.id.taskCard)
        val timelineLine = view.findViewById<View>(R.id.timelineLine)

        timeTextView.text = formatTime(task.time)

        val iconRes = priorityIcons[task.priority] ?: R.drawable.ic_default
        val gradientRes = priorityGradients[task.priority] ?: R.drawable.gradient_line_green
        val currentColor = priorityBaseColors[task.priority] ?: "#4CAF50"

        iconCircle.setImageResource(iconRes)

        // Используем градиент для иконки
        try {
            val gradientDrawable = resources.getDrawable(gradientRes, null).mutate() as GradientDrawable
            gradientDrawable.cornerRadius = dpToPx(21).toFloat() // 42dp / 2 = 21dp радиус
            iconCircle.background = gradientDrawable
        } catch (e: Exception) {
            // Если градиент не найден, используем обычный цвет
            iconCircle.setBackgroundColor(Color.parseColor(currentColor))
        }

        // Устанавливаем градиент для линии
        try {
            timelineLine.setBackgroundResource(gradientRes)
        } catch (e: Exception) {
            timelineLine.setBackgroundColor(Color.parseColor(currentColor))
        }

        // Анимируем переход цвета линии от предыдущего элемента
        animateLineColorTransition(timelineLine, previousColor, currentColor)

        iconCircle.setColorFilter(android.graphics.Color.WHITE)

        // Добавляем тень для лучшего визуального разделения
        taskCard.cardElevation = dpToPx(2).toFloat()

        taskTitle.text = task.title

        if (task.description.isNotEmpty()) {
            taskDescription.text = task.description
            taskDescription.isVisible = true
        }

        // Добавляем эффект свечения при наведении
        taskCard.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.98f)
                        .scaleY(0.98f)
                        .translationZ(dpToPx(2).toFloat())
                        .setDuration(150)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationZ(0f)
                        .setDuration(150)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
            }
            false
        }

        taskCard.setOnClickListener {
            showEditTaskDialog(task)
        }

        taskCard.setOnLongClickListener {
            deleteTask(task.id)
            true
        }

        return view
    }

    private fun animateLineColorTransition(view: View, fromColor: String, toColor: String) {
        // Останавливаем предыдущую анимацию для этого view
        colorAnimators[view]?.cancel()

        val colorAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.parseColor(fromColor),
            Color.parseColor(toColor)
        )

        colorAnim.duration = 1000
        colorAnim.interpolator = FastOutSlowInInterpolator()
        colorAnim.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            view.setBackgroundColor(color)
        }

        colorAnim.start()
        colorAnimators[view] = colorAnim
    }

    private fun animateColorTransitions() {
        // Дополнительная анимация для фоновой линии
        val bgColorAnim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.parseColor("#2A2E38"),
            Color.parseColor("#3A3E48"),
            Color.parseColor("#2A2E38")
        )

        bgColorAnim.duration = 4000
        bgColorAnim.repeatCount = ValueAnimator.INFINITE
        bgColorAnim.interpolator = FastOutSlowInInterpolator()
        bgColorAnim.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            backgroundTimeline.setBackgroundColor(color)
        }

        bgColorAnim.start()
        colorAnimators[backgroundTimeline] = bgColorAnim
    }

    private fun animateTaskView(taskView: View, index: Int) {
        taskView.alpha = 0f
        taskView.translationY = 50f
        taskView.scaleX = 0.95f
        taskView.scaleY = 0.95f

        taskView.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setStartDelay((index * 100).toLong())
            .setInterpolator(FastOutSlowInInterpolator())
            .start()
    }

    private fun addEmptyTimelineMessage() {
        val emptyView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dpToPx(100), 0, dpToPx(100))
            }

            addView(ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_empty_calendar)
                layoutParams = LinearLayout.LayoutParams(dpToPx(120), dpToPx(120))
                (layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER

                // Анимация пульсации иконки
                val pulseAnim = ObjectAnimator.ofFloat(this, "alpha", 0.5f, 1f, 0.5f)
                pulseAnim.duration = 2000
                pulseAnim.repeatCount = ObjectAnimator.INFINITE
                pulseAnim.interpolator = FastOutSlowInInterpolator()
                pulseAnim.start()
            })

            addView(TextView(requireContext()).apply {
                text = "Свободный день!"
                setTextColor(resources.getColor(android.R.color.white, null))
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(24), 0, dpToPx(8))

                // Анимация появления текста
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            })

            addView(TextView(requireContext()).apply {
                text = "Идеальное время для планирования\nновых целей и проектов ✨"
                setTextColor(Color.parseColor("#9AA0A6"))
                textSize = 14f
                gravity = Gravity.CENTER
                setLineSpacing(dpToPx(4).toFloat(), 1f)

                // Анимация появления текста с задержкой
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setStartDelay(200)
                    .setDuration(600)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .start()
            })

            addView(Button(requireContext()).apply {
                text = "Добавить первое занятие"
                setBackgroundColor(Color.parseColor("#FF6B6B"))
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dpToPx(32)
                }
                setOnClickListener {
                    showAddTaskDialog()
                }

                // Анимация кнопки
                scaleX = 0f
                scaleY = 0f
                alpha = 0f
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(400)
                    .setInterpolator(FastOutSlowInInterpolator())
                    .setStartDelay(400)
                    .start()
            })
        }

        emptyView.alpha = 0f
        emptyView.scaleX = 0.9f
        emptyView.scaleY = 0.9f

        emptyView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()

        timelineContainer.addView(emptyView)
    }

    private fun setupClickListeners() {
        fabAddTask.setOnClickListener {
            showAddTaskDialog()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAnimations() {
        fabAddTask.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(100)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .setInterpolator(FastOutSlowInInterpolator())
                        .start()
                }
            }
            false
        }
    }

    // Вспомогательные функции для работы со временем
    private fun timeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun minutesToTime(minutes: Int): String {
        val hour = minutes / 60
        val minute = minutes % 60
        return String.format("%02d:%02d", hour, minute)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun showLoading(show: Boolean) {
        loadingProgress.isVisible = show
    }

    private fun saveTask(title: String, description: String, time: String,
                         day: String, isRecurring: Boolean, priority: Int) {
        if (currentUserId.isEmpty()) return

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
                // Перезагружаем график для текущей даты с анимацией
                loadTasksForDate(selectedDate, animate = true)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAddTaskDialog() {
        showAddTaskDialogForTime("")
    }

    private fun showAddTaskDialogForTime(prefilledTime: String) {
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

        val dayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Сегодня", "Завтра") + daysOfWeekFull
        )
        daySpinner.adapter = dayAdapter

        // Устанавливаем предзаполненное время
        timeInput.setText(prefilledTime)

        // Фокусируемся на названии
        titleInput.requestFocus()

        priorityRadioGroup.check(R.id.priorityMedium)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить занятие на $prefilledTime")
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

            val priority = when(priorityRadioGroup.checkedRadioButtonId) {
                R.id.priorityLow -> 1
                R.id.priorityHigh -> 3
                else -> 2
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

        // Показываем клавиатуру
        titleInput.postDelayed({
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(titleInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
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

        // Скрываем daySpinner
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
                else -> 2
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

    override fun onDestroyView() {
        super.onDestroyView()
        tasksListener?.remove()
        updateTimeLineRunnable?.let {
            handler.removeCallbacks(it)
        }

        // Останавливаем все анимации
        colorAnimators.values.forEach { it.cancel() }
        colorAnimators.clear()
    }
}

private data class EmptySlot(
    val startTime: Int,
    val endTime: Int,
    val duration: Int,
    val previousColor: String
)

data class Task(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val dayOfWeek: String = "",
    val date: String = "",
    var time: String = "",
    val isRecurring: Boolean = false,
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val priority: Int = 2
)