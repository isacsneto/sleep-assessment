package com.noom.interview.fullstack.sleep.web

import com.noom.interview.fullstack.sleep.service.SleepLogService
import com.noom.interview.fullstack.sleep.web.dto.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.web.dto.SleepAveragesResponse
import com.noom.interview.fullstack.sleep.web.dto.SleepLogResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * REST API for the sleep logger.
 *
 * Authentication is out of scope, but every request is scoped to a user via the
 * `X-User-Id` header. The header defaults to the seeded user so the API is
 * usable out of the box.
 */
@RestController
@RequestMapping("/api/sleep-logs")
class SleepLogController(
    private val sleepLogService: SleepLogService,
) {
    /** REQ #1: create the sleep log for last night. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createSleepLog(
        @RequestHeader(name = USER_ID_HEADER, defaultValue = DEFAULT_USER_ID) userId: Long,
        @RequestBody request: CreateSleepLogRequest,
    ): SleepLogResponse {
        val created = sleepLogService.createSleepLog(request.toCommand(userId))
        return SleepLogResponse.from(created)
    }

    /** REQ #1B: fetch information about last night's sleep. */
    @GetMapping("/last-night")
    fun getLastNight(
        @RequestHeader(name = USER_ID_HEADER, defaultValue = DEFAULT_USER_ID) userId: Long,
    ): SleepLogResponse {
        return SleepLogResponse.from(sleepLogService.getLastNight(userId))
    }

    /** REQ #3: get the last N-day averages (defaults to 30 days). */
    @GetMapping("/averages")
    fun getAverages(
        @RequestHeader(name = USER_ID_HEADER, defaultValue = DEFAULT_USER_ID) userId: Long,
        @RequestParam(name = "days", defaultValue = "30") days: Long,
    ): SleepAveragesResponse {
        return SleepAveragesResponse.from(sleepLogService.getAverages(userId, days))
    }

    companion object {
        const val USER_ID_HEADER = "X-User-Id"
        const val DEFAULT_USER_ID = "1"
    }
}
