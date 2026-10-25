package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import fr.kevin.llps.payments.shared.domain.MalformedValueException;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecallSearchCriteriaTest {

    @Test
    void from_should_ignore_blank_filters_and_always_limit_to_50() {
        // GIVEN no usable filter and a requested limit
        Integer requestedLimit = 5;

        // WHEN
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, "  ", "", null, " ", null, "", requestedLimit);

        // THEN
        assertThat(criteria).isEqualTo(
                new RecallSearchCriteria(null, null, null, null, null, null, null, new SearchLimit(50)));
    }

    @Test
    void from_should_keep_a_known_recall_type_normalized() {
        // GIVEN
        String recallType = " r-cor ";

        // WHEN
        RecallSearchCriteria criteria = RecallSearchCriteria.from(recallType, null, null, null, null, null, null, null);

        // THEN
        assertThat(criteria.recallType()).isEqualTo("R-COR");
    }

    @Test
    void from_should_parse_the_trimmed_original_payment_id() {
        // GIVEN
        String originalPaymentId = " 00000000-0000-0000-0000-000000000401 ";

        // WHEN
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, originalPaymentId, null, null, null, null, null, null);

        // THEN
        assertThat(criteria.originalPaymentId()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000401"));
    }

    @Test
    void from_should_trim_and_upper_case_original_payment_type_reason_code_and_status() {
        // GIVEN lower-case, padded codes

        // WHEN
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, null, " outbound ", "ac04", " initiated", null, null, null);

        // THEN
        assertThat(criteria).extracting(RecallSearchCriteria::originalPaymentType, RecallSearchCriteria::reasonCode,
                RecallSearchCriteria::status).containsExactly("OUTBOUND", "AC04", "INITIATED");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"2026-10-16T12:00:00", "2026-10-16 12:00:00", " 2026-10-16T12:00:00 "})
    void from_should_parse_initiated_bounds_with_t_or_space_separator(String bound) {
        // GIVEN the same timestamp bound for both ends

        // WHEN
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, null, null, null, null, bound, bound, null);

        // THEN
        LocalDateTime expected = LocalDateTime.of(2026, 10, 16, 12, 0, 0);
        assertThat(criteria).extracting(RecallSearchCriteria::initiatedFrom, RecallSearchCriteria::initiatedTo)
                .containsExactly(expected, expected);
    }

    @Test
    void from_should_read_a_date_only_bound_as_midnight_for_both_ends() {
        // GIVEN date-only bounds (initiatedTo therefore excludes the rest of that day)

        // WHEN
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, null, null, null, null, "2026-10-01", "2026-10-16", null);

        // THEN
        assertThat(criteria).extracting(RecallSearchCriteria::initiatedFrom, RecallSearchCriteria::initiatedTo)
                .containsExactly(LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 16, 0, 0));
    }

    // Regression checks below: these behaviors come from RecallType, PaymentId and LenientTimestamp (tested there);
    // they are pinned here because the criteria must keep combining them this way.

    @Test
    void from_should_reject_an_unknown_recall_type() {
        assertThatThrownBy(() -> RecallSearchCriteria.from("NOPE", null, null, null, null, null, null, null))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessage("Unknown recall type: NOPE");
    }

    @Test
    void from_should_reject_a_malformed_original_payment_id() {
        assertThatThrownBy(() -> RecallSearchCriteria.from(null, "not-a-uuid", null, null, null, null, null, null))
                .isInstanceOf(MalformedValueException.class);
    }

    @Test
    void from_should_check_the_recall_type_before_the_original_payment_id() {
        assertThatThrownBy(() -> RecallSearchCriteria.from("NOPE", "not-a-uuid", null, null, null, null, null, null))
                .isInstanceOf(InvalidSearchFilterException.class);
    }

    @Test
    void from_should_drop_unparseable_initiated_bounds() {
        RecallSearchCriteria criteria = RecallSearchCriteria.from(null, null, null, null, null, "last week", "16/10/2026", null);

        assertThat(criteria).extracting(RecallSearchCriteria::initiatedFrom, RecallSearchCriteria::initiatedTo)
                .containsExactly(null, null);
    }
}
