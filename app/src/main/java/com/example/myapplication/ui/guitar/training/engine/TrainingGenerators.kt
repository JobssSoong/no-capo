package com.example.myapplication.ui.guitar.training.engine

import com.example.myapplication.ui.guitar.chord.ChordNote
import com.example.myapplication.ui.guitar.chord.ChordType
import com.example.myapplication.ui.guitar.chord.ChordVoicing
import com.example.myapplication.ui.guitar.chord.Tuning
import com.example.myapplication.ui.guitar.chord.generateVoicings
import com.example.myapplication.ui.guitar.chord.nameChord
import com.example.myapplication.ui.guitar.chord.toNoteName
import com.example.myapplication.ui.guitar.training.model.ChordProgressionQuestion
import com.example.myapplication.ui.guitar.training.model.ChordRecognitionQuestion
import com.example.myapplication.ui.guitar.training.model.Difficulty
import com.example.myapplication.ui.guitar.training.model.EarTrainingQuestion
import com.example.myapplication.ui.guitar.training.model.FindNoteQuestion
import com.example.myapplication.ui.guitar.training.model.IntervalRecognitionQuestion
import com.example.myapplication.ui.guitar.training.model.MarkChordQuestion
import com.example.myapplication.ui.guitar.training.model.NoteRecognitionQuestion
import com.example.myapplication.ui.guitar.training.model.ScaleFindNotesQuestion
import com.example.myapplication.ui.guitar.training.model.ScaleType
import com.example.myapplication.ui.guitar.training.model.TrainingMode
import com.example.myapplication.ui.guitar.training.model.TrainingQuestion
import com.example.myapplication.ui.guitar.training.model.TrainingSettings
import kotlin.random.Random

private const val MAX_GENERATION_ATTEMPTS = 200

fun intervalName(semitones: Int): String {
    val normalized = semitones % 12
    return when (normalized) {
        0 -> "纯一度"
        1 -> "小二度"
        2 -> "大二度"
        3 -> "小三度"
        4 -> "大三度"
        5 -> "纯四度"
        6 -> "三全音"
        7 -> "纯五度"
        8 -> "小六度"
        9 -> "大六度"
        10 -> "小七度"
        11 -> "大七度"
        else -> "纯八度"
    }
}

fun Int.toDisplayName(settings: TrainingSettings): String {
    val sharpName = toNoteName()
    val flatName = when (this % 12) {
        1 -> "Db"
        3 -> "Eb"
        6 -> "Gb"
        8 -> "Ab"
        10 -> "Bb"
        else -> sharpName
    }
    return when {
        settings.includeSharps && settings.includeFlats -> "$sharpName/$flatName"
        settings.includeFlats -> flatName
        else -> sharpName
    }
}

fun generateQuestion(
    mode: TrainingMode,
    settings: TrainingSettings,
    tuning: Tuning = Tuning.Standard,
    random: Random = Random.Default
): TrainingQuestion {
    return when (mode) {
        TrainingMode.NoteRecognition -> generateNoteRecognitionQuestion(settings, tuning, random)
        TrainingMode.FindNote -> generateFindNoteQuestion(settings, tuning, random)
        TrainingMode.ChordRecognition -> generateChordRecognitionQuestion(settings, tuning, random)
        TrainingMode.MarkChord -> generateMarkChordQuestion(settings, tuning, random)
        TrainingMode.IntervalRecognition -> generateIntervalRecognitionQuestion(settings, tuning, random)
        TrainingMode.EarTraining -> generateEarTrainingQuestion(settings, tuning, random)
        TrainingMode.ScaleFindNotes -> generateScaleFindNotesQuestion(settings, tuning, random)
        TrainingMode.ChordProgression -> generateChordProgressionQuestion(settings, tuning, random)
    }
}

