package com.example.myapplication.ui.guitar

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.guitar.chord.CustomRoots
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.chord.toNoteName
import com.example.myapplication.ui.guitar.chord.toPitchClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tuning: Tuning,
    onTuningChange: (Tuning) -> Unit,
    modifier: Modifier = Modifier
) {
    var isCustomTuning by remember { mutableStateOf(false) }
    var customTuningPcs by remember {
        mutableStateOf(List(Tuning.STRING_COUNT) { index -> tuning.pitchClasses[index] })
    }
    var expandedString by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF121212),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB0B0B0)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "调弦",
                style = MaterialTheme.typography.titleMedium
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
                            isCustomTuning = false
                            expandedString = null
                            onTuningChange(preset)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(preset.name)
                    }
                }
                OutlinedButton(
                    onClick = { isCustomTuning = !isCustomTuning },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isCustomTuning) MaterialTheme.colorScheme.primaryContainer
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isCustomTuning) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "点击弦选择空弦音（1 弦到 6 弦）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        customTuningPcs.forEachIndexed { stringIndex, pc ->
                            val selected = expandedString == stringIndex
                            OutlinedButton(
                                onClick = {
                                    expandedString = if (selected) null else stringIndex
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
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

                    expandedString?.let { activeString ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomRoots.forEach { root ->
                                val rootPc = root.toPitchClass() ?: return@forEach
                                val selected = customTuningPcs[activeString] == rootPc
                                OutlinedButton(
                                    onClick = {
                                        customTuningPcs = customTuningPcs.mapIndexed { i, value ->
                                            if (i == activeString) rootPc else value
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
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
                            val tuningName = customTuningPcs.map { it.toNoteName() }.joinToString("-")
                            onTuningChange(Tuning.fromPitchClasses(tuningName, customTuningPcs))
                            expandedString = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("应用自定义调弦")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
