package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.domain.MorningFeeling
import com.noom.interview.fullstack.sleep.domain.SleepLog
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

/**
 * Repository tests against a real Postgres (Testcontainers) with the Flyway
 * migrations applied, so schema constraints and SQL are exercised for real.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SleepLogRepositoryTest {

    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
    private lateinit var repository: SleepLogRepository
    private lateinit var userRepository: UserRepository

    private val seededUserId = 1L

    @BeforeAll
    fun setUp() {
        val dataSource = DriverManagerDataSource(
            postgres.jdbcUrl,
            postgres.username,
            postgres.password,
        ).apply { setDriverClassName("org.postgresql.Driver") }

        Flyway.configure().dataSource(dataSource).load().migrate()

        jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        repository = SleepLogRepository(jdbcTemplate)
        userRepository = UserRepository(jdbcTemplate)
    }

    @BeforeEach
    fun cleanSleepLogs() {
        jdbcTemplate.jdbcTemplate.execute("TRUNCATE TABLE sleep_logs RESTART IDENTITY")
    }

    @Test
    fun `save returns the generated id and created timestamp`() {
        val saved = repository.save(newLog(LocalDate.parse("2026-09-01")))

        assertThat(saved.id).isNotNull
        assertThat(saved.createdAt).isNotNull
        assertThat(saved.totalMinutesInBed).isEqualTo(480)
    }

    @Test
    fun `findLatestByUserId returns the most recent log by sleep date`() {
        repository.save(newLog(LocalDate.parse("2026-08-30")))
        val newest = repository.save(newLog(LocalDate.parse("2026-09-01")))
        repository.save(newLog(LocalDate.parse("2026-08-31")))

        val latest = repository.findLatestByUserId(seededUserId)

        assertThat(latest?.sleepDate).isEqualTo(newest.sleepDate)
    }

    @Test
    fun `findLatestByUserId returns null when the user has no logs`() {
        assertThat(repository.findLatestByUserId(seededUserId)).isNull()
    }

    @Test
    fun `findByUserIdAndDateRange filters inclusively and orders newest first`() {
        repository.save(newLog(LocalDate.parse("2026-08-01"))) // before range
        repository.save(newLog(LocalDate.parse("2026-08-10"))) // range start
        repository.save(newLog(LocalDate.parse("2026-08-15")))
        repository.save(newLog(LocalDate.parse("2026-08-20"))) // range end
        repository.save(newLog(LocalDate.parse("2026-08-25"))) // after range

        val logs = repository.findByUserIdAndDateRange(
            seededUserId,
            LocalDate.parse("2026-08-10"),
            LocalDate.parse("2026-08-20"),
        )

        assertThat(logs.map { it.sleepDate }).containsExactly(
            LocalDate.parse("2026-08-20"),
            LocalDate.parse("2026-08-15"),
            LocalDate.parse("2026-08-10"),
        )
    }

    @Test
    fun `saving two logs for the same user and night violates the unique constraint`() {
        val date = LocalDate.parse("2026-09-01")
        repository.save(newLog(date))

        assertThatThrownBy { repository.save(newLog(date)) }
            .isInstanceOf(DuplicateKeyException::class.java)
    }

    @Test
    fun `existsById reflects the seeded user`() {
        assertThat(userRepository.existsById(seededUserId)).isTrue
        assertThat(userRepository.existsById(999L)).isFalse
    }

    private fun newLog(sleepDate: LocalDate) = SleepLog(
        userId = seededUserId,
        sleepDate = sleepDate,
        inBedStart = LocalDateTime.of(sleepDate, java.time.LocalTime.of(23, 0)),
        inBedEnd = LocalDateTime.of(sleepDate.plusDays(1), java.time.LocalTime.of(7, 0)),
        morningFeeling = MorningFeeling.GOOD,
    )

    companion object {
        @Container
        @JvmStatic
        private val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:13-alpine").apply { start() }
    }
}