fun generateNoteRecognitionQuestion(
    settings: TrainingSettings,
    tuning: Tuning,
    random: Random = Random.Default
): NoteRecognitionQuestion {
    val allowed = settings.effectivePitchClasses(TrainingMode.NoteRecognition)
    val fretRange = settings.effectiveFretRange(TrainingMode.NoteRecognition)
    repeat(MAX_GENERATION_ATTEMPTS) {
        val stringIndex = random.nextInt(6)
        val fret = random.nextInt(fretRange.first, fretRange.last + 1)
        val pc = (tuning.pitchClasses[stringIndex] + fret) % 12
        if (pc in allowed) {
            return NoteRecognitionQuestion(stringIndex, fret, pc, tuning)
        }
    }
    val stringIndex = random.nextInt(6)
    val fret = 0
    return NoteRecognitionQuestion(stringIndex, fret, tuning.pitchClasses[stringIndex], tuning)
}

fun generateFindNoteQuestion(
    settings: TrainingSettings,
    tuning: Tuning,
    random: Random = Random.Default
): FindNoteQuestion {
    val mode = TrainingMode.FindNote
    val allowed = settings.effectivePitchClasses(mode).toList()
    val fretRange = settings.effectiveFretRange(mode)
    repeat(MAX_GENERATION_ATTEMPTS) {
        val pitchClass = allowed.random(random)
        val stringIndex = random.nextInt(6)
        val correctFret = (pitchClass - tuning.pitchClasses[stringIndex]).mod(12)
        if (correctFret in fretRange) {
            return FindNoteQuestion(pitchClass, stringIndex, correctFret, tuning)
        }
    }
    return FindNoteQuestion(0, 0, 0, tuning)
}

fun generateChordRecognitionQuestion(
    settings: TrainingSettings,
    tuning: Tuning,
    random: Random = Random.Default
): ChordRecognitionQuestion {
    val mode = TrainingMode.ChordRecognition
    val soundedCount = random.nextInt(3, 7)
    val allowedTypes = settings.allowedChordTypes(mode).filter {
        it.intervals.size <= soundedCount
    }
    val fretRange = settings.effectiveFretRange(mode)

    repeat(MAX_GENERATION_ATTEMPTS) {
        val root = random.nextInt(12)
        val type = allowedTypes.random(random)
        val voicing = pickVoicing(
            root = root,
            type = type,
            tuning = tuning,
            settings = settings,
            soundedCount = soundedCount,
            fretRange = fretRange,
            random = random
        ) ?: return@repeat

        val correctName = chordDisplayName(root, type, voicing, tuning)
        val options = generateChordOptions(root, type, correctName, allowedTypes, random)
        return ChordRecognitionQuestion(voicing, tuning, correctName, options)
    }

    // ultimate fallback: C major open
    return ChordRecognitionQuestion(
        voicing = ChordVoicing(
            listOf(
                ChordNote.Open, ChordNote.Fretted(1), ChordNote.Open,
                ChordNote.Fretted(2), ChordNote.Fretted(3), ChordNote.Muted
            )
        ),
        tuning = tuning,
        correctName = "C",
        options = listOf("C", "G", "Am", "F").shuffled(random)
    )
}

fun generateMarkChordQuestion(
    settings: TrainingSettings,
    tuning: Tuning,
    random: Random = Random.Default
): MarkChordQuestion {
    val mode = TrainingMode.MarkChord
    val soundedCount = random.nextInt(3, 7)
    val allowedTypes = settings.allowedChordTypes(mode).filter {
        it.intervals.size <= soundedCount
    }
    val fretRange = settings.effectiveFretRange(mode)

    repeat(MAX_GENERATION_ATTEMPTS) {
        val root = random.nextInt(12)
        val type = allowedTypes.random(random)
        val positionStart = random.nextInt(fretRange.first, fretRange.last)
        val positionEnd = (positionStart + when (settings.difficultyFor(mode)) {
            Difficulty.Easy -> random.nextInt(2, 4)
            Difficulty.Medium -> random.nextInt(3, 5)
            Difficulty.Hard -> random.nextInt(4, 6)
        }).coerceAtMost(fretRange.last)
        val voicing = pickVoicingInPosition(
            root = root,
            type = type,
            tuning = tuning,
            settings = settings,
            soundedCount = soundedCount,
            positionStart = positionStart,
            positionEnd = positionEnd,
            random = random
        ) ?: return@repeat

        val name = chordDisplayName(root, type, voicing, tuning)
        return MarkChordQuestion(
            chordName = name,
            rootPitchClass = root,
            chordType = type,
            positionStart = positionStart,
            positionEnd = positionEnd,
            tuning = tuning,
            requireRootInBass = !settings.allowChordInversions
        )
    }

    return MarkChordQuestion(
        chordName = "C",
        rootPitchClass = 0,
        chordType = ChordType.MAJOR,
        positionStart = 0,
        positionEnd = 3,
        tuning = tuning,
        requireRootInBass = true
    )
}

