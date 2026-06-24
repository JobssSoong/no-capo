package com.example.myapplication.ui.guitar

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.DpSize
import com.example.myapplication.ui.guitar.audio.AudioSettings
import com.example.myapplication.ui.guitar.audio.GuitarAudioPlayer
import com.example.myapplication.ui.guitar.chord.ChordControlPanel
import com.example.myapplication.ui.guitar.chord.ChordNote
import com.example.myapplication.ui.guitar.chord.ChordVoicing
import com.example.myapplication.ui.guitar.chord.NoteColors
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.chord.commonVoicings
import com.example.myapplication.ui.guitar.chord.nameChord
import com.example.myapplication.ui.guitar.chord.parseChordName
import com.example.myapplication.ui.guitar.chord.toNoteName
import com.example.myapplication.ui.theme.CardBackground
import com.example.myapplication.ui.theme.CardText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private const val FretCount = 12

private class PerlinNoise(seed: Long = 12345L) {
    private val perm = IntArray(512)

    init {
        val p = (0 until 256).shuffled(Random(seed)).toIntArray()
        for (i in 0 until 512) {
            perm[i] = p[i and 255]
        }
    }

    fun noise(x: Double, y: Double): Double {
        val xi = x.toInt() and 255
        val yi = y.toInt() and 255

        val xf = x - x.toInt()
        val yf = y - y.toInt()

        val u = fade(xf)
        val v = fade(yf)

        val aa = perm[xi] + yi
        val ab = perm[xi] + yi + 1
        val ba = perm[xi + 1] + yi
        val bb = perm[xi + 1] + yi + 1

        val x1 = lerp(grad(perm[aa], xf, yf), grad(perm[ba], xf - 1, yf), u)
        val x2 = lerp(grad(perm[ab], xf, yf - 1), grad(perm[bb], xf - 1, yf - 1), u)

        return lerp(x1, x2, v)
    }

    private fun fade(t: Double) = t * t * t * (t * (t * 6 - 15) + 10)
    private fun lerp(a: Double, b: Double, t: Double) = a + t * (b - a)
    private fun grad(hash: Int, x: Double, y: Double): Double {
        val h = hash and 3
        val u = if (h < 2) x else y
        val v = if (h < 2) y else x
        return (if ((h and 1) == 0) u else -u) + (if ((h and 2) == 0) v else -v)
    }
}

private fun generateEbonyTexture(size: Int = 512): ImageBitmap {
    val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
    val noise = PerlinNoise(seed = 42L)
    val baseGray = 0x0A

    // 总振幅用于归一化
    val totalAmp = 1.0 + 0.65 + 0.42 + 0.27 + 0.18 + 0.12

    for (x in 0 until size) {
        for (y in 0 until size) {
            // 更大尺度、低频为主的大理石纹理
            val n1 = noise.noise(x * 0.0008, y * 0.0008)
            val n2 = noise.noise(x * 0.002 + 50, y * 0.002 + 50) * 0.65
            val n3 = noise.noise(x * 0.005 + 120, y * 0.005 + 120) * 0.42
            val n4 = noise.noise(x * 0.012 + 230, y * 0.012 + 230) * 0.27
            val n5 = noise.noise(x * 0.03 + 340, y * 0.03 + 340) * 0.18
            val n6 = noise.noise(x * 0.07 + 450, y * 0.07 + 450) * 0.12
            val n = (n1 + n2 + n3 + n4 + n5 + n6) / totalAmp

            // 阈值型对比度：只有噪声较强处才形成浅色纹理，大部分区域保持暗色
            val t = kotlin.math.abs(n)
            val vein = when {
                t <= 0.55 -> 0.0
                t >= 0.95 -> 1.0
                else -> {
                    val x = (t - 0.55) / 0.4
                    x * x * (3 - 2 * x)
                }
            }

            val intensity = (vein * 200).toInt()
            val gray = (baseGray + intensity).coerceIn(0, 0xFF)

            bitmap.setPixel(x, y, android.graphics.Color.argb(0xFF, gray, gray, gray))
        }
    }
    return bitmap.asImageBitmap()
}

