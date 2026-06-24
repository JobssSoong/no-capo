package com.example.myapplication.ui.guitar.training.stats

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Context.trainingDataStore: DataStore<Preferences> by preferencesDataStore(name = "training_stats")

data class TrainingStats(
    val totalPracticeSeconds: Long = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCheckInDate: String? = null,
    val totalCorrect: Int = 0,
    val totalAnswered: Int = 0,
    val practiceDates: Set<String> = emptySet()
)

class TrainingStatsRepository(context: Context) {
    private val dataStore = context.trainingDataStore

    val statsFlow: Flow<TrainingStats> = dataStore.data.map { prefs ->
        TrainingStats(
            totalPracticeSeconds = prefs[Keys.TOTAL_PRACTICE_SECONDS] ?: 0L,
            currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0,
            longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0,
            lastCheckInDate = prefs[Keys.LAST_CHECK_IN_DATE],
            totalCorrect = prefs[Keys.TOTAL_CORRECT] ?: 0,
            totalAnswered = prefs[Keys.TOTAL_ANSWERED] ?: 0,
            practiceDates = prefs[Keys.PRACTICE_DATES]?.toSet() ?: emptySet()
        )
    }

    suspend fun recordSession(
        correct: Int,
        answered: Int,
        seconds: Int
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.TOTAL_PRACTICE_SECONDS] = (prefs[Keys.TOTAL_PRACTICE_SECONDS] ?: 0L) + seconds
            prefs[Keys.TOTAL_CORRECT] = (prefs[Keys.TOTAL_CORRECT] ?: 0) + correct
            prefs[Keys.TOTAL_ANSWERED] = (prefs[Keys.TOTAL_ANSWERED] ?: 0) + answered

            val today = getTodayString()
            val updatedDates = (prefs[Keys.PRACTICE_DATES] ?: emptySet()).toMutableSet()
            updatedDates.add(today)
            prefs[Keys.PRACTICE_DATES] = updatedDates
        }
    }

    suspend fun recordCheckIn() {
        val today = getTodayString()
        dataStore.edit { prefs ->
            val lastDate = prefs[Keys.LAST_CHECK_IN_DATE]
            val currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0
            val newStreak = when {
                lastDate == today -> currentStreak
                lastDate == getYesterdayString() -> currentStreak + 1
                else -> 1
            }
            prefs[Keys.CURRENT_STREAK] = newStreak
            prefs[Keys.LONGEST_STREAK] = maxOf<Int>(prefs[Keys.LONGEST_STREAK] ?: 0, newStreak)
            prefs[Keys.LAST_CHECK_IN_DATE] = today

            val updatedDates = (prefs[Keys.PRACTICE_DATES] ?: emptySet()).toMutableSet()
            updatedDates.add(today)
            prefs[Keys.PRACTICE_DATES] = updatedDates
        }
    }

    private object Keys {
        val TOTAL_PRACTICE_SECONDS = longPreferencesKey("total_practice_seconds")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val LAST_CHECK_IN_DATE = stringPreferencesKey("last_check_in_date")
        val TOTAL_CORRECT = intPreferencesKey("total_correct")
        val TOTAL_ANSWERED = intPreferencesKey("total_answered")
        val PRACTICE_DATES = stringSetPreferencesKey("practice_dates")
    }
}

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

fun getTodayString(): String {
    return dateFormat.format(Calendar.getInstance().time)
}

fun getYesterdayString(): String {
    val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return dateFormat.format(calendar.time)
}

fun isToday(dateString: String?): Boolean = dateString == getTodayString()

fun isYesterday(dateString: String?): Boolean = dateString == getYesterdayString()
