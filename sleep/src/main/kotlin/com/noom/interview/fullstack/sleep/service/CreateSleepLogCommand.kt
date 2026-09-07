package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Input for creating a sleep log, decoupled from the web/DTO layer.
 *
 * [sleepDate] is optional; when absent the service derives it from the date the
 * user got to bed ([inBedStart]).
 */
data class CreateSleepLogCommand(
    val userId: Long,
    val inBedStart: LocalDateTime,
    val inBedEnd: LocalDateTime,
    val morningFeeling: MorningFeeling,
    val sleepDate: LocalDate? = null,
)
