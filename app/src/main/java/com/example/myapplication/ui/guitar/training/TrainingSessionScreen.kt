package com.example.myapplication.ui.guitar.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.guitar.audio.AudioSettings
import com.example.myapplication.ui.guitar.audio.GuitarAudioPlayer
import com.example.myapplication.ui.guitar.chord.ChordNote
import com.example.myapplication.ui.guitar.chord.ChordVoicing
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.training.components.AnswerFeedbackOverlay
import com.example.myapplication.ui.guitar.training.components.AnswerOptionsGrid
import com.example.myapplication.ui.guitar.training.components.CelebrationOverlay
import com.example.myapplication.ui.guitar.training.components.FretboardQuestionView
import com.example.myapplication.ui.guitar.training.components.TimerDisplay
import com.example.myapplication.ui.guitar.training.engine.generateQuestion
import com.example.myapplication.ui.guitar.training.engine.intervalName
import com.example.myapplication.ui.guitar.training.engine.toDisplayName
import com.example.myapplication.ui.theme.CardBackground
import com.example.myapplication.ui.theme.CardText
import com.example.myapplication.ui.guitar.training.model.ChordProgressionQuestion
import com.example.myapplication.ui.guitar.training.model.ChordRecognitionQuestion
import com.example.myapplication.ui.guitar.training.model.Difficulty
import com.example.myapplication.ui.guitar.training.model.EarTrainingQuestion
import com.example.myapplication.ui.guitar.training.model.FindNoteQuestion
import com.example.myapplication.ui.guitar.training.model.IntervalRecognitionQuestion
import com.example.myapplication.ui.guitar.training.model.MarkChordQuestion
import com.example.myapplication.ui.guitar.training.model.NoteRecognitionQuestion
import com.example.myapplication.ui.guitar.training.model.ScaleFindNotesQuestion
import com.example.myapplication.ui.guitar.training.model.TrainingMode
import com.example.myapplication.ui.guitar.training.model.TrainingQuestion
import com.example.myapplication.ui.guitar.training.model.TrainingSettings
import com.example.myapplication.ui.guitar.training.model.UserAnswer
import com.example.myapplication.ui.guitar.training.model.difficultyDescription
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class TrainingSessionConfig(
    val mode: TrainingMode?,
    val questionCount: Int
)

