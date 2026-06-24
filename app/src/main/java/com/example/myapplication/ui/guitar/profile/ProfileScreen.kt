package com.example.myapplication.ui.guitar.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.guitar.audio.AudioSettings
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.training.model.TrainingSettings
import com.example.myapplication.ui.theme.CardText

@Composable
fun ProfileScreen(
    tuning: Tuning,
    onTuningChange: (Tuning) -> Unit,
    audioSettings: AudioSettings,
    onAudioSettingsChange: (AudioSettings) -> Unit,
    trainingSettings: TrainingSettings,
    onTrainingSettingsChange: (TrainingSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val background = MaterialTheme.colorScheme.background
    val primaryTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(primaryTint, background),
                    center = Offset(
                        x = 0.5f,
                        y = 0.22f
                    ),
                    radius = 1.2f
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "我的",
            style = MaterialTheme.typography.headlineSmall,
            color = CardText
        )

        TrainingSettingsPanel(
            settings = trainingSettings,
            onSettingsChange = onTrainingSettingsChange
        )

        TuningSettingsPanel(
            tuning = tuning,
            onTuningChange = onTuningChange
        )
    }
}
