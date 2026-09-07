package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepLog
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Persistence for [SleepLog] rows using Spring's [NamedParameterJdbcTemplate].
 *
 * Averages are intentionally computed in the service layer rather than in SQL:
 * averaging clock times (get-to-bed / get-out-of-bed) is business logic worth
 * unit testing, so this repository only exposes the range query it needs.
 */
@Repository
class SleepLogRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    /**
     * Inserts a new sleep log and returns it with the generated id and
     * created_at populated.
     */
    fun save(sleepLog: SleepLog): SleepLog {
        val sql = """
            INSERT INTO sleep_logs
                (user_id, sleep_date, in_bed_start, in_bed_end, total_minutes_in_bed, morning_feeling)
            VALUES
                (:userId, :sleepDate, :inBedStart, :inBedEnd, :totalMinutes, :morningFeeling)
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", sleepLog.userId)
            .addValue("sleepDate", sleepLog.sleepDate)
            .addValue("inBedStart", sleepLog.inBedStart)
            .addValue("inBedEnd", sleepLog.inBedEnd)
            .addValue("totalMinutes", sleepLog.totalMinutesInBed)
            .addValue("morningFeeling", sleepLog.morningFeeling.name)

        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update(sql, params, keyHolder, arrayOf("id", "created_at"))

        val keys = keyHolder.keys ?: emptyMap()
        return sleepLog.copy(
            id = (keys["id"] as Number).toLong(),
            createdAt = (keys["created_at"] as java.sql.Timestamp).toLocalDateTime(),
        )
    }

    /** Returns the user's most recent sleep log, or null if they have none. */
    fun findLatestByUserId(userId: Long): SleepLog? {
        val sql = """
            SELECT * FROM sleep_logs
            WHERE user_id = :userId
            ORDER BY sleep_date DESC
            LIMIT 1
        """.trimIndent()
        val params = MapSqlParameterSource("userId", userId)
        return jdbcTemplate.query(sql, params, rowMapper).firstOrNull()
    }

    /**
     * Returns the user's sleep logs whose sleep_date falls within
     * [from, to] (both inclusive), most recent first.
     */
    fun findByUserIdAndDateRange(userId: Long, from: LocalDate, to: LocalDate): List<SleepLog> {
        val sql = """
            SELECT * FROM sleep_logs
            WHERE user_id = :userId
              AND sleep_date BETWEEN :from AND :to
            ORDER BY sleep_date DESC
        """.trimIndent()
        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("from", from)
            .addValue("to", to)
        return jdbcTemplate.query(sql, params, rowMapper)
    }

    private val rowMapper = RowMapper { rs, _ ->
        SleepLog(
            id = rs.getLong("id"),
            userId = rs.getLong("user_id"),
            sleepDate = rs.getObject("sleep_date", LocalDate::class.java),
            inBedStart = rs.getObject("in_bed_start", LocalDateTime::class.java),
            inBedEnd = rs.getObject("in_bed_end", LocalDateTime::class.java),
            morningFeeling = MorningFeeling.valueOf(rs.getString("morning_feeling")),
            createdAt = rs.getObject("created_at", LocalDateTime::class.java),
        )
    }
}