fun generateIntervalRecognitionQuestion(
    settings: TrainingSettings,
    tuning: Tuning,
    random: Random = Random.Default
): IntervalRecognitionQuestion {
    val mode = TrainingMode.IntervalRecognition
    val allowed = settings.effectivePitchClasses(mode)
    val fretRange = settings.effectiveFretRange(mode)
    val difficulty = settings.difficultyFor(mode)
    repeat(MAX_GENERATION_ATTEMPTS) {
        val string1 = random.nextInt(6)
        val fret1 = if (difficulty == Difficulty.Easy) 0 else random.nextInt(fretRange.first, fretRange.last + 1)
        val pc1 = (tuning.pitchClasses[string1] + fret1) % 12
        if (pc1 !in allowed) return@repeat

        val allowedStringDistances = when (difficulty) {
            Difficulty.Easy -> setOf(1)
            Difficulty.Medium -> setOf(1, 2)
            Difficulty.Hard -> (1..5).toSet()
        }
        val possibleString2 = (0..5).filter { kotlin.math.abs(it - string1) in allowedStringDistances }
        if (possibleString2.isEmpty()) return@repeat
        val string2 = possibleString2.random(random)
        val minFret2 = if (difficulty == Difficulty.Easy) 1 else fretRange.first
        val fret2 = random.nextInt(minFret2, fretRange.last + 1)
        val pc2 = (tuning.pitchClasses[string2] + fret2) % 12
        if (pc2 !in allowed || pc2 == pc1) return@repeat

        val interval = (pc2 - pc1).mod(12)
        if (interval == 0) return@repeat

        val possibleIntervals = when (difficulty) {
            Difficulty.Easy -> (1..5)
            else -> (1..11)
        }
        val allNames = possibleIntervals.map(::intervalName)
        val correct = intervalName(interval)
        val distractors = allNames.filter { it != correct }.shuffled(random).take(3)
        val options = (listOf(correct) + distractors).shuffled(random)

        return IntervalRecognitionQuestion(
            lowerString = string1,
            lowerFret = fret1,
            upperString = string2,
            upperFret = fret2,
            intervalSemitones = interval,
            options = options,
            tuning = tuning
        )
    }

    return IntervalRecognitionQuestion(
        lowerString = 0,
        lowerFret = 0,
        upperString = 1,
        upperFret = 7,
        intervalSemitones = 2,
        options = listOf("大二度", "小二度", "大三度", "小三度"),
        tuning = tuning
    )
}

fun generateEarTrainingQuestion(
    settings: TrainingSettings,
    tuning: Tuning,
    random: Random = Random.Default
): EarTrainingQuestion {
    val mode = TrainingMode.EarTraining
    val allowed = settings.effectivePitchClasses(mode)
    val fretRange = settings.effectiveFretRange(mode)
    repeat(MAX_GENERATION_ATTEMPTS) {
        val stringIndex = random.nextInt(6)
        val fret = random.nextInt(fretRange.first, fretRange.last + 1)
        val pc = (tuning.pitchClasses[stringIndex] + fret) % 12
        if (pc in allowed) {
            return EarTrainingQuestion(pc, stringIndex, fret, tuning)
        }
    }
    return EarTrainingQuestion(0, 0, 8, tuning)
}

