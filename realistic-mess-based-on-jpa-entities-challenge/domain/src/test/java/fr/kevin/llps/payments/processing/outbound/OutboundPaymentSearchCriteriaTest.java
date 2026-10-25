package fr.kevin.llps.payments.processing.outbound;

import fr.kevin.llps.payments.shared.domain.MalformedValueException;
import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundPaymentSearchCriteriaTest {

    @Test
    void from_should_ignore_blank_filters_and_default_the_limit_to_50() {
        // GIVEN no usable filter and no limit

        // WHEN
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from(" ", null, "", "  ", null, " ",
                null);

        // THEN
        assertThat(criteria).isEqualTo(
                new OutboundPaymentSearchCriteria(null, null, null, null, null, null, new SearchLimit(50)));
    }

    @Test
    void from_should_trim_the_debtor_iban_and_keep_its_case() {
        // GIVEN a padded, lower-case IBAN (exact, case-sensitive match)

        // WHEN
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from(" de89370400440532013000 ", null,
                null, null, null, null, null);

        // THEN
        assertThat(criteria.debtorIban()).isEqualTo("de89370400440532013000");
    }

    @Test
    void from_should_trim_and_upper_case_destination_bic_charges_type_and_status() {
        // GIVEN lower-case, padded values (the destination BIC is then matched as "contains")

        // WHEN
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from(null, " abna ", "sha", " sent",
                null, null, null);

        // THEN
        assertThat(criteria).extracting(OutboundPaymentSearchCriteria::destinationBicContains,
                        OutboundPaymentSearchCriteria::chargesType, OutboundPaymentSearchCriteria::status)
                .containsExactly("ABNA", "SHA", "SENT");
    }

    @Test
    void from_should_parse_execution_date_bounds() {
        // GIVEN padded date bounds

        // WHEN
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from(null, null, null, null,
                " 2026-10-01", "2026-10-31 ", null);

        // THEN
        assertThat(criteria).extracting(OutboundPaymentSearchCriteria::executionFrom,
                OutboundPaymentSearchCriteria::executionTo).containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @ParameterizedTest(name = "[{index}] executionFrom=\"{0}\", executionTo=\"{1}\"")
    @CsvSource({
            "16/10/2026, ",
            ", 2026-10-01T00:00:00",
            "tomorrow, 2026-10-31"
    })
    void from_should_reject_a_malformed_execution_date_bound(String executionFrom, String executionTo) {
        // GIVEN a non-blank bound Date.valueOf cannot parse (legacy: HTTP 500)

        // WHEN / THEN
        assertThatThrownBy(() -> OutboundPaymentSearchCriteria.from(null, null, null, null, executionFrom,
                executionTo, null)).isInstanceOf(MalformedValueException.class);
    }

    // Regression check: the cap comes from SearchLimit.capped (tested there).
    @Test
    void from_should_cap_the_limit_at_200() {
        assertThat(OutboundPaymentSearchCriteria.from(null, null, null, null, null, null, 500).limit())
                .isEqualTo(new SearchLimit(200));
    }
}
