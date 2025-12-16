package com.subhajitrajak.durare.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DailyWorkoutStats(
    val date: String = "",
    val sets: Int = 0,
    val reps: Int = 0,
    val activeTimeMs: Long = 0L,
    val averageRepDurationMs: Long = 0L,
    val restTimeMs: Long = 0L
) : Parcelable