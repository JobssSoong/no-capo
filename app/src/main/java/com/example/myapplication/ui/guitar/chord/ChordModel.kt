package com.example.myapplication.ui.guitar.chord

import androidx.compose.ui.graphics.Color

val NoteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

// 标准调弦：从第 1 弦（高音 E）到第 6 弦（低音 E）
// 对应 pitch class 索引 0..5

val NoteColors = mapOf(
    "C" to Color(0xFFE53935),
    "C#" to Color(0xFFFF5722),
    "D" to Color(0xFFFFB300),
    "D#" to Color(0xFF7CB342),
    "E" to Color(0xFF43A047),
    "F" to Color(0xFF00897B),
    "F#" to Color(0xFF00ACC1),
    "G" to Color(0xFF1E88E5),
    "G#" to Color(0xFF3949AB),
    "A" to Color(0xFF8E24AA),
    "A#" to Color(0xFFD81B60),
    "B" to Color(0xFF546E7A)
)

sealed class ChordNote {
    object Muted : ChordNote()
    object Open : ChordNote()
    data class Fretted(val fret: Int) : ChordNote()

    fun fretOrNull(): Int? = when (this) {
        is Muted -> null
        is Open -> 0
        is Fretted -> fret
    }
}

data class ChordVoicing(
    val notes: List<ChordNote>
) {
    init {
        require(notes.size == 6) { "Guitar voicing must contain exactly 6 string entries" }
    }

    companion object {
        fun muted(): ChordVoicing = ChordVoicing(List(6) { ChordNote.Muted })
    }
}

enum class ChordType(
    val intervals: Set<Int>,
    val suffix: String
) {
    MAJOR(setOf(0, 4, 7), ""),
    MINOR(setOf(0, 3, 7), "m"),
    DIMINISHED(setOf(0, 3, 6), "dim"),
    AUGMENTED(setOf(0, 4, 8), "aug"),
    DOMINANT7(setOf(0, 4, 7, 10), "7"),
    MAJOR7(setOf(0, 4, 7, 11), "maj7"),
    MINOR7(setOf(0, 3, 7, 10), "m7"),
    HALF_DIMINISHED(setOf(0, 3, 6, 10), "m7b5"),
    DIMINISHED7(setOf(0, 3, 6, 9), "dim7"),
    SUS2(setOf(0, 2, 7), "sus2"),
    SUS4(setOf(0, 5, 7), "sus4"),
    ADD9(setOf(0, 2, 4, 7), "add9"),
    SIX(setOf(0, 4, 7, 9), "6"),
    MINOR6(setOf(0, 3, 7, 9), "m6"),
    NINE(setOf(0, 2, 4, 7, 10), "9"),
    MAJOR9(setOf(0, 2, 4, 7, 11), "maj9"),
    MINOR9(setOf(0, 2, 3, 7, 10), "m9"),
    DOMINANT7_SHARP9(setOf(0, 3, 4, 7, 10), "7#9"),
    MAJOR7_SHARP9(setOf(0, 3, 4, 7, 11), "maj7#9");

    companion object {
        private val suffixMap = entries.associateBy { it.suffix }

        fun fromSuffix(suffix: String): ChordType? = suffixMap[suffix]
    }
}

fun String.toPitchClass(): Int? {
    val normalized = when (this) {
        "Db" -> "C#"
        "Eb" -> "D#"
        "Gb" -> "F#"
        "Ab" -> "G#"
        "Bb" -> "A#"
        else -> this
    }
    return NoteNames.indexOf(normalized).takeIf { it >= 0 }
}

fun Int.toNoteName(): String = NoteNames[this % 12]
