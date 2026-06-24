package com.example.myapplication.ui.guitar.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.guitar.training.model.Difficulty
import com.example.myapplication.ui.guitar.training.model.TrainingMode
import com.example.myapplication.ui.guitar.training.model.TrainingSettings
import com.example.myapplication.ui.guitar.training.model.difficultyDescription
import com.example.myapplication.ui.theme.CardBackground
import com.example.myapplication.ui.theme.CardText

@Composable
fun TrainingSettingsPanel(
    settings: TrainingSettings,
    onSettingsChange: (TrainingSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "训练设置",
                style = MaterialTheme.typography.titleMedium,
                color = CardText
            )

            SwitchRow(
                label = "允许转位和弦（slash chord）",
                checked = settings.allowChordInversions,
                onCheckedChange = { onSettingsChange(settings.copy(allowChordInversions = it)) }
            )

            Text(
                text = "每日打卡难度",
                style = MaterialTheme.typography.bodyMedium,
                color = CardText
            )
            DifficultySelector(
                selected = settings.dailyCheckInDifficulty,
                onSelect = { onSettingsChange(settings.copy(dailyCheckInDifficulty = it)) }
            )
            Text(
                text = "随机训练时默认使用的难度",
                style = MaterialTheme.typography.bodySmall,
                color = CardText.copy(alpha = 0.6f)
            )

            Text(
                text = "各模式难度",
                style = MaterialTheme.typography.bodyMedium,
                color = CardText
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainingMode.entries.forEach { mode ->
                    val difficulty = settings.difficultyFor(mode)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CardText
                            )
                            DifficultySelector(
                                selected = difficulty,
                                onSelect = {
                                    onSettingsChange(
                                        settings.copy(
                                            modeDifficulties = settings.modeDifficulties.toMutableMap().apply { put(mode, it) }
                                        )
                                    )
                                }
                            )
                        }
                        Text(
                            text = difficultyDescription(mode, difficulty),
                            style = MaterialTheme.typography.bodySmall,
                            color = CardText.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Text(
                text = "每日打卡题数：${settings.dailyQuestionCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = CardText
            )
            Slider(
                value = settings.dailyQuestionCount.toFloat(),
                onValueChange = { onSettingsChange(settings.copy(dailyQuestionCount = it.toInt())) },
                valueRange = 10f..100f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
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
                Difficulty.Easy -> Color(0xFF43A047)
                Difficulty.Medium -> Color(0xFF1E88E5)
                Difficulty.Hard -> Color(0xFFD32F2F)
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
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = checked,
                onClick = { onCheckedChange(!checked) },
                role = Role.Switch
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = CardText)
        Switch(checked = checked, onCheckedChange = null)
    }
}
