package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepLog
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import com.noom.interview.fullstack.sleep.repository.UserRepository
import com.noom.interview.fullstack.sleep.service.exception.DuplicateSleepLogException
import com.noom.interview.fullstack.sleep.service.exception.InvalidSleepLogException
import com.noom.interview.fullstack.sleep.service.exception.SleepLogNotFoundException
import com.noom.interview.fullstack.sleep.service.exception.UserNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.dao.DuplicateKeyException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

class SleepLogServiceTest {

    private val sleepLogRepository = mock<SleepLogRepository>()
    private val userRepository = mock<UserRepository>()

    // Fixed clock so the averages window is deterministic: "today" is 2026-09-02.
    private val clock = Clock.fixed(Instant.parse("2026-09-02T10:00:00Z"), ZoneOffset.UTC)

    private val service = SleepLogService(sleepLogRepository, userRepository, clock)

    private val userId = 1L

    @BeforeEach
    fun stubUserExists() {
        whenever(userRepository.existsById(userId)).thenReturn(true)
    }

    @Test
    fun `createSleepLog derives sleep date from bedtime and persists the log`() {
        val command = CreateSleepLogCommand(
            userId = userId,
            inBedStart = LocalDateTime.parse("2026-09-01T23:00:00"),
            inBedEnd = LocalDateTime.parse("2026-09-02T07:00:00"),
            morningFeeling = MorningFeeling.GOOD,
        )
        whenever(sleepLogRepository.save(any())).thenAnswer { it.arguments[0] as SleepLog }

        val result = service.createSleepLog(command)

        assertThat(result.sleepDate).isEqualTo(LocalDate.parse("2026-09-01"))
        assertThat(result.totalMinutesInBed).isEqualTo(480)
        assertThat(result.morningFeeling).isEqualTo(MorningFeeling.GOOD)
    }

    @Test
    fun `createSleepLog honours an explicit sleep date`() {
        val command = CreateSleepLogCommand(
            userId = userId,
            inBedStart = LocalDateTime.parse("2026-09-01T23:00:00"),
            inBedEnd = LocalDateTime.parse("2026-09-02T07:00:00"),
            morningFeeling = MorningFeeling.OK,
            sleepDate = LocalDate.parse("2026-08-15"),
        )
        whenever(sleepLogRepository.save(any())).thenAnswer { it.arguments[0] as SleepLog }

        val result = service.createSleepLog(command)

        assertThat(result.sleepDate).isEqualTo(LocalDate.parse("2026-08-15"))
    }