@Composable
private fun rememberEbonyTexture(): ImageBitmap? {
    val texture by produceState<ImageBitmap?>(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            generateEbonyTexture(512)
        }
    }
    return texture
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuitarFretboardScreen(
    modifier: Modifier = Modifier,
    tuning: Tuning = Tuning.Standard,
    audioSettings: AudioSettings = AudioSettings()
) {
    val context = LocalContext.current
    val audioPlayer = remember { GuitarAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    var isChordMode by remember { mutableStateOf(false) }

    var selectedString by remember { mutableIntStateOf(-1) }
    var selectedFret by remember { mutableIntStateOf(-1) }

    var chordNotes by remember { mutableStateOf<List<ChordNote>>(List(6) { ChordNote.Muted }) }
    var availableVoicings by remember { mutableStateOf<List<ChordVoicing>>(listOf()) }
    var currentVoicingIndex by remember { mutableIntStateOf(0) }
    var vibratingNotes by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    var vibrationTrigger by remember { mutableIntStateOf(0) }

    var startFret by remember { mutableFloatStateOf(0f) }
    var endFret by remember { mutableFloatStateOf(FretCount.toFloat()) }

    LaunchedEffect(tuning, audioSettings) {
        audioPlayer.initialize(tuning, audioSettings)
        // 调弦或音效变化后，原有按法对应的音高/音色已改变，清空和弦/选中状态
        chordNotes = List(6) { ChordNote.Muted }
        availableVoicings = emptyList()
        currentVoicingIndex = 0
        selectedString = -1
        selectedFret = -1
    }

    val currentChordName = remember(chordNotes, tuning) {
        nameChord(ChordVoicing(chordNotes), tuning).firstOrNull()
    }

    val noteName = if (isChordMode) {
        currentChordName ?: "-"
    } else if (selectedString != -1 && selectedFret != -1) {
        (tuning.pitchClasses[selectedString] + selectedFret).toNoteName()
    } else {
        "-"
    }

    val fretboardNotes = if (isChordMode) {
        chordNotes
    } else if (selectedString != -1 && selectedFret != -1) {
        List(6) { index ->
            if (index == selectedString) ChordNote.Fretted(selectedFret) else ChordNote.Muted
        }
    } else {
        List(6) { ChordNote.Muted }
    }

    fun loadChord(name: String) {
        val parsed = parseChordName(name)
        val voicings = if (parsed != null) commonVoicings(name, tuning) else emptyList()
        if (voicings.isNotEmpty()) {
            availableVoicings = voicings
            currentVoicingIndex = 0
            chordNotes = voicings[0].notes
        }
    }

    fun applyVoicing(index: Int) {
        if (index in availableVoicings.indices) {
            currentVoicingIndex = index
            chordNotes = availableVoicings[index].notes
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background
                    ),
                    center = Offset(0.5f, 0.35f),
                    radius = 1.1f
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            RangeSlider(
                value = startFret..endFret,
                onValueChange = { range ->
                    val newStart = range.start.coerceIn(0f, FretCount - 1f)
                    val newEnd = range.endInclusive.coerceIn(newStart + 1f, FretCount.toFloat())
                    startFret = newStart
                    endFret = newEnd
                },
                valueRange = 0f..FretCount.toFloat(),
                steps = 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                track = { sliderState ->
                    SliderDefaults.Track(
                        rangeSliderState = sliderState,
                        modifier = Modifier.height(2.dp),
                        thumbTrackGapSize = 0.dp,
                        colors = SliderDefaults.colors(
                            inactiveTrackColor = MaterialTheme.colorScheme.outline,
                            activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                },
                startThumb = {
                    SliderHandle()
                },
                endThumb = {
                    SliderHandle()
                }
            )

            GuitarFretboard(
                modifier = Modifier.fillMaxWidth(),
                tuning = tuning,
                startFret = startFret,
                endFret = endFret,
                chordNotes = fretboardNotes,
                vibratingNotes = vibratingNotes,
                vibrationTrigger = vibrationTrigger,
                onFretTapped = { stringIndex, fretIndex ->
                    if (isChordMode) {
                        val current = chordNotes[stringIndex]
                        chordNotes = chordNotes.mapIndexed<ChordNote, ChordNote> { i, note ->
                            if (i != stringIndex) note
                            else if (current is ChordNote.Fretted && current.fret == fretIndex) ChordNote.Muted
                            else if (fretIndex == 0) ChordNote.Open
                            else ChordNote.Fretted(fretIndex)
                        }
                    } else {
                        selectedString = stringIndex
                        selectedFret = fretIndex
                    }
                },
                onPlayNote = { stringIndex, fretIndex ->
                    audioPlayer.playString(stringIndex, fretIndex)
                    vibratingNotes = listOf(stringIndex to fretIndex)
                    vibrationTrigger++
                }
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = noteName,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!isChordMode && selectedString != -1 && selectedFret != -1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "第 ${selectedString + 1} 弦 · 第 ${selectedFret} 品",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CardText.copy(alpha = 0.7f)
                    )
                }
            }
        }

        ChordControlPanel(
            isChordMode = isChordMode,
            onModeChange = { enabled ->
                isChordMode = enabled
                if (!enabled) {
                    chordNotes = List(6) { ChordNote.Muted }
                    availableVoicings = emptyList()
                    currentVoicingIndex = 0
                }
            },
            onChordSelected = ::loadChord,
            onPlayChord = {
                audioPlayer.playChord(chordNotes)
                vibratingNotes = chordNotes.mapIndexedNotNull { index, note ->
                    note.fretOrNull()?.let { index to it }
                }
                vibrationTrigger++
            },
            currentChordName = currentChordName,
            availableVoicings = availableVoicings.size,
            currentVoicingIndex = currentVoicingIndex,
            onPrevVoicing = { applyVoicing(currentVoicingIndex - 1) },
            onNextVoicing = { applyVoicing(currentVoicingIndex + 1) }
        )

        val tuningNames = tuning.pitchClasses.map { it.toNoteName() }.reversed().joinToString("-")
        Text(
            text = "${tuning.name} $tuningNames",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SliderHandle() {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(18.dp)
            .background(Color(0xFFFFF8E1))
    )
}

@Composable
fun GuitarFretboard(
    modifier: Modifier = Modifier,
    tuning: Tuning = Tuning.Standard,
    startFret: Float = 0f,
    endFret: Float = FretCount.toFloat(),
    chordNotes: List<ChordNote> = List(6) { ChordNote.Muted },
    showNoteNames: Boolean = true,
    highlightColorOverride: Color? = null,
    vibratingNotes: List<Pair<Int, Int>> = emptyList(),
    vibrationTrigger: Int = 0,
    onFretTapped: (stringIndex: Int, fretIndex: Int) -> Unit,
    onPlayNote: (stringIndex: Int, fretIndex: Int) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val vibrationOffset = remember { Animatable(0f) }
    val ebonyTexture = rememberEbonyTexture()
    var animatingNotes by remember { mutableStateOf(listOf<Pair<Int, Int>>()) }
    val currentVibratingNotes by rememberUpdatedState(vibratingNotes)
    val currentOnFretTapped by rememberUpdatedState(onFretTapped)
    val currentOnPlayNote by rememberUpdatedState(onPlayNote)

    LaunchedEffect(vibrationTrigger) {
        if (vibrationTrigger > 0 && currentVibratingNotes.isNotEmpty()) {
            animatingNotes = currentVibratingNotes
            val durationMs = 600f
            val startTime = System.nanoTime()
            val maxAmplitude = 24f
            val frequency = 0.08f

            while (true) {
                val now = System.nanoTime()
                val elapsedMs = (now - startTime) / 1_000_000f
                if (elapsedMs >= durationMs) break

                val progress = elapsedMs / durationMs
                val decay = 1f - progress
                val amplitude = maxAmplitude * decay
                val value = sin(elapsedMs * frequency * 2f * PI.toFloat()) * amplitude
                vibrationOffset.snapTo(value)
                delay(16)
            }
            vibrationOffset.snapTo(0f)
            animatingNotes = emptyList()
        }
    }

    val highlightColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp)
            .pointerInput(startFret, endFret) {
                detectTapGestures { offset ->
                    val canvasWidth = size.width.toFloat()
                    val canvasHeight = size.height.toFloat()

                    val (fretPositions, leftEdgeX) = calculateFretPositions(
                        canvasWidth = canvasWidth,
                        fretCount = FretCount,
                        startFret = startFret,
                        endFret = endFret
                    )
                    val stringYPositions = calculateStringYPositions(canvasHeight)
                    val stringSpacing = (canvasHeight - 48f) / 5f
                    val hitHalfGap = stringSpacing / 2.2f

                    val hitFret = findHitFret(offset.x, fretPositions)
                    val hitString = findHitString(
                        x = offset.x,
                        y = offset.y,
                        leftEdgeX = leftEdgeX,
                        nutX = fretPositions[0],
                        stringYPositions = stringYPositions,
                        hitHalfGap = hitHalfGap
                    )

                    if (hitFret != -1 && hitString != -1 && hitFret.toFloat() in startFret..endFret) {
                        currentOnPlayNote(hitString, hitFret)
                        currentOnFretTapped(hitString, hitFret)
                    }
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val (fretPositions, leftEdgeX, rightEdgeX) = calculateFretPositions(
            canvasWidth = canvasWidth,
            fretCount = FretCount,
            startFret = startFret,
            endFret = endFret
        )
        val stringYPositions = calculateStringYPositions(canvasHeight)

        val nutX = fretPositions[0]
        // 琴枕随滑动从左侧渐出/渐入，过渡范围更大更柔和
        val showNut = nutX >= leftEdgeX - 48f
        val nutAlpha = ((nutX - leftEdgeX + 48f) / 48f).coerceIn(0f, 1f)

        drawFretboardBackground(
            width = canvasWidth,
            height = canvasHeight,
            texture = ebonyTexture
        )
        if (showNut) {
            drawNut(nutX, canvasHeight, alpha = nutAlpha)
        }
        drawFrets(
            fretPositions = fretPositions,
            startFret = startFret,
            endFret = endFret,
            canvasHeight = canvasHeight
        )
        drawFretBoundaryLabels(
            startFret = startFret,
            endFret = endFret,
            leftEdgeX = leftEdgeX,
            rightEdgeX = rightEdgeX
        )
        drawInlays(
            fretPositions = fretPositions,
            startFret = startFret,
            endFret = endFret,
            stringYPositions = stringYPositions
        )
        drawStrings(
            stringYPositions = stringYPositions,
            fretPositions = fretPositions,
            leftEdgeX = leftEdgeX,
            rightEdgeX = rightEdgeX,
            animatingNotes = animatingNotes,
            vibrationOffset = vibrationOffset.value
        )
        drawChordHighlights(
            chordNotes = chordNotes,
            tuning = tuning,
            stringYPositions = stringYPositions,
            fretPositions = fretPositions,
            leftEdgeX = leftEdgeX,
            rightEdgeX = rightEdgeX,
            startFret = startFret,
            endFret = endFret,
            highlightColor = highlightColor,
            highlightColorOverride = highlightColorOverride,
            showNoteNames = showNoteNames,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight
        )
        drawMutedIndicators(
            chordNotes = chordNotes,
            stringYPositions = stringYPositions,
            leftEdgeX = leftEdgeX
        )
    }
}

private fun calculateFretPositions(
    canvasWidth: Float,
    fretCount: Int = FretCount,
    startFret: Float = 0f,
    endFret: Float = fretCount.toFloat()
): Triple<List<Float>, Float, Float> {
    val leftPadding = 16f
    val rightPadding = canvasWidth - 16f

    // 对数品格位置（归一化）
    val normPositions = List(fretCount + 1) { fret ->
        1f - 2f.pow(-fret / 12f)
    }

    val leftBoundaryFret = startFret
    val rightBoundaryFret = endFret

    val availableStart = leftPadding
    val availableEnd = rightPadding
    val startNorm = 1f - 2f.pow(-leftBoundaryFret / 12f)
    val endNorm = 1f - 2f.pow(-rightBoundaryFret / 12f)
    val normRange = endNorm - startNorm

    val positions = normPositions.map { norm ->
        availableStart + (norm - startNorm) / normRange * (availableEnd - availableStart)
    }

    return Triple(positions, availableStart, availableEnd)
}

private fun calculateStringYPositions(canvasHeight: Float): List<Float> {
    val radius = 44f
    val top = radius
    val bottom = canvasHeight - radius
    val spacing = (bottom - top) / 5
    return List(6) { index -> top + index * spacing }
}

private fun findHitFret(x: Float, fretPositions: List<Float>): Int {
    val nutX = fretPositions[0]
    // 弦枕及左侧区域为空弦音（0 品）
    if (x < nutX) return 0
    // 弦枕右侧第一格为 1 品，以此类推
    for (i in 0 until fretPositions.size - 1) {
        if (x >= fretPositions[i] && x < fretPositions[i + 1]) {
            return i + 1
        }
    }
    return -1
}

private fun findHitString(
    x: Float,
    y: Float,
    leftEdgeX: Float,
    nutX: Float,
    stringYPositions: List<Float>,
    hitHalfGap: Float
): Int {
    var closestIndex = -1
    var closestDistance = Float.MAX_VALUE

    for (i in stringYPositions.indices) {
        val expectedY = stringYAt(i, x, leftEdgeX, nutX, stringYPositions)
        val distance = kotlin.math.abs(y - expectedY)
        if (distance < closestDistance) {
            closestDistance = distance
            closestIndex = i
        }
    }

    return if (closestDistance <= hitHalfGap) closestIndex else -1
}

private fun stringYAt(
    stringIndex: Int,
    x: Float,
    leftEdgeX: Float,
    nutX: Float,
    stringYPositions: List<Float>
): Float {
    return stringYPositions[stringIndex]
}

private fun DrawScope.drawFretboardBackground(
    width: Float,
    height: Float,
    texture: ImageBitmap?
) {
    if (texture != null) {
        drawImage(
            image = texture,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width.toInt(), height.toInt()),
            filterQuality = FilterQuality.Low
        )
    } else {
        drawRect(
            color = Color(0xFF12100E),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )
    }

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.2f)
            ),
            center = Offset(width / 2, height / 2),
            radius = width.coerceAtLeast(height) * 0.8f
        ),
        topLeft = Offset.Zero,
        size = Size(width, height)
    )
}

