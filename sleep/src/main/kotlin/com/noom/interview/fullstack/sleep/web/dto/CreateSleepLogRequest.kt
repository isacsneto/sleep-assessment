package com.noom.interview.fullstack.sleep.web.dto

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.service.CreateSleepLogCommand
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Request body for creating a sleep log.
 *
 * Non-null Kotlin fields are enforced by the Jackson Kotlin module, so a
 * missing required field is rejected as a bad request rather than silently
 * defaulting to null.
 */
data class CreateSleepLogRequest(
    val inBedStart: LocalDateTime,
    val inBedEnd: LocalDateTime,
    val morningFeeling: MorningFeeling,
    /** Optional; defaults to the date the user got to bed. */
    val sleepDate: LocalDate? = null,
) {
    fun toCommand(userId: Long) = CreateSleepLogCommand(
        userId = userId,
        inBedStart = inBedStart,
        inBedEnd = inBedEnd,
        morningFeeling = morningFeeling,
        sleepDate = sleepDate,
    )
}
