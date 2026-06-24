package com.example.myapplication.ui.guitar.chord

import kotlin.math.pow

data class Tuning(
    val name: String,
    val pitchClasses: List<Int>,
    val frequencies: List<Double>
) {
    init {
        require(pitchClasses.size == STRING_COUNT) { "Tuning must define exactly $STRING_COUNT strings" }
        require(frequencies.size == STRING_COUNT) { "Tuning must define exactly $STRING_COUNT frequencies" }
    }

    companion object {
        const val STRING_COUNT = 6

        // 标准调弦 E-A-D-G-B-E（A4 = 440Hz）
        val Standard = Tuning(
            name = "标准调弦",
            pitchClasses = listOf(4, 11, 7, 2, 9, 4),   // 1弦到6弦
            frequencies = listOf(329.63, 246.94, 196.00, 146.83, 110.00, 82.41)
        )

        val DropD = Tuning(
            name = "Drop D",
            pitchClasses = listOf(4, 11, 7, 2, 9, 2),   // 6弦降到 D
            frequencies = listOf(329.63, 246.94, 196.00, 146.83, 110.00, 73.42)
        )

        val DropC = Tuning(
            name = "Drop C",
            pitchClasses = listOf(4, 11, 7, 2, 9, 0),   // 6弦降到 C
            frequencies = listOf(329.63, 246.94, 196.00, 146.83, 110.00, 65.41)
        )

        val OpenD = Tuning(
            name = "Open D",
            pitchClasses = listOf(2, 9, 6, 11, 7, 2),   // D-A-D-F#-A-D
            frequencies = listOf(293.66, 220.00, 146.83, 92.50, 110.00, 73.42)
        )

        val OpenG = Tuning(
            name = "Open G",
            pitchClasses = listOf(7, 11, 7, 2, 9, 7),   // D-G-D-G-B-D
            frequencies = listOf(392.00, 246.94, 196.00, 146.83, 110.00, 98.00)
        )

        val Dadgad = Tuning(
            name = "DADGAD",
            pitchClasses = listOf(2, 9, 7, 2, 9, 2),    // D-A-D-G-A-D
            frequencies = listOf(293.66, 220.00, 196.00, 146.83, 110.00, 73.42)
        )

        val OpenC = Tuning(
            name = "Open C",
            pitchClasses = listOf(4, 9, 4, 11, 7, 0),   // C-G-C-G-C-E
            frequencies = listOf(329.63, 196.00, 130.81, 98.00, 82.41, 65.41)
        )

        val BuiltIns = listOf(Standard, DropD, DropC, OpenD, OpenG, Dadgad, OpenC)

        /**
         * 根据 6 个 pitch classes 生成 Tuning。
         * 频率以标准调弦各弦为基准，按 pitch class 差值偏移。
         */
        fun fromPitchClasses(name: String, pitchClasses: List<Int>): Tuning {
            val freqs = pitchClasses.mapIndexed { index, pc ->
                val standardPc = Standard.pitchClasses[index]
                val standardFreq = Standard.frequencies[index]
                standardFreq * 2.0.pow((pc - standardPc) / 12.0)
            }
            return Tuning(name, pitchClasses, freqs)
        }
    }
}
