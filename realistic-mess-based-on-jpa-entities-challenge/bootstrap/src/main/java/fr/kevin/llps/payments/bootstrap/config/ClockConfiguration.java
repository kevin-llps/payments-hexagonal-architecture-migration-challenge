package fr.kevin.llps.payments.bootstrap.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

    /** The JVM default zone, as the legacy {@code System.currentTimeMillis()} timestamps used. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
