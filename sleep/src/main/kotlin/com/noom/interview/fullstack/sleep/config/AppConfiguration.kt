package com.noom.interview.fullstack.sleep.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class AppConfiguration {
    /**
     * A single injectable clock so time-dependent logic (e.g. the averages
     * window) can be driven by a fixed clock in tests.
     */
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
