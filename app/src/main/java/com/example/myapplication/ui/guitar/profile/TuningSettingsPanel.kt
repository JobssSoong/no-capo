package com.example.myapplication.ui.guitar.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.guitar.chord.CustomRoots
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.chord.toNoteName
import com.example.myapplication.ui.guitar.chord.toPitchClass
import com.example.myapplication.ui.theme.CardBackground
import com.example.myapplication.ui.theme.CardText

@Composable
fun TuningSettingsPanel(
    tuning: Tuning,
    onTuningChange: (Tuning) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCustom by remember { mutableStateOf(false) }
    var customPcs by remember { mutableStateOf(tuning.pitchClasses) }
    var activeString by remember { mutableStateOf<Int?>(null) }

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
                text = "调弦设置",
                style = MaterialTheme.typography.titleMedium,
                color = CardText
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Tuning.BuiltIns.forEach { preset ->
                    val selected = preset.pitchClasses == tuning.pitchClasses
                    OutlinedButton(
                        onClick = {
                            isCustom = false
                            activeString = null
                            onTuningChange(preset)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(preset.name)
                    }
                }
                OutlinedButton(
                    onClick = { isCustom = !isCustom },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isCustom) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text("自定义")
                }
            }

            val tuningNames = tuning.pitchClasses.map { it.toNoteName() }.reversed().joinToString("-")
            Text(
                text = "当前：${tuning.name} $tuningNames",
                style = MaterialTheme.typography.bodyMedium,
                color = CardText
            )

            if (isCustom) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "点击弦选择空弦音（1 弦到 6 弦）",
                        style = MaterialTheme.typography.bodySmall,
                        color = CardText.copy(alpha = 0.7f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        customPcs.forEachIndexed { stringIndex, pc ->
                            val selected = activeString == stringIndex
                            OutlinedButton(
                                onClick = {
                                    activeString = if (selected) null else stringIndex
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    "${stringIndex + 1}弦\n${pc.toNoteName()}",
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    activeString?.let { active ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomRoots.forEach { root ->
                                val rootPc = root.toPitchClass() ?: return@forEach
                                val selected = customPcs[active] == rootPc
                                OutlinedButton(
                                    onClick = {
                                        customPcs = customPcs.mapIndexed { i, value ->
                                            if (i == active) rootPc else value
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text(root)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val name = customPcs.map { it.toNoteName() }.joinToString("-")
                            onTuningChange(Tuning.fromPitchClasses(name, customPcs))
                            activeString = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("应用自定义调弦")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
