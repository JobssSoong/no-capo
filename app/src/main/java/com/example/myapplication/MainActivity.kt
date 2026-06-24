package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.guitar.GuitarFretboardScreen
import com.example.myapplication.ui.guitar.SettingsScreen
import com.example.myapplication.ui.guitar.audio.AudioSettings
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var showSettings by remember { mutableStateOf(false) }
    var tuning by remember { mutableStateOf(Tuning.Standard) }
    var audioSettings by remember { mutableStateOf(AudioSettings()) }
    var revealCenter by remember { mutableStateOf(Offset.Zero) }
    val revealProgress = remember { Animatable(0f) }

    LaunchedEffect(showSettings) {
        revealProgress.animateTo(
            targetValue = if (showSettings) 1f else 0f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        GuitarFretboardScreen(
            modifier = Modifier.fillMaxSize(),
            tuning = tuning,
            audioSettings = audioSettings
        )

        if (revealProgress.value > 0f) {
            val radius = revealProgress.value * 4000f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircularRevealShape(center = revealCenter, radius = radius))
            ) {
                SettingsScreen(
                    tuning = tuning,
                    onTuningChange = { tuning = it }
                )
            }
        }

        IconButton(
            onClick = {
                showSettings = !showSettings
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(56.dp)
                .onGloballyPositioned { coordinates ->
                    revealCenter = coordinates.boundsInParent().center
                }
        ) {
            Icon(
                imageVector = if (showSettings) Icons.Default.Close else Icons.Default.Settings,
                contentDescription = if (showSettings) "关闭设置" else "设置",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private class CircularRevealShape(
    private val center: Offset,
    private val radius: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            addOval(
                Rect(
                    left = center.x - radius,
                    top = center.y - radius,
                    right = center.x + radius,
                    bottom = center.y + radius
                )
            )
        }
        return Outline.Generic(path)
    }
}

@Composable
fun ControlGallery(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var clickCount by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }
    var isChecked by remember { mutableStateOf(false) }
    var isSwitched by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0.5f) }
    var selectedOption by remember { mutableStateOf("选项 A") }
    val options = listOf("选项 A", "选项 B", "选项 C")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Compose 常用控件示例",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = {
                context.startActivity(Intent(context, GuitarActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("打开吉他指板页面")
        }

        DemoSection(title = "1. 文本 Text") {
            Text(
                text = "这是普通文本",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "这是标题样式文本",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        DemoSection(title = "2. 按钮 Button") {
            Button(
                onClick = {
                    clickCount++
                    Toast.makeText(context, "点击了 $clickCount 次", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("点我计数: $clickCount")
            }
        }

        DemoSection(title = "3. 输入框 OutlinedTextField") {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("请输入内容") },
                modifier = Modifier.fillMaxWidth()
            )
            if (text.isNotEmpty()) {
                Text("你输入了: $text")
            }
        }

        DemoSection(title = "4. 开关 Switch") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = isSwitched,
                    onCheckedChange = { isSwitched = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSwitched) "开启" else "关闭")
            }
        }

        DemoSection(title = "5. 复选框 Checkbox") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { isChecked = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isChecked) "已选中" else "未选中")
            }
        }

        DemoSection(title = "6. 单选按钮 RadioButton") {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        }

        DemoSection(title = "7. 滑块 Slider") {
            Column {
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("当前值: ${"%.2f".format(sliderValue)}")
            }
        }

        DemoSection(title = "8. 卡片 Card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "卡片标题",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("这是卡片里的内容，可以包裹任意组合控件。")
                }
            }
        }

        DemoSection(title = "9. 横向排列 Row") {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("左")
                Text("中")
                Text("右")
            }
        }

        DemoSection(title = "10. 样式示例 Styling") {
            // ... 原有内容不变 ...
        }

        DemoSection(title = "11. 自定义绘制 Canvas") {
            // ... 原有内容不变 ...
        }

        DemoSection(title = "12. 自定义绘制 + 点击检测") {
            InteractiveCanvasDemo()
        }
    }
}

@Composable
fun InteractiveCanvasDemo() {
    val context = LocalContext.current
    val density = LocalDensity.current
    var selectedIndex by remember { mutableIntStateOf(-1) }

    val buttonCount = 4
    val buttonWidth = 120.dp
    val buttonHeight = 60.dp
    val spacing = 20.dp
    val startY = 30.dp
    val startX = 20.dp

    val buttonWidthPx = with(density) { buttonWidth.toPx() }
    val buttonHeightPx = with(density) { buttonHeight.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val startXPx = with(density) { startX.toPx() }
    val startYPx = with(density) { startY.toPx() }

    Column {
        Text(
            text = "点击下面的自定义矩形按钮",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            var x = startXPx
                            var clickedIndex = -1
                            for (index in 0 until buttonCount) {
                                val rect = Rect(
                                    offset = Offset(x, startYPx),
                                    size = Size(buttonWidthPx, buttonHeightPx)
                                )
                                if (rect.contains(offset)) {
                                    clickedIndex = index
                                    break
                                }
                                x += buttonWidthPx + spacingPx
                            }

                            if (clickedIndex != -1) {
                                selectedIndex = clickedIndex
                                Toast.makeText(context, "点击了按钮 $clickedIndex", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            ) {
                var x = startXPx
                for (index in 0 until buttonCount) {
                    val isSelected = index == selectedIndex
                    val color = if (isSelected) Color(0xFF6650a4) else Color(0xFFD0BCFF)

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, startYPx),
                        size = Size(buttonWidthPx, buttonHeightPx),
                        cornerRadius = CornerRadius(x = 12f, y = 12f)
                    )

                    x += buttonWidthPx + spacingPx
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = startX, top = startY),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                repeat(buttonCount) { index ->
                    Box(
                        modifier = Modifier.size(buttonWidth, buttonHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "按钮 $index",
                            color = if (index == selectedIndex) Color.White else Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (selectedIndex != -1) {
            Text(
                text = "当前选中：按钮 $selectedIndex",
                modifier = Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun DemoSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun ControlGalleryPreview() {
    MyApplicationTheme {
        ControlGallery()
    }
}
