package com.noom.interview.fullstack.sleep.web.dto

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepAverages
import java.time.LocalDate
import java.time.LocalTime

/** API representation of the last-N-days sleep averages. */
data class SleepAveragesResponse(
    val rangeStart: LocalDate,
    val rangeEnd: LocalDate,
    val logCount: Int,
    val averageTotalMinutesInBed: Long?,
    val averageInBedStart: LocalTime?,
    val averageInBedEnd: LocalTime?,
    val feelingFrequencies: Map<MorningFeeling, Int>,
) {
    companion object {
        fun from(averages: SleepAverages) = SleepAveragesResponse(
            rangeStart = averages.rangeStart,
            rangeEnd = averages.rangeEnd,
            logCount = averages.logCount,
            averageTotalMinutesInBed = averages.averageTotalMinutesInBed,
            averageInBedStart = averages.averageInBedStart,
            averageInBedEnd = averages.averageInBedEnd,
            feelingFrequencies = averages.feelingFrequencies,
        )
    }
}
