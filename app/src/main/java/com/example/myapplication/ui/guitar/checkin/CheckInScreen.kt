package com.example.myapplication.ui.guitar.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.guitar.audio.AudioSettings
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.training.SessionResult
import com.example.myapplication.ui.guitar.training.TrainingSessionConfig
import com.example.myapplication.ui.guitar.training.TrainingSessionScreen
import com.example.myapplication.ui.guitar.training.model.Difficulty
import com.example.myapplication.ui.guitar.training.model.TrainingMode
import com.example.myapplication.ui.guitar.training.model.TrainingSettings
import com.example.myapplication.ui.guitar.training.stats.TrainingStats
import com.example.myapplication.ui.guitar.training.stats.TrainingStatsRepository
import com.example.myapplication.ui.guitar.training.stats.getTodayString
import com.example.myapplication.ui.theme.CardBackground
import com.example.myapplication.ui.theme.CardText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

@Composable
fun CheckInScreen(
    modifier: Modifier = Modifier,
    tuning: Tuning = Tuning.Standard,
    audioSettings: AudioSettings = AudioSettings(),
    settings: TrainingSettings = TrainingSettings(),
    onSettingsChange: (TrainingSettings) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { TrainingStatsRepository(context) }
    val stats by repository.statsFlow.collectAsState(initial = TrainingStats())
    val scope = rememberCoroutineScope()

    val dailySettings = remember(settings) {
        settings.copy(
            modeDifficulties = TrainingMode.entries.associateWith {
                when (settings.dailyCheckInDifficulty) {
                    Difficulty.Easy -> if (Random.nextFloat() < 0.1f) Difficulty.Medium else Difficulty.Easy
                    Difficulty.Medium -> if (Random.nextFloat() < 0.1f) Difficulty.Hard else Difficulty.Medium
                    Difficulty.Hard -> Difficulty.Hard
                }
            }
        )
    }

    var sessionConfig by remember { mutableStateOf<TrainingSessionConfig?>(null) }
    var justFinishedResult by remember { mutableStateOf<SessionResult?>(null) }

    sessionConfig?.let { config ->
        TrainingSessionScreen(
            config = config,
            settings = dailySettings,
            tuning = tuning,
            audioSettings = audioSettings,
            onFinish = { result ->
                scope.launch {
                    repository.recordSession(result.correct, result.total, result.elapsedSeconds)
                    if (result.completed) {
                        repository.recordCheckIn()
                    }
                }
                if (result.completed) {
                    justFinishedResult = result
                }
                sessionConfig = null
            },
            onSettingsChange = onSettingsChange,
            modifier = modifier
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val width = constraints.maxWidth.toFloat()
            val height = constraints.maxHeight.toFloat()
            val primaryTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryTint, MaterialTheme.colorScheme.background),
                            center = Offset(width / 2f, height * 0.14f),
                            radius = (width.coerceAtLeast(height) * 0.95f)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "每日打卡",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CardText
                    )

                    StreakCard(stats = stats)

                    StatsSummaryCard(stats = stats)

                    PracticeCalendar(stats = stats)

                    justFinishedResult?.let { result ->
                        PanelCard {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "上一轮成绩",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CardText
                                )
                                Text(
                                    text = "${result.correct} / ${result.total} 正确",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "用时 ${result.elapsedSeconds / 60}分${result.elapsedSeconds % 60}秒",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CardText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    val checkedInToday = stats.lastCheckInDate == getTodayString()
                    Button(
                        onClick = {
                            sessionConfig = TrainingSessionConfig(
                                mode = null,
                                questionCount = settings.dailyQuestionCount
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (checkedInToday) "再次挑战 ${settings.dailyQuestionCount} 题"
                            else "开始每日 ${settings.dailyQuestionCount} 题"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun StreakCard(stats: TrainingStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatColumn(
                value = "${stats.currentStreak} 🔥",
                label = "连续打卡",
                contentColor = CardText
            )
            StatColumn(
                value = stats.longestStreak.toString(),
                label = "最长连胜",
                contentColor = CardText
            )
            StatColumn(
                value = stats.practiceDates.size.toString(),
                label = "打卡天数",
                contentColor = CardText
            )
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun StatsSummaryCard(stats: TrainingStats) {
    PanelCard {
        Column {
            Text(
                text = "累计数据",
                style = MaterialTheme.typography.titleMedium,
                color = CardText
            )
            Spacer(modifier = Modifier.height(8.dp))
            val accuracy = if (stats.totalAnswered > 0) {
                "${(stats.totalCorrect * 100 / stats.totalAnswered)}%"
            } else "-"
            SummaryRow(label = "总正确率", value = accuracy)
            SummaryRow(
                label = "总练习时长",
                value = "${stats.totalPracticeSeconds / 60} 分钟"
            )
            SummaryRow(
                label = "累计答题",
                value = "${stats.totalAnswered} 题"
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CardText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = CardText
        )
    }
}

private fun Calendar.addMonths(delta: Int): Pair<Int, Int> {
    val cal = Calendar.getInstance().apply { set(this@addMonths.get(Calendar.YEAR), this@addMonths.get(Calendar.MONTH), 1) }
    cal.add(Calendar.MONTH, delta)
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
}

@Composable
private fun PracticeCalendar(stats: TrainingStats) {
    val today = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }

    val cells = remember(currentYear, currentMonth) { monthCells(currentYear, currentMonth + 1) }
    val title = remember(currentYear, currentMonth) {
        SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(
            Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }.time
        )
    }

    fun changeMonth(delta: Int) {
        val (y, m) = Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }.addMonths(delta)
        currentYear = y
        currentMonth = m
    }

    PanelCard(modifier = Modifier.height(280.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (dragAmount < -30f) changeMonth(1)
                        else if (dragAmount > 30f) changeMonth(-1)
                    }
                }
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CardText,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { weekday ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weekday,
                            style = MaterialTheme.typography.bodySmall,
                            color = CardText.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cells.size) { index ->
                    when (val cell = cells[index]) {
                        is CalendarCell.Empty -> Spacer(modifier = Modifier.aspectRatio(1f))
                        is CalendarCell.Day -> DayDot(
                            label = cell.label,
                            practiced = cell.date in stats.practiceDates,
                            isToday = cell.isToday
                        )
                    }
                }
            }
        }
    }
}

private sealed class CalendarCell {
    data object Empty : CalendarCell()
    data class Day(
        val date: String,
        val label: String,
        val isToday: Boolean
    ) : CalendarCell()
}

private fun monthCells(year: Int, month: Int): List<CalendarCell> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val labelFormat = SimpleDateFormat("d", Locale.getDefault())
    val today = getTodayString()
    val calendar = Calendar.getInstance().apply {
        set(year, month - 1, 1)
        firstDayOfWeek = Calendar.MONDAY
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstWeekday = calendar.get(Calendar.DAY_OF_WEEK)
    val leading = (firstWeekday - Calendar.MONDAY + 7) % 7

    val cells = mutableListOf<CalendarCell>()
    repeat(leading) { cells.add(CalendarCell.Empty) }
    for (day in 1..daysInMonth) {
        calendar.set(Calendar.DAY_OF_MONTH, day)
        val date = dateFormat.format(calendar.time)
        cells.add(CalendarCell.Day(date, labelFormat.format(calendar.time), date == today))
    }
    return cells
}

@Composable
private fun DayDot(label: String, practiced: Boolean, isToday: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(
                    when {
                        practiced -> Color(0xFF43A047)
                        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else -> CardBackground
                    }
                )
                .then(
                    if (isToday) Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (practiced) Color.White else CardText.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class CalendarDay(val date: String, val dayLabel: String)
