package com.example.myapplication.ui.guitar.chord

private const val STRING_COUNT = 6

fun parseChordName(name: String): Pair<Int, ChordType>? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return null

    val root = (2 downTo 1).firstNotNullOfOrNull { len ->
        trimmed.take(len).toPitchClass()?.let { it to len }
    } ?: return null

    val suffix = trimmed.drop(root.second)
    val type = ChordType.fromSuffix(suffix) ?: return null
    return root.first to type
}

fun voicingToPitchClasses(voicing: ChordVoicing, tuning: Tuning): Set<Int> {
    val result = mutableSetOf<Int>()
    voicing.notes.forEachIndexed { stringIndex, note ->
        val fret = note.fretOrNull() ?: return@forEachIndexed
        result.add((tuning.pitchClasses[stringIndex] + fret) % 12)
    }
    return result
}

fun voicingToNoteNames(voicing: ChordVoicing, tuning: Tuning): List<String?> {
    return voicing.notes.mapIndexed { stringIndex, note ->
        val fret = note.fretOrNull() ?: return@mapIndexed null
        (tuning.pitchClasses[stringIndex] + fret).toNoteName()
    }
}

fun nameChord(voicing: ChordVoicing, tuning: Tuning): List<String> {
    val notes = voicing.notes
    val sounded = notes.mapIndexedNotNull { index, note ->
        val fret = note.fretOrNull() ?: return@mapIndexedNotNull null
        index to ((tuning.pitchClasses[index] + fret) % 12)
    }

    if (sounded.isEmpty()) return emptyList()
    if (sounded.size == 1) {
        return listOf(sounded.first().second.toNoteName())
    }

    val pitchClasses = sounded.map { it.second }.toSet()
    val bassString = sounded.maxBy { it.first }.first
    val bassNote = sounded.first { it.first == bassString }.second

    val matches = mutableListOf<String>()

    for (rootPc in pitchClasses) {
        val intervals = pitchClasses.map { (it - rootPc + 12) % 12 }.toSet()
        for (type in ChordType.entries) {
            if (type.intervals == intervals) {
                val baseName = rootPc.toNoteName() + type.suffix
                val name = if (rootPc == bassNote) baseName else "$baseName/${bassNote.toNoteName()}"
                matches.add(name)
            }
        }
    }

    return matches.distinct()
}

fun generateVoicings(
    rootPc: Int,
    type: ChordType,
    tuning: Tuning,
    maxFret: Int = 12,
    maxSpan: Int = 4
): List<ChordVoicing> {
    val requiredPcs = type.intervals.map { (rootPc + it) % 12 }.toSet()

    val optionsPerString = (0 until STRING_COUNT).map { stringIndex ->
        val openPc = tuning.pitchClasses[stringIndex]
        val options = mutableListOf<Int?>(null)
        for (fret in 0..maxFret) {
            val pc = (openPc + fret) % 12
            if (pc in requiredPcs) {
                options.add(fret)
            }
        }
        options
    }

    val results = mutableListOf<ChordVoicing>()

    fun backtrack(
        stringIndex: Int,
        current: MutableList<Int?>,
        covered: MutableSet<Int>,
        minFret: Int,
        maxFretUsed: Int
    ) {
        if (results.size >= 64) return

        if (stringIndex == STRING_COUNT) {
            if (covered.containsAll(requiredPcs)) {
                val voicing = ChordVoicing(current.map { fret ->
                    when (fret) {
                        null -> ChordNote.Muted
                        0 -> ChordNote.Open
                        else -> ChordNote.Fretted(fret)
                    }
                })
                results.add(voicing)
            }
            return
        }

        for (fret in optionsPerString[stringIndex]) {
            val newMin = if (fret != null && fret > 0) minOf(minFret, fret) else minFret
            val newMax = if (fret != null && fret > 0) maxOf(maxFretUsed, fret) else maxFretUsed
            val frettedSpan = if (newMax < newMin) 0 else newMax - newMin
            if (frettedSpan > maxSpan) continue

            val pc = if (fret != null) (tuning.pitchClasses[stringIndex] + fret) % 12 else null
            val added = pc != null && covered.add(pc)

            current.add(fret)
            backtrack(stringIndex + 1, current, covered, newMin, newMax)
            current.removeAt(current.lastIndex)
            if (added) covered.remove(pc)
        }
    }

    backtrack(
        stringIndex = 0,
        current = mutableListOf(),
        covered = mutableSetOf(),
        minFret = Int.MAX_VALUE,
        maxFretUsed = Int.MIN_VALUE
    )

    return results
        .sortedWith(
            compareBy(
                { countMuted(it) },
                { maxFret(it) },
                { totalFret(it) },
                { fretSpan(it) },
                { -openStringCount(it, tuning, requiredPcs) }
            )
        )
        .take(20)
}

private fun countMuted(voicing: ChordVoicing): Int =
    voicing.notes.count { it is ChordNote.Muted }

private fun maxFret(voicing: ChordVoicing): Int =
    voicing.notes.mapNotNull { it.fretOrNull() }.maxOrNull() ?: 0