    @Test
    fun `createSleepLog rejects an unknown user`() {
        whenever(userRepository.existsById(userId)).thenReturn(false)
        val command = validCommand()

        assertThatThrownBy { service.createSleepLog(command) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    fun `createSleepLog rejects an interval that does not move forward`() {
        val command = CreateSleepLogCommand(
            userId = userId,
            inBedStart = LocalDateTime.parse("2026-09-02T07:00:00"),
            inBedEnd = LocalDateTime.parse("2026-09-02T07:00:00"),
            morningFeeling = MorningFeeling.BAD,
        )

        assertThatThrownBy { service.createSleepLog(command) }
            .isInstanceOf(InvalidSleepLogException::class.java)
    }

    @Test
    fun `createSleepLog maps a unique-constraint violation to a duplicate error`() {
        whenever(sleepLogRepository.save(any())).doThrow(DuplicateKeyException("dup"))

        assertThatThrownBy { service.createSleepLog(validCommand()) }
            .isInstanceOf(DuplicateSleepLogException::class.java)
    }

    @Test
    fun `getLastNight returns the latest log`() {
        val log = persistedLog(sleepDate = LocalDate.parse("2026-09-01"))
        whenever(sleepLogRepository.findLatestByUserId(userId)).thenReturn(log)

        assertThat(service.getLastNight(userId)).isEqualTo(log)
    }

    @Test
    fun `getLastNight throws when the user has no logs`() {
        whenever(sleepLogRepository.findLatestByUserId(userId)).thenReturn(null)

        assertThatThrownBy { service.getLastNight(userId) }
            .isInstanceOf(SleepLogNotFoundException::class.java)
    }

    @Test
    fun `getAverages over an empty range reports nulls and zero frequencies`() {
        whenever(sleepLogRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
            .thenReturn(emptyList())

        val averages = service.getAverages(userId)

        assertThat(averages.rangeEnd).isEqualTo(LocalDate.parse("2026-09-02"))
        assertThat(averages.rangeStart).isEqualTo(LocalDate.parse("2026-08-03")) // today - 30 days
        assertThat(averages.logCount).isZero
        assertThat(averages.averageTotalMinutesInBed).isNull()
        assertThat(averages.averageInBedStart).isNull()
        assertThat(averages.averageInBedEnd).isNull()
        assertThat(averages.feelingFrequencies).containsExactlyInAnyOrderEntriesOf(
            mapOf(MorningFeeling.BAD to 0, MorningFeeling.OK to 0, MorningFeeling.GOOD to 0),
        )
    }

    @Test
    fun `getAverages computes totals, frequencies and clock-time means`() {
        val logs = listOf(
            logWith("2026-09-01T23:00", "2026-09-02T07:00", MorningFeeling.GOOD), // 480 min
            logWith("2026-08-31T01:00", "2026-08-31T07:00", MorningFeeling.OK), // 360 min
        )
        whenever(sleepLogRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
            .thenReturn(logs)

        val averages = service.getAverages(userId)

        assertThat(averages.logCount).isEqualTo(2)
        assertThat(averages.averageTotalMinutesInBed).isEqualTo(420) // (480 + 360) / 2
        // Bedtimes 23:00 and 01:00 straddle midnight; circular mean is midnight.
        assertThat(averages.averageInBedStart).isEqualTo(LocalTime.of(0, 0))
        assertThat(averages.averageInBedEnd).isEqualTo(LocalTime.of(7, 0))
        assertThat(averages.feelingFrequencies).isEqualTo(
            mapOf(MorningFeeling.BAD to 0, MorningFeeling.OK to 1, MorningFeeling.GOOD to 1),
        )
    }

    @Test
    fun `getAverages averages bedtimes on either side of midnight without collapsing to noon`() {
        val logs = listOf(
            logWith("2026-09-01T23:50", "2026-09-02T07:00", MorningFeeling.OK),
            logWith("2026-08-31T00:10", "2026-08-31T07:00", MorningFeeling.OK),
        )
        whenever(sleepLogRepository.findByUserIdAndDateRange(eq(userId), any(), any()))
            .thenReturn(logs)

        val averages = service.getAverages(userId)

        assertThat(averages.averageInBedStart).isEqualTo(LocalTime.of(0, 0))
    }

    @Test
    fun `getAverages rejects a non-positive window`() {
        assertThatThrownBy { service.getAverages(userId, days = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun validCommand() = CreateSleepLogCommand(
        userId = userId,
        inBedStart = LocalDateTime.parse("2026-09-01T23:00:00"),
        inBedEnd = LocalDateTime.parse("2026-09-02T07:00:00"),
        morningFeeling = MorningFeeling.GOOD,
    )

    private fun logWith(start: String, end: String, feeling: MorningFeeling) = SleepLog(
        id = 1L,
        userId = userId,
        sleepDate = LocalDateTime.parse(start).toLocalDate(),
        inBedStart = LocalDateTime.parse(start),
        inBedEnd = LocalDateTime.parse(end),
        morningFeeling = feeling,
    )

    private fun persistedLog(sleepDate: LocalDate) = SleepLog(
        id = 42L,
        userId = userId,
        sleepDate = sleepDate,
        inBedStart = LocalDateTime.of(sleepDate, LocalTime.of(23, 0)),
        inBedEnd = LocalDateTime.of(sleepDate.plusDays(1), LocalTime.of(7, 0)),
        morningFeeling = MorningFeeling.GOOD,
    )
}
