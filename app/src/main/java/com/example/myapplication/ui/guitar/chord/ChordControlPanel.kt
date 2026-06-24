package com.example.myapplication.ui.guitar.chord

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val CommonChordNames = listOf(
    "C", "G", "Am", "F", "D", "Em", "A", "E", "Bm", "Dm", "B7", "Cmaj7"
)

internal val CustomRoots = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

private val CustomTypes = ChordType.entries

@Composable
fun ChordControlPanel(
    isChordMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    currentChordName: String?,
    onChordSelected: (String) -> Unit,
    onPlayChord: () -> Unit,
    availableVoicings: Int,
    currentVoicingIndex: Int,
    onPrevVoicing: () -> Unit,
    onNextVoicing: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customRootIndex by remember { mutableIntStateOf(0) }
    var customTypeIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "和弦模式",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = isChordMode,
                onCheckedChange = onModeChange
            )
        }

        if (isChordMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentChordName ?: "-",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = onPlayChord,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("▶ 播放")
                }
            }

            Text(
                text = "常用和弦",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommonChordNames.forEach { name ->
                    OutlinedButton(
                        onClick = { onChordSelected(name) }
                    ) {
                        Text(name)
                    }
                }
            }

            Text(
                text = "自定义",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomRoots.forEachIndexed { index, root ->
                    val selected = index == customRootIndex
                    OutlinedButton(
                        onClick = { customRootIndex = index },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(root)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomTypes.forEachIndexed { index, type ->
                    val selected = index == customTypeIndex
                    OutlinedButton(
                        onClick = { customTypeIndex = index },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(type.suffix.ifEmpty { "maj" })
                    }
                }
            }
            Button(
                onClick = {
                    val root = CustomRoots[customRootIndex]
                    val suffix = CustomTypes[customTypeIndex].suffix
                    onChordSelected("$root$suffix")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("加载自定义和弦")
            }

            if (availableVoicings > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onPrevVoicing,
                        enabled = currentVoicingIndex > 0
                    ) {
                        Text("上")
                    }
                    Text(
                        text = "${currentVoicingIndex + 1} / $availableVoicings",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onNextVoicing,
                        enabled = currentVoicingIndex < availableVoicings - 1
                    ) {
                        Text("下")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