private fun totalFret(voicing: ChordVoicing): Int =
    voicing.notes.mapNotNull { it.fretOrNull() }.sum()

private fun fretSpan(voicing: ChordVoicing): Int {
    val fretted = voicing.notes.mapNotNull { it.fretOrNull() }.filter { it > 0 }
    return if (fretted.isEmpty()) 0 else fretted.max() - fretted.min()
}

private fun openStringCount(
    voicing: ChordVoicing,
    tuning: Tuning,
    requiredPcs: Set<Int>
): Int {
    return voicing.notes.countIndexed { index, note ->
        if (note !is ChordNote.Open) return@countIndexed false
        val pc = tuning.pitchClasses[index] % 12
        pc in requiredPcs
    }
}

private inline fun <T> List<T>.countIndexed(predicate: (index: Int, item: T) -> Boolean): Int {
    var count = 0
    forEachIndexed { index, item -> if (predicate(index, item)) count++ }
    return count
}

fun commonVoicings(name: String, tuning: Tuning): List<ChordVoicing> {
    val parsed = parseChordName(name)

    // 内置常用按法只在标准调弦下直接使用，避免非标准调弦下音高错误
    if (tuning == Tuning.Standard) {
        val lookup = COMMON_VOICINGS[name]
        if (lookup != null) return lookup
    }

    if (parsed != null) return generateVoicings(parsed.first, parsed.second, tuning)
    return emptyList()
}

private val COMMON_VOICINGS = mapOf(
    "C" to listOf(
        ChordVoicing(listOf(
            ChordNote.Open, ChordNote.Fretted(1), ChordNote.Open,
            ChordNote.Fretted(2), ChordNote.Fretted(3), ChordNote.Muted
        ))
    ),
    "G" to listOf(
        ChordVoicing(listOf(
            ChordNote.Fretted(3), ChordNote.Open, ChordNote.Open,
            ChordNote.Open, ChordNote.Fretted(2), ChordNote.Fretted(3)
        )),
        ChordVoicing(listOf(
            ChordNote.Fretted(3), ChordNote.Open, ChordNote.Open,
            ChordNote.Open, ChordNote.Fretted(3), ChordNote.Fretted(3)
        ))
    ),
    "Am" to listOf(
        ChordVoicing(listOf(
            ChordNote.Open, ChordNote.Fretted(1), ChordNote.Fretted(2),
            ChordNote.Fretted(2), ChordNote.Open, ChordNote.Muted
        ))
    ),
    "F" to listOf(
        ChordVoicing(listOf(
            ChordNote.Fretted(1), ChordNote.Fretted(1), ChordNote.Fretted(2),
            ChordNote.Fretted(3), ChordNote.Fretted(3), ChordNote.Fretted(1)
        ))
    ),
    "D" to listOf(
        ChordVoicing(listOf(
            ChordNote.Fretted(2), ChordNote.Fretted(3), ChordNote.Fretted(2),
            ChordNote.Open, ChordNote.Muted, ChordNote.Muted
        ))
    ),
    "Em" to listOf(
        ChordVoicing(listOf(
            ChordNote.Open, ChordNote.Open, ChordNote.Fretted(2),
            ChordNote.Fretted(2), ChordNote.Open, ChordNote.Open
        ))
    ),
    "A" to listOf(
        ChordVoicing(listOf(
            ChordNote.Open, ChordNote.Fretted(2), ChordNote.Fretted(2),
            ChordNote.Fretted(2), ChordNote.Open, ChordNote.Muted
        )),
        ChordVoicing(listOf(
            ChordNote.Open, ChordNote.Fretted(2), ChordNote.Fretted(2),
            ChordNote.Fretted(2), ChordNote.Open, ChordNote.Muted
        ))
    ),
    "E" to listOf(
        ChordVoicing(listOf(
            ChordNote.Open, ChordNote.Open, ChordNote.Fretted(1),
            ChordNote.Fretted(2), ChordNote.Fretted(2), ChordNote.Open
        ))
    ),
    "Bm" to listOf(
        ChordVoicing(listOf(
            ChordNote.Fretted(2), ChordNote.Fretted(3), ChordNote.Fretted(4),
            ChordNote.Fretted(4), ChordNote.Fretted(2), ChordNote.Fretted(2)
        ))
    ),
    "Dm" to listOf(
        ChordVoicing(listOf(
            ChordNote.Fretted(1), ChordNote.Fretted(3), ChordNote.Fretted(2),
            ChordNote.Open, ChordNote.Muted, ChordNote.Muted
        ))
    ),
    "B7" to listOf(
        ChordVoicing(listOf(
            ChordNote.Fretted(2), ChordNote.Open, ChordNote.Fretted(2),
            ChordNote.Fretted(1), ChordNote.Fretted(2), ChordNote.Muted
        ))
    ),
    "Cmaj7" to listOf(
        ChordVoicing(listOf(
            ChordNote.Open, ChordNote.Fretted(0), ChordNote.Open,
            ChordNote.Fretted(2), ChordNote.Fretted(3), ChordNote.Muted
        ))
    )
)