private fun DrawScope.drawFretBoundaryLabels(
    startFret: Float,
    endFret: Float,
    leftEdgeX: Float,
    rightEdgeX: Float
) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 28f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
    }

    val labelY = 26f
    drawContext.canvas.nativeCanvas.drawText(
        startFret.toInt().toString(),
        leftEdgeX,
        labelY,
        paint
    )
    drawContext.canvas.nativeCanvas.drawText(
        endFret.toInt().toString(),
        rightEdgeX,
        labelY,
        paint
    )
}

private fun DrawScope.drawFrets(
    fretPositions: List<Float>,
    startFret: Float,
    endFret: Float,
    canvasHeight: Float
) {
    // 可见范围内的品格线：包含部分露出的左右边界
    val startIndex = if (startFret <= 0f) 1 else (startFret - 1f).toInt().coerceAtLeast(1)
    val endIndex = (endFret + 1f).toInt().coerceAtMost(FretCount)
    for (index in startIndex..endIndex) {
        val x = fretPositions[index]
        drawLine(
            color = Color(0xFFB0BEC5),
            start = Offset(x, 0f),
            end = Offset(x, canvasHeight),
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawNut(nutX: Float, canvasHeight: Float, alpha: Float = 1f) {
    // 白色弦枕
    drawLine(
        color = Color(0xFFFFF8E1).copy(alpha = alpha),
        start = Offset(nutX, 0f),
        end = Offset(nutX, canvasHeight),
        strokeWidth = 10f
    )
    // 弦枕高光
    drawLine(
        color = Color.White.copy(alpha = 0.6f * alpha),
        start = Offset(nutX - 1f, 0f),
        end = Offset(nutX - 1f, canvasHeight),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawInlays(
    fretPositions: List<Float>,
    startFret: Float,
    endFret: Float,
    stringYPositions: List<Float>
) {
    val inlayFrets = listOf(3, 5, 7, 9)
    val doubleInlayFrets = listOf(12)

    val centerY = (stringYPositions.first() + stringYPositions.last()) / 2f

    val inlayColor = Color(0xFFDEE0FF).copy(alpha = 0.55f)

    inlayFrets.forEach { fret ->
        if (fret.toFloat() in startFret..endFret) {
            val centerX = (fretPositions[fret - 1] + fretPositions[fret]) / 2f
            drawCircle(
                color = inlayColor,
                radius = 8f,
                center = Offset(centerX, centerY)
            )
        }
    }

    doubleInlayFrets.forEach { fret ->
        if (fret.toFloat() in startFret..endFret) {
            val centerX = (fretPositions[fret - 1] + fretPositions[fret]) / 2f
            val gap = 18f
            drawCircle(
                color = inlayColor,
                radius = 7f,
                center = Offset(centerX, centerY - gap)
            )
            drawCircle(
                color = inlayColor,
                radius = 7f,
                center = Offset(centerX, centerY + gap)
            )
        }
    }
}

private fun DrawScope.drawStrings(
    stringYPositions: List<Float>,
    fretPositions: List<Float>,
    leftEdgeX: Float,
    rightEdgeX: Float,
    animatingNotes: List<Pair<Int, Int>>,
    vibrationOffset: Float
) {
    stringYPositions.forEachIndexed { stringIndex, baseY ->
        // 第 1 弦（最上面）最细，第 6 弦（最下面）最粗，粗细对比更明显
        val strokeWidth = when (stringIndex) {
            0 -> 1.3f
            1 -> 1.8f
            2 -> 2.4f
            3 -> 4.2f
            4 -> 7.8f
            5 -> 9.5f
            else -> 2f
        }

        val stringStartX = leftEdgeX

        // 1-3 弦是钢弦（银色金属光泽），4-6 弦是缠弦（铜棕色螺纹感）
        val stringBrush = when (stringIndex) {
            in 0..2 -> createSteelStringBrush(baseY, strokeWidth)
            else -> createWoundStringBrush(
                startX = stringStartX,
                endX = rightEdgeX,
                baseY = baseY
            )
        }

        val vibratingFret = animatingNotes.firstOrNull { it.first == stringIndex }?.second
        val isVibrating = vibratingFret != null

        if (isVibrating) {
            // 以按下的品格为中心做高斯包络振动
            val tapX = if (vibratingFret == 0) {
                stringStartX
            } else {
                (fretPositions[vibratingFret - 1] + fretPositions[vibratingFret]) / 2f
            }

            val vibrateEndX = rightEdgeX
            val samples = 40
            val sigma = (vibrateEndX - stringStartX) * 0.45f

            val path = Path().apply {
                moveTo(stringStartX, baseY)

                // 左端点固定在弦枕/左边界，从 i=1 开始生成振动点
                for (i in 1..samples) {
                    val progress = i / samples.toFloat()
                    val x = stringStartX + (vibrateEndX - stringStartX) * progress
                    // 高斯包络：按下的品格处振幅最大，向两端衰减
                    val envelope = kotlin.math.exp(-((x - tapX) / sigma).pow(2)).toFloat()
                    // 左端固定：振幅从左端 0  smoothstep 到 1
                    val leftFixedFactor = smoothStep(
                        edge0 = stringStartX,
                        edge1 = stringStartX + (vibrateEndX - stringStartX) * 0.25f,
                        x = x
                    )
                    val y = baseY + envelope * leftFixedFactor * vibrationOffset
                    lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                brush = stringBrush,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        } else {
            // 非振动状态
            val path = Path().apply {
                moveTo(stringStartX, baseY)
                lineTo(rightEdgeX, baseY)
            }

            drawPath(
                path = path,
                brush = stringBrush,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

private fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

private fun createSteelStringBrush(baseY: Float, strokeWidth: Float): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFF37474F),   // 边缘蓝灰
            Color(0xFF90A4AE),   // 浅蓝银
            Color(0xFFECF2FF),   // 冷白高光
            Color(0xFF90A4AE),   // 浅蓝银
            Color(0xFF37474F)    // 边缘蓝灰
        ),
        startY = baseY - strokeWidth * 1.8f,
        endY = baseY + strokeWidth * 1.8f
    )
}

private fun createWoundStringBrush(startX: Float, endX: Float, baseY: Float): Brush {
    return Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFF431D09),
            0.15f to Color(0xFF6A392B),
            0.35f to Color(0xFFBA784D),
            0.5f to Color(0xFFEABCA1),
            0.65f to Color(0xFFB78352),
            0.85f to Color(0xFF6A3D2C),
            1.0f to Color(0xFF5B2D1B)
        ),
        startX = startX,
        endX = startX + 5f,
        tileMode = TileMode.Repeated
    )
}

private fun DrawScope.drawChordHighlights(
    chordNotes: List<ChordNote>,
    tuning: Tuning,
    stringYPositions: List<Float>,
    fretPositions: List<Float>,
    leftEdgeX: Float,
    rightEdgeX: Float,
    startFret: Float,
    endFret: Float,
    highlightColor: Color,
    highlightColorOverride: Color?,
    showNoteNames: Boolean,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val radius = 44f
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 34f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    chordNotes.forEachIndexed { stringIndex, note ->
        val fret = note.fretOrNull() ?: return@forEachIndexed
        val centerY = stringYPositions[stringIndex]

        val inRange = fret.toFloat() in startFret..endFret
        val radius = if (inRange) 44f else 28f
        val alpha = if (inRange) 1f else 0.4f

        val centerX = when {
            fret == 0 -> fretPositions[0]
            inRange -> (fretPositions[fret - 1] + fretPositions[fret]) / 2f
            fret < startFret -> leftEdgeX - 20f
            else -> rightEdgeX + 20f
        }

        val clampedX = centerX.coerceIn(radius, canvasWidth - radius)

        val noteName = (tuning.pitchClasses[stringIndex] + fret).toNoteName()
        val noteColor = highlightColorOverride?.copy(alpha = alpha)
            ?: (NoteColors[noteName] ?: highlightColor).copy(alpha = alpha)

        textPaint.alpha = (255 * alpha).toInt()

        drawCircle(
            color = noteColor,
            radius = radius,
            center = Offset(clampedX, centerY)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.4f * alpha),
            radius = radius,
            center = Offset(clampedX, centerY),
            style = Stroke(width = 2f)
        )

        if (showNoteNames) {
            val textBounds = android.graphics.Rect()
            textPaint.getTextBounds(noteName, 0, noteName.length, textBounds)
            val textOffset = -(textBounds.top + textBounds.bottom) / 2f
            drawContext.canvas.nativeCanvas.drawText(
                noteName,
                clampedX,
                centerY + textOffset,
                textPaint
            )
        }
    }
}

private fun DrawScope.drawMutedIndicators(
    chordNotes: List<ChordNote>,
    stringYPositions: List<Float>,
    leftEdgeX: Float
) {
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 24f
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    chordNotes.forEachIndexed { stringIndex, note ->
        if (note is ChordNote.Muted) {
            val y = stringYPositions[stringIndex]
            drawContext.canvas.nativeCanvas.drawText(
                "×",
                leftEdgeX - 10f,
                y + 8f,
                paint
            )
        }
    }
}
