package com.example.myapplication.ui.guitar.training.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.guitar.GuitarFretboard
import com.example.myapplication.ui.guitar.chord.ChordNote
import com.example.myapplication.ui.guitar.chord.Tuning
import kotlinx.coroutines.delay

@Composable
fun FretboardQuestionView(
    notes: List<ChordNote>,
    tuning: Tuning,
    startFret: Float,
    endFret: Float,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    showNoteNames: Boolean = true,
    highlightColorOverride: Color? = null,
    singleSelection: Boolean = false,
    onNotesChanged: (List<ChordNote>) -> Unit = {},
    onPlayNote: (stringIndex: Int, fretIndex: Int) -> Unit = { _, _ -> }
) {
    var vibratingNotes by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var vibrationTrigger by remember { mutableIntStateOf(0) }
    val currentNotes by rememberUpdatedState(notes)

    GuitarFretboard(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp),
        tuning = tuning,
        startFret = startFret,
        endFret = endFret,
        chordNotes = notes,
        showNoteNames = showNoteNames,
        highlightColorOverride = highlightColorOverride,
        vibratingNotes = vibratingNotes,
        vibrationTrigger = vibrationTrigger,
        onFretTapped = { stringIndex, fretIndex ->
            if (!readOnly) {
                val updated = if (singleSelection) {
                    List<ChordNote>(6) { ChordNote.Muted }.toMutableList().apply {
                        this[stringIndex] = if (fretIndex == 0) ChordNote.Open else ChordNote.Fretted(fretIndex)
                    }
                } else {
                    currentNotes.toMutableList().apply {
                        val current = this[stringIndex]
                        this[stringIndex] = when {
                            current is ChordNote.Fretted && current.fret == fretIndex -> ChordNote.Muted
                            fretIndex == 0 -> ChordNote.Open
                            else -> ChordNote.Fretted(fretIndex)
                        }
                    }
                }
                onNotesChanged(updated)
            }
            vibratingNotes = listOf(stringIndex to fretIndex)
            vibrationTrigger++
            onPlayNote(stringIndex, fretIndex)
        },
        onPlayNote = { _, _ -> }
    )
}

@Composable
fun AnswerOptionsGrid(
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    columns: Int = 4
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(options) { option ->
            OutlinedButton(
                onClick = { onOptionSelected(option) },
                enabled = enabled,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.aspectRatio(2f)
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TimerDisplay(
    seconds: Int,
    modifier: Modifier = Modifier,
    totalSeconds: Int? = null
) {
    val display = "%02d:%02d".format(seconds / 60, seconds % 60)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = display,
            style = MaterialTheme.typography.headlineSmall
        )
        if (totalSeconds != null && totalSeconds > 0) {
            val progress = (totalSeconds - seconds).coerceIn(0, totalSeconds).toFloat() / totalSeconds
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AnswerFeedbackOverlay(
    isCorrect: Boolean,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(150)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCorrect) Color(0xFF43A047) else Color(0xFFE53935)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    text = if (isCorrect) "正确" else "错误",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 48.dp, vertical = 24.dp)
                )
            }
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            delay(400)
            onAnimationEnd()
        }
    }
}

@Composable
fun CelebrationOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    message: String = "打卡完成！"
) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "celebrationScale"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFB300)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.scale(scale)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "🎉",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
