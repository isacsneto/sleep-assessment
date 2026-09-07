package com.noom.interview.fullstack.sleep.domain

import java.time.LocalDate
import java.time.LocalTime

/**
 * Aggregated view of a user's sleep over a date range (e.g. the last 30 days).
 *
 * All averages are null when the range contains no sleep logs.
 */
data class SleepAverages(
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val logCount: Int,
    val averageTotalMinutesInBed: Long?,
    val averageInBedStart: LocalTime?,
    val averageInBedEnd: LocalTime?,
    /** Count of logs per morning feeling. Every [MorningFeeling] is present, defaulting to 0. */
    val feelingFrequencies: Map<MorningFeeling, Int>,
)
