package fr.kevin.llps.payments.domain.sharedkernel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiDateTimeFormatTest {

    @Test
    void should_expose_the_legacy_api_rendering_patterns() {
        // GIVEN
        String expectedTimestampPattern = "yyyy-MM-dd'T'HH:mm:ss";

        // WHEN
        String actualTimestampPattern = ApiDateTimeFormat.TIMESTAMP_PATTERN;

        // THEN
        assertThat(actualTimestampPattern).isEqualTo(expectedTimestampPattern);
        assertThat(ApiDateTimeFormat.DATE_PATTERN).isEqualTo("yyyy-MM-dd");
    }
}
