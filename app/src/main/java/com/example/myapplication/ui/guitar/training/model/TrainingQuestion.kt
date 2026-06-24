package com.example.myapplication.ui.guitar.training.model

import com.example.myapplication.ui.guitar.chord.ChordNote
import com.example.myapplication.ui.guitar.chord.ChordType
import com.example.myapplication.ui.guitar.chord.ChordVoicing
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.chord.toPitchClass
import com.example.myapplication.ui.guitar.training.engine.intervalName

sealed class UserAnswer {
    data class Option(val label: String) : UserAnswer()
    data class Fretboard(val notes: List<ChordNote>) : UserAnswer()
}

sealed class TrainingQuestion(
    open val mode: TrainingMode,
    open val tuning: Tuning
) {
    abstract fun check(answer: UserAnswer): Boolean
}

data class NoteRecognitionQuestion(
    val stringIndex: Int,
    val fret: Int,
    val pitchClass: Int,
    override val tuning: Tuning
) : TrainingQuestion(TrainingMode.NoteRecognition, tuning) {
    override fun check(answer: UserAnswer): Boolean {
        return answer is UserAnswer.Option && answer.label.toPitchClass() == pitchClass
    }
}

data class FindNoteQuestion(
    val pitchClass: Int,
    val stringIndex: Int,
    val correctFret: Int,
    override val tuning: Tuning
) : TrainingQuestion(TrainingMode.FindNote, tuning) {
    override fun check(answer: UserAnswer): Boolean {
        val notes = (answer as? UserAnswer.Fretboard)?.notes ?: return false
        val selected = notes
            .mapIndexedNotNull { index, note ->
                note.fretOrNull()?.let { index to it }
            }
            .filter { it.first == stringIndex }
        return selected.any { (selectedString, fret) ->
            (tuning.pitchClasses[selectedString] + fret) % 12 == pitchClass
        }
    }
}

data class ChordRecognitionQuestion(
    val voicing: ChordVoicing,
    override val tuning: Tuning,
    val correctName: String,
    val options: List<String>
) : TrainingQuestion(TrainingMode.ChordRecognition, tuning) {
    override fun check(answer: UserAnswer): Boolean {
        return answer is UserAnswer.Option && answer.label == correctName
    }
}

data class MarkChordQuestion(
    val chordName: String,
    val rootPitchClass: Int,
    val chordType: ChordType,
    val positionStart: Int,
    val positionEnd: Int,
    override val tuning: Tuning,
    val requireRootInBass: Boolean = true
) : TrainingQuestion(TrainingMode.MarkChord, tuning) {
    override fun check(answer: UserAnswer): Boolean {
        val notes = (answer as? UserAnswer.Fretboard)?.notes ?: return false
        val sounded = notes.mapIndexedNotNull { index, note ->
            note.fretOrNull()?.let { index to it }
        }
        if (sounded.isEmpty()) return false
        if (sounded.any { it.second > 0 && (it.second < positionStart || it.second > positionEnd) }) return false

        val requiredPcs = chordType.intervals.map { (rootPitchClass + it) % 12 }.toSet()
        val actualPcs = sounded.map { (stringIndex, fret) ->
            (tuning.pitchClasses[stringIndex] + fret) % 12
        }.toSet()
        if (actualPcs != requiredPcs) return false

        if (requireRootInBass) {
            val bass = sounded.maxBy { it.first }
            val bassPc = (tuning.pitchClasses[bass.first] + bass.second) % 12
            if (bassPc != rootPitchClass) return false
        }
        return true
    }
}

data class IntervalRecognitionQuestion(
    val lowerString: Int,
    val lowerFret: Int,
    val upperString: Int,
    val upperFret: Int,
    val intervalSemitones: Int,
    val options: List<String>,
    override val tuning: Tuning
) : TrainingQuestion(TrainingMode.IntervalRecognition, tuning) {
    override fun check(answer: UserAnswer): Boolean {
        return answer is UserAnswer.Option && answer.label == intervalName(intervalSemitones)
    }
}

data class EarTrainingQuestion(
    val pitchClass: Int,
    val referenceString: Int,
    val referenceFret: Int,
    override val tuning: Tuning
) : TrainingQuestion(TrainingMode.EarTraining, tuning) {
    override fun check(answer: UserAnswer): Boolean {
        val notes = (answer as? UserAnswer.Fretboard)?.notes ?: return false
        val sounded = notes.mapIndexedNotNull { index, note ->
            note.fretOrNull()?.let { index to it }
        }
        return sounded.any { (stringIndex, fret) ->
            (tuning.pitchClasses[stringIndex] + fret) % 12 == pitchClass
        }
    }
}

data class ScaleFindNotesQuestion(
    val keyRoot: Int,
    val scaleType: ScaleType,
    val startFret: Int,
    val endFret: Int,
    val targetPositions: Set<Pair<Int, Int>>,
    override val tuning: Tuning
) : TrainingQuestion(TrainingMode.ScaleFindNotes, tuning) {
    override fun check(answer: UserAnswer): Boolean {
        val notes = (answer as? UserAnswer.Fretboard)?.notes ?: return false
        val selected = notes.mapIndexedNotNull { index, note ->
            note.fretOrNull()?.let { index to it }
        }.toSet()
        return selected == targetPositions
    }
}

data class ChordProgressionQuestion(
    val keyRoot: Int,
    val progression: List<Pair<String, ChordType>>,
    val chordIndex: Int,
    val chordName: String,
    val options: List<String>,
    override val tuning: Tuning
) : TrainingQuestion(TrainingMode.ChordProgression, tuning) {
    override fun check(answer: UserAnswer): Boolean {
        return answer is UserAnswer.Option && answer.label == chordName
    }
}

enum class ScaleType(
    val intervals: List<Int>,
    val suffix: String
) {
    Major(listOf(0, 2, 4, 5, 7, 9, 11), "Major"),
    NaturalMinor(listOf(0, 2, 3, 5, 7, 8, 10), "Minor"),
    PentatonicMajor(listOf(0, 2, 4, 7, 9), "Pentatonic"),
    Blues(listOf(0, 3, 5, 6, 7, 10), "Blues")
}
