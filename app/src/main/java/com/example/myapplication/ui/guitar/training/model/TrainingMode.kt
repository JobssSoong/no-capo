package com.example.myapplication.ui.guitar.training.model

enum class TrainingMode(
    val title: String,
    val description: String
) {
    NoteRecognition(
        title = "识音挑战",
        description = "指板上高亮一个音，选择它的音名"
    ),
    FindNote(
        title = "找音位置",
        description = "给出音名和一根弦，在指板上点出位置"
    ),
    ChordRecognition(
        title = "和弦识别",
        description = "看指板按法，选出正确和弦"
    ),
    MarkChord(
        title = "按和弦",
        description = "给出和弦名和把位，在指板上标出来"
    ),
    IntervalRecognition(
        title = "音程识别",
        description = "指板上两个音，判断音程"
    ),
    EarTraining(
        title = "听音找位置",
        description = "听一个音，在指板上点出位置"
    ),
    ScaleFindNotes(
        title = "音阶找音",
        description = "在指定把位内标出某音阶的所有音"
    ),
    ChordProgression(
        title = "和弦进行",
        description = "给出调式和进行，选出指定级数的和弦"
    );

    companion object {
        val entries: List<TrainingMode>
            @JvmStatic
            get() = values().asList()
    }
}

enum class Difficulty(val label: String) {
    Easy("简单"),
    Medium("中等"),
    Hard("困难")
}

fun difficultyDescription(mode: TrainingMode, difficulty: Difficulty): String {
    return when (mode) {
        TrainingMode.NoteRecognition -> when (difficulty) {
            Difficulty.Easy -> "0–5 品，自然音"
            Difficulty.Medium -> "0–12 品，自然音"
            Difficulty.Hard -> "0–12 品，含升降号"
        }
        TrainingMode.FindNote -> when (difficulty) {
            Difficulty.Easy -> "0–5 品，自然音"
            Difficulty.Medium -> "0–12 品，自然音"
            Difficulty.Hard -> "0–12 品，含升降号"
        }
        TrainingMode.ChordRecognition -> when (difficulty) {
            Difficulty.Easy -> "0–5 品，大三/小三，显示音名"
            Difficulty.Medium -> "0–12 品，常用和弦，不显示音名"
            Difficulty.Hard -> "0–12 品，所有和弦，不显示音名"
        }
        TrainingMode.MarkChord -> when (difficulty) {
            Difficulty.Easy -> "0–5 品，大三/小三和弦"
            Difficulty.Medium -> "0–12 品，常用和弦"
            Difficulty.Hard -> "0–12 品，所有和弦"
        }
        TrainingMode.IntervalRecognition -> when (difficulty) {
            Difficulty.Easy -> "同一弦 0–5 品，只显示一个音"
            Difficulty.Medium -> "0–12 品，自然音，不显示音名"
            Difficulty.Hard -> "0–12 品，含升降号，不显示音名"
        }
        TrainingMode.EarTraining -> when (difficulty) {
            Difficulty.Easy -> "0–5 品，自然音"
            Difficulty.Medium -> "0–12 品，自然音"
            Difficulty.Hard -> "0–12 品，含升降号"
        }
        TrainingMode.ScaleFindNotes -> when (difficulty) {
            Difficulty.Easy -> "0–5 品，五声音阶"
            Difficulty.Medium -> "0–12 品，大/小调音阶"
            Difficulty.Hard -> "0–12 品，所有音阶"
        }
        TrainingMode.ChordProgression -> when (difficulty) {
            Difficulty.Easy -> "I、IV、V 级大三和弦"
            Difficulty.Medium -> "自然大调顺阶三和弦"
            Difficulty.Hard -> "含七和弦及变化音"
        }
    }
}