fun generateScaleFindNotesQuestion(
    settings: TrainingSettings,
    tuning: Tuning,
    random: Random = Random.Default
): ScaleFindNotesQuestion {
    val mode = TrainingMode.ScaleFindNotes
    val allowedRoots = settings.effectivePitchClasses(mode).toList()
    val scaleType = settings.allowedScaleTypes(mode).random(random)
    val fretRange = settings.effectiveFretRange(mode)

    repeat(MAX_GENERATION_ATTEMPTS) {
        val keyRoot = allowedRoots.random(random)
        val startFret = random.nextInt(fretRange.first, fretRange.last - 1)
        val endFret = (startFret + when (settings.difficultyFor(mode)) {
            Difficulty.Easy -> random.nextInt(2, 4)
            Difficulty.Medium -> random.nextInt(3, 5)
            Difficulty.Hard -> random.nextInt(4, 6)
        }).coerceAtMost(fretRange.last)
        val scalePcs = scaleType.intervals.map { (keyRoot + it) % 12 }.toSet()

        val targets = mutableSetOf<Pair<Int, Int>>()
        for (stringIndex in 0 until 6) {
            for (fret in startFret..endFret) {
                val pc = (tuning.pitchClasses[stringIndex] + fret) % 12
                if (pc in scalePcs) {
                    targets.add(stringIndex to fret)
                }
            }
        }
        if (targets.size >= 4) {
            return ScaleFindNotesQuestion(keyRoot, scaleType, startFret, endFret, targets, tuning)
        }
    }

    return ScaleFindNotesQuestion(
        keyRoot = 0,
        scaleType = ScaleType.Major,
        startFret = 0,
        endFret = 3,
        targetPositions = setOf(0 to 0, 1 to 1, 2 to 0, 3 to 2),
        tuning = tuning
    )
}

fun generateChordProgressionQuestion(
    settings: TrainingSettings,
    tuning: Tuning,
    random: Random = Random.Default
): ChordProgressionQuestion {
    val mode = TrainingMode.ChordProgression
    val difficulty = settings.difficultyFor(mode)
    val degrees = when (difficulty) {
        Difficulty.Easy -> listOf(
            "I" to ChordType.MAJOR,
            "IV" to ChordType.MAJOR,
            "V" to ChordType.MAJOR
        )
        Difficulty.Medium -> listOf(
            "I" to ChordType.MAJOR,
            "ii" to ChordType.MINOR,
            "iii" to ChordType.MINOR,
            "IV" to ChordType.MAJOR,
            "V" to ChordType.DOMINANT7,
            "vi" to ChordType.MINOR
        )
        Difficulty.Hard -> listOf(
            "I" to ChordType.MAJOR,
            "ii" to ChordType.MINOR,
            "iii" to ChordType.MINOR,
            "IV" to ChordType.MAJOR,
            "V" to ChordType.DOMINANT7,
            "vi" to ChordType.MINOR,
            "vii°" to ChordType.DIMINISHED
        )
    }
    val allowedRoots = settings.effectivePitchClasses(mode).toList()
    val keyRoot = allowedRoots.random(random)

    val progression = List(4) { degrees.random(random) }
    val chordIndex = random.nextInt(progression.size)
    val (roman, type) = progression[chordIndex]
    val rootPc = (keyRoot + romanToOffset(roman)) % 12
    val chordName = rootPc.toNoteName() + type.suffix

    val allChordNames = progression.map { (r, t) ->
        ((keyRoot + romanToOffset(r)) % 12).toNoteName() + t.suffix
    }
    val allowedTypesForFallback = degrees.map { it.second }.distinct()
    val distractors = allChordNames.filter { it != chordName }
        .ifEmpty { generateRandomChordNames(3, random, allowedTypesForFallback) }
        .shuffled(random)
        .take(3)
    val options = (listOf(chordName) + distractors).shuffled(random)

    return ChordProgressionQuestion(
        keyRoot = keyRoot,
        progression = progression,
        chordIndex = chordIndex,
        chordName = chordName,
        options = options,
        tuning = tuning
    )
}

