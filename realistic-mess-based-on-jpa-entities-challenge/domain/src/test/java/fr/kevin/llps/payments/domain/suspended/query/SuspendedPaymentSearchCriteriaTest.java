package fr.kevin.llps.payments.domain.suspended.query;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.NegativeSearchLimitException;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SuspendedPaymentSearchCriteriaTest {

    @Test
    void should_trim_the_debtor_iban_and_keep_its_case() {
        // GIVEN
        String debtorIban = " de89370400440532013000 ";

        // WHEN
        SuspendedPaymentSearchCriteria actual = SuspendedPaymentSearchCriteria.from(debtorIban, null, null, null, null, null);

        // THEN
        assertThat(actual).returns(Optional.of("de89370400440532013000"), SuspendedPaymentSearchCriteria::debtorIban);
    }

    @Test
    void should_ignore_a_blank_debtor_iban() {
        // GIVEN
        String blankDebtorIban = "   ";

        // WHEN
        SuspendedPaymentSearchCriteria actual = SuspendedPaymentSearchCriteria.from(blankDebtorIban, null, null, null, null, null);

        // THEN
        assertThat(actual).returns(Optional.empty(), SuspendedPaymentSearchCriteria::debtorIban);
    }

    @Test
    void should_normalize_hold_reason_and_review_status_codes() {
        // GIVEN
        String holdReason = " aml_threshold ";
        String reviewStatus = "pending";

        // WHEN
        SuspendedPaymentSearchCriteria actual = SuspendedPaymentSearchCriteria.from(null, holdReason, reviewStatus, null, null, null);

        // THEN
        assertThat(actual)
                .returns(Optional.of(new CodeFilter("AML_THRESHOLD")), SuspendedPaymentSearchCriteria::holdReason)
                .returns(Optional.of(new CodeFilter("PENDING")), SuspendedPaymentSearchCriteria::reviewStatus);
    }

    @Test
    void should_parse_held_bounds_in_the_api_timestamp_format() {
        // GIVEN
        String heldFrom = "2026-10-01T00:00:00";
        String heldTo = "2026-10-31T00:00:00";

        // WHEN
        SuspendedPaymentSearchCriteria actual = SuspendedPaymentSearchCriteria.from(null, null, null, heldFrom, heldTo, null);

        // THEN
        assertThat(actual)
                .returns(Optional.of(LocalDateTime.of(2026, 10, 1, 0, 0)), criteria -> criteria.heldFrom().value())
                .returns(Optional.of(LocalDateTime.of(2026, 10, 31, 0, 0)), criteria -> criteria.heldTo().value())
                .returns(false, SuspendedPaymentSearchCriteria::matchesNothing);
    }

    @ParameterizedTest(name = "{0}: heldFrom={1} heldTo={2} matches nothing")
    @CsvSource(value = {
            "date-only lower bound,          2026-10-01,          NULL",
            "date-only upper bound,          NULL,                2026-10-31",
            "unparsable lower bound,         garbage,             NULL",
            "unparsable upper bound,         NULL,                garbage",
            "space-separated lower bound,    2026-10-01 00:00:00, NULL"
    }, nullValues = "NULL")
    void should_match_nothing_when_a_held_bound_is_not_in_the_api_format(String testCase, String heldFrom, String heldTo) {
        // GIVEN
        String from = heldFrom;

        // WHEN
        SuspendedPaymentSearchCriteria actual = SuspendedPaymentSearchCriteria.from(null, null, null, from, heldTo, null);

        // THEN
        assertThat(actual).returns(true, SuspendedPaymentSearchCriteria::matchesNothing);
    }

    @ParameterizedTest(name = "{0}: requested {1} gives {2}")
    @CsvSource({
            "absent limit uses the default, ,    50",
            "limit above 200 is capped,     500, 200",
            "limit within bounds is kept,   2,   2"
    })
    void should_apply_the_capped_limit(String testCase, Integer limit, int expected) {
        // GIVEN
        Integer requested = limit;

        // WHEN
        SuspendedPaymentSearchCriteria actual = SuspendedPaymentSearchCriteria.from(null, null, null, null, null, requested);

        // THEN
        assertThat(actual).returns(new SearchLimit(expected), SuspendedPaymentSearchCriteria::limit);
    }

    @Test
    void should_reject_a_negative_limit_even_when_a_held_bound_matches_nothing() {
        // GIVEN
        String unparsableHeldFrom = "garbage";

        // WHEN
        Throwable actual = catchThrowable(() -> SuspendedPaymentSearchCriteria.from(null, null, null, unparsableHeldFrom, null, -1));

        // THEN
        assertThat(actual).isInstanceOf(NegativeSearchLimitException.class).hasMessage("max-results cannot be negative");
    }
}
