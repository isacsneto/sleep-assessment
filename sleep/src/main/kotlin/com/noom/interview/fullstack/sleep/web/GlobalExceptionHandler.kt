package com.noom.interview.fullstack.sleep.web

import com.noom.interview.fullstack.sleep.service.exception.DuplicateSleepLogException
import com.noom.interview.fullstack.sleep.service.exception.InvalidSleepLogException
import com.noom.interview.fullstack.sleep.service.exception.SleepLogNotFoundException
import com.noom.interview.fullstack.sleep.service.exception.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/** Translates domain and request errors into consistent HTTP responses. */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException::class, SleepLogNotFoundException::class)
    fun handleNotFound(e: RuntimeException) = build(HttpStatus.NOT_FOUND, e.message)

    @ExceptionHandler(DuplicateSleepLogException::class)
    fun handleConflict(e: DuplicateSleepLogException) = build(HttpStatus.CONFLICT, e.message)

    @ExceptionHandler(InvalidSleepLogException::class, IllegalArgumentException::class)
    fun handleBadRequest(e: RuntimeException) = build(HttpStatus.BAD_REQUEST, e.message)

    /** Malformed JSON, wrong types, or missing required fields. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(e: HttpMessageNotReadableException) =
        build(HttpStatus.BAD_REQUEST, "Malformed or invalid request body")

    private fun build(status: HttpStatus, message: String?): ResponseEntity<ApiError> {
        val body = ApiError(
            status = status.value(),
            error = status.reasonPhrase,
            message = message ?: status.reasonPhrase,
        )
        return ResponseEntity.status(status).body(body)
    }
}