data class SessionResult(
    val total: Int,
    val correct: Int,
    val elapsedSeconds: Int,
    val completed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSessionScreen(
    config: TrainingSessionConfig,
    settings: TrainingSettings,
    tuning: Tuning,
    audioSettings: AudioSettings,
    onFinish: (SessionResult) -> Unit,
    onSettingsChange: (TrainingSettings) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioPlayer = remember { GuitarAudioPlayer(context) }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    var questions by remember { mutableStateOf(listOf<TrainingQuestion>()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    var userNotes by remember { mutableStateOf(List<ChordNote>(6) { ChordNote.Muted }) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackCorrect by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(tuning, audioSettings) {
        audioPlayer.initialize(tuning, audioSettings)
    }

    LaunchedEffect(config, settings, tuning) {
        val generated = List(config.questionCount) {
            val mode = config.mode ?: TrainingMode.entries.random(Random.Default)
            generateQuestion(mode, settings, tuning)
        }
        questions = generated
        currentIndex = 0
        score = 0
        elapsedSeconds = 0
        isFinished = false
        showCelebration = false
        userNotes = List(6) { ChordNote.Muted }
        selectedOption = null
    }

    val question = questions.getOrNull(currentIndex)

    LaunchedEffect(Unit) {
        while (!isFinished) {
            delay(1000)
            if (!showFeedback) {
                elapsedSeconds++
            }
        }
    }

    LaunchedEffect(question) {
        val q = question
        if (q is EarTrainingQuestion) {
            delay(300)
            audioPlayer.playString(q.referenceString, q.referenceFret)
        }
    }

    fun resetAnswerState() {
        userNotes = List(6) { ChordNote.Muted }
        selectedOption = null
    }

    fun submitAnswer() {
        if (question == null || showFeedback) return
        val answer = if (question.isInteractive()) {
            UserAnswer.Fretboard(userNotes)
        } else {
            val option = selectedOption ?: return
            UserAnswer.Option(option)
        }
        val correct = question.check(answer)
        if (correct) score++
        feedbackCorrect = correct
        showFeedback = true
        playFeedbackSound(audioPlayer, correct)
    }

    fun advance() {
        showFeedback = false
        if (currentIndex < questions.size - 1) {
            currentIndex++
            resetAnswerState()
        } else {
            isFinished = true
            showCelebration = true
            scope.launch {
                delay(1800)
                showCelebration = false
            }
        }
    }

    LaunchedEffect(showFeedback, feedbackCorrect, question) {
        if (showFeedback && feedbackCorrect && question?.isInteractive() == true) {
            delay(800)
            advance()
        }
    }

    if (question == null && !isFinished) {
        BoxLoading(modifier = modifier)
        return
    }

    val progress = if (questions.isNotEmpty()) currentIndex / questions.size.toFloat() else 0f

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onFinish(SessionResult(currentIndex, score, elapsedSeconds, completed = false)) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }

                    val currentMode = config.mode ?: question?.mode
                    currentMode?.let { mode ->
                        Column(horizontalAlignment = Alignment.End) {
                            DifficultySelector(
                                selected = settings.difficultyFor(mode),
                                onSelect = {
                                    onSettingsChange(
                                        settings.copy(
                                            modeDifficulties = settings.modeDifficulties.toMutableMap().apply { put(mode, it) }
                                        )
                                    )
                                }
                            )
                            Text(
                                text = difficultyDescription(mode, settings.difficultyFor(mode)),
                                style = MaterialTheme.typography.bodySmall,
                                color = CardText.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                if (!isFinished && question != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentIndex + 1} / ${questions.size}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TimerDisplay(seconds = elapsedSeconds)
                    }

                    val displayNotes = remember(question, settings) { question.toDisplayNotes(settings) }
                    val isInteractive = question.isInteractive()
                    val singleSelection = question.mode == TrainingMode.FindNote || question.mode == TrainingMode.EarTraining

                    FretboardQuestionView(
                        notes = if (isInteractive) userNotes else displayNotes,
                        tuning = tuning,
                        startFret = question.fretboardStartFret(),
                        endFret = question.fretboardEndFret(),
                        readOnly = !isInteractive,
                        showNoteNames = settings.showNoteNamesOnFretboard(question.mode),
                        highlightColorOverride = if (question.mode == TrainingMode.NoteRecognition) MaterialTheme.colorScheme.primary else null,
                        singleSelection = singleSelection,
                        onNotesChanged = { newNotes ->
                            userNotes = newNotes
                            if (singleSelection) {
                                scope.launch {
                                    delay(200)
                                    submitAnswer()
                                }
                            }
                        },
                        onPlayNote = { stringIndex, fretIndex ->
                            audioPlayer.playString(stringIndex, fretIndex)
                        }
                    )

                    Text(
                        text = questionPrompt(question, settings),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (question.mode == TrainingMode.EarTraining) {
                        Button(
                            onClick = {
                                val q = question as EarTrainingQuestion
                                audioPlayer.playString(q.referenceString, q.referenceFret)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("▶ 再听一次")
                        }
                    }

                    if (isInteractive) {
                        Button(
                            onClick = { submitAnswer() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !showFeedback
                        ) {
                            Text("提交答案")
                        }
                    } else {
                        val options = question.options(settings)
                        AnswerOptionsGrid(
                            options = options,
                            onOptionSelected = { option ->
                                selectedOption = option
                                submitAnswer()
                            },
                            enabled = !showFeedback,
                            columns = if (options.size <= 4) 2 else 4,
                            selectedOption = selectedOption,
                            correctOption = if (showFeedback) correctAnswerText(question, settings) else null,
                            showFeedback = showFeedback,
                            onNext = { advance() }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (isFinished) {
                SessionResultOverlay(
                    score = score,
                    total = questions.size,
                    elapsedSeconds = elapsedSeconds,
                    onFinish = { onFinish(SessionResult(questions.size, score, elapsedSeconds, completed = true)) }
                )
            }

            if (question == null || question.isInteractive()) {
                AnswerFeedbackOverlay(
                    isCorrect = feedbackCorrect,
                    visible = showFeedback,
                    correctAnswerText = if (question != null) correctAnswerText(question, settings) else null,
                    onNext = { advance() }
                )
            }

            CelebrationOverlay(
                visible = showCelebration,
                message = "打卡完成！"
            )
        }
    }
}

@Composable
private fun DifficultySelector(
    selected: Difficulty,
    onSelect: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Difficulty.entries.forEach { difficulty ->
            val isSelected = difficulty == selected
            val color = when (difficulty) {
                Difficulty.Easy -> Color(0xFF3D6B40)
                Difficulty.Medium -> Color(0xFF3A6FA3)
                Difficulty.Hard -> Color(0xFF9B4545)
            }
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .background(
                        color = if (isSelected) color else Color.Transparent,
                        shape = RoundedCornerShape(15.dp)
                    )
                    .then(
                        if (!isSelected) Modifier.border(
                            width = 1.dp,
                            color = CardText.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(15.dp)
                        ) else Modifier
                    )
                    .clickable { onSelect(difficulty) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = difficulty.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Color.White else CardText.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun SessionResultOverlay(
    score: Int,
    total: Int,
    elapsedSeconds: Int,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(durationMillis = 500))
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale.value)
                    .padding(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🎉 训练完成",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "$score / $total 正确",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "用时 ${elapsedSeconds / 60}分${elapsedSeconds % 60}秒",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("完成")
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxLoading(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("准备题目中…", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun TrainingQuestion.isInteractive(): Boolean {
    return when (mode) {
        TrainingMode.FindNote,
        TrainingMode.MarkChord,
        TrainingMode.EarTraining,
        TrainingMode.ScaleFindNotes -> true
        else -> false
    }
}

private fun TrainingQuestion.fretboardStartFret(): Float {
    return when (this) {
        is MarkChordQuestion -> positionStart.toFloat()
        is ScaleFindNotesQuestion -> startFret.toFloat()
        else -> 0f
    }
}

private fun TrainingQuestion.fretboardEndFret(): Float {
    return when (this) {
        is MarkChordQuestion -> (positionEnd + 1).coerceAtMost(12).toFloat()
        is ScaleFindNotesQuestion -> (endFret + 1).coerceAtMost(12).toFloat()
        else -> 12f
    }
}

private fun TrainingQuestion.toDisplayNotes(settings: TrainingSettings): List<ChordNote> {
    return when (this) {
        is NoteRecognitionQuestion -> List<ChordNote>(6) { ChordNote.Muted }.toMutableList().apply {
            this[stringIndex] = if (fret == 0) ChordNote.Open else ChordNote.Fretted(fret)
        }

        is ChordRecognitionQuestion -> voicing.notes

        is IntervalRecognitionQuestion -> List<ChordNote>(6) { ChordNote.Muted }.toMutableList().apply {
            this[lowerString] = if (lowerFret == 0) ChordNote.Open else ChordNote.Fretted(lowerFret)
            this[upperString] = if (upperFret == 0) ChordNote.Open else ChordNote.Fretted(upperFret)
        }

        else -> List(6) { ChordNote.Muted }
    }
}

private fun TrainingQuestion.options(settings: TrainingSettings): List<String> {
    return when (this) {
        is NoteRecognitionQuestion -> settings.effectivePitchClasses(TrainingMode.NoteRecognition)
            .map { it.toDisplayName(settings) }
            .sorted()

        is ChordRecognitionQuestion -> options

        is IntervalRecognitionQuestion -> options

        is ChordProgressionQuestion -> options

        else -> emptyList()
    }
}

@Composable
private fun questionPrompt(question: TrainingQuestion, settings: TrainingSettings): String {
    return when (question) {
        is NoteRecognitionQuestion -> "指板上的这个音是什么？"
        is FindNoteQuestion -> "在 第 ${question.stringIndex + 1} 弦 上找到 ${question.pitchClass.toDisplayName(settings)}"
        is ChordRecognitionQuestion -> "这个和弦是什么？"
        is MarkChordQuestion -> "在 ${question.positionStart}–${question.positionEnd} 品之间按出 ${question.chordName}"
        is IntervalRecognitionQuestion -> "这两个音的音程是？"
        is EarTrainingQuestion -> "听音后，在指板上点出这个音的位置"
        is ScaleFindNotesQuestion -> "在 ${question.startFret}–${question.endFret} 品内，标出 ${question.keyRoot.toDisplayName(settings)} ${question.scaleType.suffix} 的所有音"
        is ChordProgressionQuestion -> {
            val progressionText = question.progression.map { it.first }.joinToString(" – ")
            "${question.keyRoot.toDisplayName(settings)} 大调：${progressionText} 中的第 ${question.chordIndex + 1} 个和弦"
        }
    }
}

private fun correctAnswerText(question: TrainingQuestion, settings: TrainingSettings): String {
    return when (question) {
        is NoteRecognitionQuestion -> question.pitchClass.toDisplayName(settings)
        is FindNoteQuestion -> "第 ${question.stringIndex + 1} 弦 第 ${question.correctFret} 品"
        is ChordRecognitionQuestion -> question.correctName
        is MarkChordQuestion -> question.chordName
        is IntervalRecognitionQuestion -> intervalName(question.intervalSemitones)
        is EarTrainingQuestion -> "${question.pitchClass.toDisplayName(settings)}（参考：第 ${question.referenceString + 1} 弦 第 ${question.referenceFret} 品）"
        is ScaleFindNotesQuestion -> "${question.keyRoot.toDisplayName(settings)} ${question.scaleType.suffix}"
        is ChordProgressionQuestion -> question.chordName
    }
}

private fun playFeedbackSound(audioPlayer: GuitarAudioPlayer, correct: Boolean) {
    if (correct) {
        audioPlayer.playChord(SUCCESS_CHORD.notes)
    } else {
        audioPlayer.playChord(ERROR_CHORD.notes)
    }
}

private val SUCCESS_CHORD = ChordVoicing(
    listOf(
        ChordNote.Fretted(3), ChordNote.Open, ChordNote.Open,
        ChordNote.Open, ChordNote.Fretted(2), ChordNote.Fretted(3)
    )
)

private val ERROR_CHORD = ChordVoicing(
    listOf(
        ChordNote.Muted, ChordNote.Muted, ChordNote.Fretted(2),
        ChordNote.Fretted(2), ChordNote.Open, ChordNote.Open
    )
)

@Composable
fun TrainingHubScreen(
    modifier: Modifier = Modifier,
    tuning: Tuning = Tuning.Standard,
    audioSettings: AudioSettings = AudioSettings(),
    settings: TrainingSettings = TrainingSettings(),
    onSettingsChange: (TrainingSettings) -> Unit = {}
) {
    var selectedConfig by remember { mutableStateOf<TrainingSessionConfig?>(null) }

    selectedConfig?.let { config ->
        TrainingSessionScreen(
            config = config,
            settings = settings,
            tuning = tuning,
            audioSettings = audioSettings,
            onFinish = { selectedConfig = null },
            onSettingsChange = onSettingsChange,
            modifier = modifier
        )
        return
    }

    val background = MaterialTheme.colorScheme.background
    val primaryTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(primaryTint, background),
                    center = Offset(0.5f, 0.55f),
                    radius = 1.1f
                )
            )
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "选择训练模式",
            style = MaterialTheme.typography.headlineSmall,
            color = CardText
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(TrainingMode.entries) { mode ->
                ModeCard(
                    mode = mode,
                    onClick = {
                        selectedConfig = TrainingSessionConfig(
                            mode = mode,
                            questionCount = 10
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: TrainingMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CardText
                )
                Text(
                    text = mode.icon(),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Text(
                text = mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = CardText.copy(alpha = 0.7f)
            )
        }
    }
}

private fun TrainingMode.icon(): String = when (this) {
    TrainingMode.NoteRecognition -> "🎵"
    TrainingMode.FindNote -> "🎯"
    TrainingMode.ChordRecognition -> "🎸"
    TrainingMode.MarkChord -> "🤘"
    TrainingMode.IntervalRecognition -> "↔️"
    TrainingMode.EarTraining -> "👂"
    TrainingMode.ScaleFindNotes -> "📈"
    TrainingMode.ChordProgression -> "🔄"
}
