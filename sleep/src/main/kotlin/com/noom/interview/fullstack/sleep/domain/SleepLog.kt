package com.noom.interview.fullstack.sleep.domain

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A single night's sleep log belonging to a user.
 *
 * The in-bed interval is stored as wall-clock timestamps so the total time in
 * bed stays correct when sleep crosses midnight. [totalMinutesInBed] is derived
 * from the interval and is therefore always consistent with it.
 */
data class SleepLog(
    val id: Long? = null,
    val userId: Long,
    val sleepDate: LocalDate,
    val inBedStart: LocalDateTime,
    val inBedEnd: LocalDateTime,
    val morningFeeling: MorningFeeling,
    val createdAt: LocalDateTime? = null,
) {
    val totalMinutesInBed: Long
        get() = Duration.between(inBedStart, inBedEnd).toMinutes()
}
