package fr.kevin.llps.payments.shared.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class ClockConfigurationTest {

    @Test
    void clock_should_be_the_system_clock_in_the_default_time_zone() {
        // GIVEN
        ClockConfiguration configuration = new ClockConfiguration();

        // WHEN
        Clock clock = configuration.clock();

        // THEN
        assertThat(clock).isEqualTo(Clock.systemDefaultZone());
    }
}
