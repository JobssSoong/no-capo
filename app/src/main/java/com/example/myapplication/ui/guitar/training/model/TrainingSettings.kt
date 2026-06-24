package com.example.myapplication.ui.guitar.training.model

import com.example.myapplication.ui.guitar.chord.ChordType

data class TrainingSettings(
    val includeSharps: Boolean = true,
    val includeFlats: Boolean = false,
    val allowChordInversions: Boolean = false,
    val dailyQuestionCount: Int = 50,
    val fretRange: IntRange = 0..12,
    val dailyCheckInDifficulty: Difficulty = Difficulty.Medium,
    val modeDifficulties: Map<TrainingMode, Difficulty> = TrainingMode.entries.associateWith { Difficulty.Medium }
) {
    init {
        require(dailyQuestionCount in 1..200) { "每日题数需在 1–200 之间" }
    }

    fun difficultyFor(mode: TrainingMode): Difficulty {
        return modeDifficulties[mode] ?: dailyCheckInDifficulty
    }

    fun allowedPitchClasses(): Set<Int> {
        val naturals = setOf(0, 2, 4, 5, 7, 9, 11)
        val accidentals = setOf(1, 3, 6, 8, 10)
        return buildSet {
            addAll(naturals)
            if (includeSharps || includeFlats) addAll(accidentals)
        }
    }

    fun effectiveFretRange(mode: TrainingMode): IntRange {
        return when (difficultyFor(mode)) {
            Difficulty.Easy -> 0..5
            Difficulty.Medium -> 0..12
            Difficulty.Hard -> 0..12
        }
    }

    fun allowedChordTypes(mode: TrainingMode): List<ChordType> {
        return when (difficultyFor(mode)) {
            Difficulty.Easy -> listOf(ChordType.MAJOR, ChordType.MINOR)
            Difficulty.Medium -> listOf(
                ChordType.MAJOR, ChordType.MINOR, ChordType.DOMINANT7,
                ChordType.MAJOR7, ChordType.MINOR7, ChordType.SUS2, ChordType.SUS4
            )
            Difficulty.Hard -> ChordType.entries
        }
    }

    fun allowedScaleTypes(mode: TrainingMode): List<ScaleType> {
        return when (difficultyFor(mode)) {
            Difficulty.Easy -> listOf(ScaleType.PentatonicMajor)
            Difficulty.Medium -> listOf(ScaleType.Major, ScaleType.NaturalMinor, ScaleType.PentatonicMajor)
            Difficulty.Hard -> ScaleType.entries
        }
    }

    fun effectivePitchClasses(mode: TrainingMode): Set<Int> {
        val naturals = setOf(0, 2, 4, 5, 7, 9, 11)
        return when (difficultyFor(mode)) {
            Difficulty.Easy -> naturals
            Difficulty.Medium -> naturals
            Difficulty.Hard -> (0..11).toSet()
        }
    }

    fun showNoteNamesOnFretboard(mode: TrainingMode): Boolean {
        return when (mode) {
            TrainingMode.NoteRecognition -> true
            TrainingMode.FindNote -> difficultyFor(mode) == Difficulty.Easy
            TrainingMode.ChordRecognition -> difficultyFor(mode) == Difficulty.Easy
            TrainingMode.MarkChord -> true
            TrainingMode.IntervalRecognition -> difficultyFor(mode) == Difficulty.Easy
            TrainingMode.EarTraining -> false
            TrainingMode.ScaleFindNotes -> true
            TrainingMode.ChordProgression -> false
        }
    }
}