private fun romanToOffset(roman: String): Int {
    return when (roman) {
        "I", "i" -> 0
        "II", "ii" -> 2
        "III", "iii" -> 4
        "IV", "iv" -> 5
        "V", "v" -> 7
        "VI", "vi" -> 9
        "VII", "vii", "vii°" -> 11
        else -> 0
    }
}

private fun pickVoicing(
    root: Int,
    type: ChordType,
    tuning: Tuning,
    settings: TrainingSettings,
    random: Random,
    soundedCount: Int,
    fretRange: IntRange = 0..12
): ChordVoicing? {
    val candidates = generateVoicings(root, type, tuning)
        .filter { voicing ->
            val sounded = voicing.notes.mapIndexedNotNull { index, note ->
                note.fretOrNull()?.let { index to it }
            }
            if (sounded.size != soundedCount) return@filter false
            if (sounded.any { it.second > 0 && it.second !in fretRange }) return@filter false
            val lowestString = sounded.maxOf { it.first }
            if (lowestString < 2) return@filter false
            if (!settings.allowChordInversions) {
                val bassPc = (tuning.pitchClasses[lowestString] + sounded.first { it.first == lowestString }.second) % 12
                if (bassPc != root) return@filter false
            }
            true
        }
    return if (candidates.isEmpty()) null else candidates.random(random)
}

private fun pickVoicingInPosition(
    root: Int,
    type: ChordType,
    tuning: Tuning,
    settings: TrainingSettings,
    random: Random,
    soundedCount: Int,
    positionStart: Int,
    positionEnd: Int
): ChordVoicing? {
    val candidates = generateVoicings(root, type, tuning)
        .filter { voicing ->
            val sounded = voicing.notes.mapIndexedNotNull { index, note ->
                note.fretOrNull()?.let { index to it }
            }
            if (sounded.size != soundedCount) return@filter false
            val fretted = sounded.map { it.second }.filter { it > 0 }
            if (fretted.any { it !in positionStart..positionEnd }) return@filter false
            val lowestString = sounded.maxOf { it.first }
            if (lowestString < 2) return@filter false
            if (!settings.allowChordInversions) {
                val bassPc = (tuning.pitchClasses[lowestString] + sounded.first { it.first == lowestString }.second) % 12
                if (bassPc != root) return@filter false
            }
            true
        }
    return if (candidates.isEmpty()) null else candidates.random(random)
}

private fun chordDisplayName(
    root: Int,
    type: ChordType,
    voicing: ChordVoicing,
    tuning: Tuning
): String {
    val names = nameChord(voicing, tuning)
    return names.firstOrNull() ?: (root.toNoteName() + type.suffix)
}

private fun generateChordOptions(
    root: Int,
    type: ChordType,
    correctName: String,
    allowedTypes: List<ChordType>,
    random: Random
): List<String> {
    val sameRoot = allowedTypes
        .filter { it != type }
        .map { root.toNoteName() + it.suffix }
        .shuffled(random)
        .take(2)

    val differentRoot = ((root + random.nextInt(1, 12)) % 12).toNoteName() + type.suffix

    val distractors = (sameRoot + differentRoot)
        .filter { it != correctName }
        .distinct()

    val options = (listOf(correctName) + distractors).shuffled(random)
    if (options.size >= 4) return options.take(4)

    val extras = generateRandomChordNames(4 - options.size, random, allowedTypes)
    return (options + extras).distinct().take(4).shuffled(random)
}

private fun generateRandomChordNames(count: Int, random: Random, allowedTypes: List<ChordType>): List<String> {
    val roots = (0..11).map { it.toNoteName() }
    val types = allowedTypes.ifEmpty { ChordType.entries }
    return List(count) {
        roots.random(random) + types.random(random).suffix
    }
}
