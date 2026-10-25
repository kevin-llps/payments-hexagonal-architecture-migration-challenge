package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecallTypeTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource({
            "R-REJ, R-REJ",
            "r-cor, R-COR",
            "' R-UPD ', R-UPD"
    })
    void validate_filter_should_return_the_normalized_type_when_it_is_known(String raw, String expected) {
        // GIVEN a known recall type, any case, possibly padded

        // WHEN
        String recallType = RecallType.validateFilter(raw);

        // THEN
        assertThat(recallType).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"NOPE", "R-XXX", " r-xxx "})
    void validate_filter_should_reject_an_unknown_type_echoing_the_requested_value(String raw) {
        // GIVEN an unknown recall type

        // WHEN / THEN
        assertThatThrownBy(() -> RecallType.validateFilter(raw))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessage("Unknown recall type: " + raw);
    }
}
