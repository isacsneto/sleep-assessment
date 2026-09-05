package com.noom.interview.fullstack.sleep

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Smoke test: boots the full application context against a real Postgres with
 * the Flyway migrations applied, verifying that beans wire up end to end.
 */
@SpringBootTest
@Testcontainers
class SleepApplicationTests {

	@Test
	fun contextLoads() {
	}

	companion object {
		@Container
		@JvmStatic
		private val postgres: PostgreSQLContainer<*> =
			PostgreSQLContainer("postgres:13-alpine")

		@JvmStatic
		@DynamicPropertySource
		fun datasourceProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
		}
	}
}
