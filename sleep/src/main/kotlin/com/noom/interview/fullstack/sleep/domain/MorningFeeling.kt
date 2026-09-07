package com.noom.interview.fullstack.sleep.domain

/**
 * How the user felt in the morning after a night's sleep.
 *
 * Persisted by name; the sleep_logs.morning_feeling column is constrained to
 * exactly these values.
 */
enum class MorningFeeling {
    BAD,
    OK,
    GOOD,
}
