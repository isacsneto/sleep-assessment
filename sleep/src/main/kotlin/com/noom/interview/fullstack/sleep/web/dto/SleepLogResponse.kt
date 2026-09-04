package com.noom.interview.fullstack.sleep.web.dto

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepLog
import java.time.LocalDate
import java.time.LocalDateTime

/** API representation of a single sleep log. */
data class SleepLogResponse(
    val id: Long,
    val sleepDate: LocalDate,
    val inBedStart: LocalDateTime,
    val inBedEnd: LocalDateTime,
    val totalMinutesInBed: Long,
    val morningFeeling: MorningFeeling,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(log: SleepLog) = SleepLogResponse(
            id = requireNotNull(log.id) { "persisted sleep log must have an id" },
            sleepDate = log.sleepDate,
            inBedStart = log.inBedStart,
            inBedEnd = log.inBedEnd,
            totalMinutesInBed = log.totalMinutesInBed,
            morningFeeling = log.morningFeeling,
            createdAt = log.createdAt,
        )
    }
}
