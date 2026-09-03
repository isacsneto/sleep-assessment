package com.noom.interview.fullstack.sleep.service.exception

/** The referenced user does not exist. */
class UserNotFoundException(userId: Long) :
    RuntimeException("User $userId does not exist")

/** The user has no sleep log matching the query (e.g. no last-night log yet). */
class SleepLogNotFoundException(message: String) : RuntimeException(message)

/** A sleep log already exists for the user on the given night. */
class DuplicateSleepLogException(message: String) : RuntimeException(message)

/** The submitted sleep log is semantically invalid (e.g. end not after start). */
class InvalidSleepLogException(message: String) : RuntimeException(message)
