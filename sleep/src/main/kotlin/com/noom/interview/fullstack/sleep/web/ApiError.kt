package com.noom.interview.fullstack.sleep.web

import java.time.OffsetDateTime

/** Consistent error payload returned for all handled failures. */
data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
)
