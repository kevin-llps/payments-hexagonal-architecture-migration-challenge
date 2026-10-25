package fr.kevin.llps.payments.compliancehold;

import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SuspendedPaymentSearchCriteriaTest {

    @Test
    void from_should_ignore_blank_filters_and_default_the_limit_to_50() {
        // GIVEN no usable filter and no limit

        // WHEN
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from(" ", null, "", "  ", null, null);

        // THEN
        assertThat(criteria).isEqualTo(
                new SuspendedPaymentSearchCriteria(null, null, null, null, null, false, new SearchLimit(50)));
    }

    @Test
    void from_should_trim_the_debtor_iban_and_keep_its_case() {
        // GIVEN a padded, lower-case IBAN (exact, case-sensitive match)

        // WHEN
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from("  de89370400440532013000 ",
                null, null, null, null, null);

        // THEN
        assertThat(criteria.debtorIban()).isEqualTo("de89370400440532013000");
    }

    @Test
    void from_should_trim_and_upper_case_hold_reason_and_review_status() {
        // GIVEN lower-case, padded codes

        // WHEN
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from(null, " aml_threshold ",
                "pending", null, null, null);

        // THEN
        assertThat(criteria).extracting(SuspendedPaymentSearchCriteria::holdReason,
                SuspendedPaymentSearchCriteria::reviewStatus).containsExactly("AML_THRESHOLD", "PENDING");
    }

    @Test
    void from_should_parse_held_bounds_in_the_legacy_timestamp_format() {
        // GIVEN padded yyyy-MM-dd'T'HH:mm:ss bounds

        // WHEN
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from(null, null, null,
                " 2026-10-01T00:00:00", "2026-10-31T23:59:59 ", null);

        // THEN
        assertThat(criteria).extracting(SuspendedPaymentSearchCriteria::heldFrom, SuspendedPaymentSearchCriteria::heldTo,
                SuspendedPaymentSearchCriteria::unsatisfiable)
                .containsExactly(LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 23, 59, 59), false);
    }

    @ParameterizedTest(name = "[{index}] heldFrom=\"{0}\", heldTo=\"{1}\"")
    @CsvSource({
            "2026-10-01, ",
            ", 2026-10-31 23:59:59",
            "last week, 2026-10-31T23:59:59"
    })
    void from_should_be_unsatisfiable_when_a_held_bound_is_not_in_the_legacy_timestamp_format(String heldFrom,
                                                                                           String heldTo) {
        // GIVEN at least one non-blank bound the legacy format cannot parse

        // WHEN
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from(null, null, null,
                heldFrom, heldTo, null);

        // THEN
        assertThat(criteria.unsatisfiable()).isTrue();
    }

    // Regression check: the cap comes from SearchLimit.capped (tested there).
    @Test
    void from_should_cap_the_limit_at_200() {
        assertThat(SuspendedPaymentSearchCriteria.from(null, null, null, null, null, 201).limit())
                .isEqualTo(new SearchLimit(200));
    }
}
