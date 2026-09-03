package com.noom.interview.fullstack.sleep.repository

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

/**
 * Minimal access to the users table. Authentication is out of scope, so this
 * only needs to answer whether a given user exists.
 */
@Repository
class UserRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    fun existsById(userId: Long): Boolean {
        val sql = "SELECT EXISTS(SELECT 1 FROM users WHERE id = :userId)"
        val params = MapSqlParameterSource("userId", userId)
        return jdbcTemplate.queryForObject(sql, params, Boolean::class.java) ?: false
    }
}
