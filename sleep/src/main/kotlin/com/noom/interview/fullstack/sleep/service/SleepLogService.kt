package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepAverages
import com.noom.interview.fullstack.sleep.domain.SleepLog
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import com.noom.interview.fullstack.sleep.repository.UserRepository
import com.noom.interview.fullstack.sleep.service.exception.DuplicateSleepLogException
import com.noom.interview.fullstack.sleep.service.exception.InvalidSleepLogException
import com.noom.interview.fullstack.sleep.service.exception.SleepLogNotFoundException
import com.noom.interview.fullstack.sleep.service.exception.UserNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

@Service
class SleepLogService(
    private val sleepLogRepository: SleepLogRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    /**
     * Creates a sleep log for the user. The sleep date defaults to the date the
     * user got to bed when not supplied.
     */
    fun createSleepLog(command: CreateSleepLogCommand): SleepLog {
        requireExistingUser(command.userId)

        if (!command.inBedEnd.isAfter(command.inBedStart)) {
            throw InvalidSleepLogException("in-bed end must be after in-bed start")
        }

        val sleepLog = SleepLog(
            userId = command.userId,
            sleepDate = command.sleepDate ?: command.inBedStart.toLocalDate(),
            inBedStart = command.inBedStart,
            inBedEnd = command.inBedEnd,
            morningFeeling = command.morningFeeling,
        )

        return try {
            sleepLogRepository.save(sleepLog)
        } catch (e: DuplicateKeyException) {
            throw DuplicateSleepLogException(
                "A sleep log already exists for user ${sleepLog.userId} on ${sleepLog.sleepDate}",
            )
        }
    }

    /** Returns the user's most recent sleep log, or throws if they have none. */
    fun getLastNight(userId: Long): SleepLog {
        requireExistingUser(userId)
        return sleepLogRepository.findLatestByUserId(userId)
            ?: throw SleepLogNotFoundException("User $userId has no sleep logs yet")
    }

    /**
     * Computes averages over the last [days] days (inclusive of today).
     *
     * The average get-to-bed and get-out-of-bed times use a circular mean so
     * that times on either side of midnight (e.g. 11:51 pm and 00:10 am)
     * average sensibly instead of collapsing toward midday.
     */
    fun getAverages(userId: Long, days: Long = DEFAULT_AVERAGE_WINDOW_DAYS): SleepAverages {
        requireExistingUser(userId)
        require(days > 0) { "days must be positive" }

        val today = LocalDate.now(clock)
        val rangeStart = today.minusDays(days)
        val logs = sleepLogRepository.findByUserIdAndDateRange(userId, rangeStart, today)

        val frequencies = MorningFeeling.values().associateWith { feeling ->
            logs.count { it.morningFeeling == feeling }
        }

        if (logs.isEmpty()) {
            return SleepAverages(
                rangeStart = rangeStart,
                rangeEnd = today,
                logCount = 0,
                averageTotalMinutesInBed = null,
                averageInBedStart = null,
                averageInBedEnd = null,
                feelingFrequencies = frequencies,
            )
        }

        return SleepAverages(
            rangeStart = rangeStart,
            rangeEnd = today,
            logCount = logs.size,
            averageTotalMinutesInBed = logs.map { it.totalMinutesInBed }.average().roundToLong(),
            averageInBedStart = averageTimeOfDay(logs.map { it.inBedStart.toLocalTime() }),
            averageInBedEnd = averageTimeOfDay(logs.map { it.inBedEnd.toLocalTime() }),
            feelingFrequencies = frequencies,
        )
    }

    private fun requireExistingUser(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException(userId)
        }
    }

    /**
     * Circular mean of a set of times of day, returned at minute precision.
     * Each time is mapped onto the unit circle over a 24h period; the average
     * angle is converted back to a time, which keeps the result correct across
     * the midnight boundary.
     */
    private fun averageTimeOfDay(times: List<LocalTime>): LocalTime {
        var sinSum = 0.0
        var cosSum = 0.0
        for (time in times) {
            val minuteOfDay = time.hour * MINUTES_PER_HOUR + time.minute
            val angle = TWO_PI * minuteOfDay / MINUTES_PER_DAY
            sinSum += sin(angle)
            cosSum += cos(angle)
        }

        var averageAngle = atan2(sinSum / times.size, cosSum / times.size)
        if (averageAngle < 0) {
            averageAngle += TWO_PI
        }

        val averageMinute = (averageAngle / TWO_PI * MINUTES_PER_DAY).roundToInt() % MINUTES_PER_DAY
        return LocalTime.of(averageMinute / MINUTES_PER_HOUR, averageMinute % MINUTES_PER_HOUR)
    }

    companion object {
        const val DEFAULT_AVERAGE_WINDOW_DAYS = 30L
        private const val MINUTES_PER_HOUR = 60
        private const val MINUTES_PER_DAY = 24 * 60
        private const val TWO_PI = 2 * Math.PI
    }
}
