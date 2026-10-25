package fr.kevin.llps.payments.processing.inbound;

import fr.kevin.llps.payments.shared.domain.MalformedValueException;
import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InboundPaymentSearchCriteriaTest {

    @Test
    void from_should_ignore_blank_filters_and_default_the_limit_to_50() {
        // GIVEN no usable filter and no limit

        // WHEN
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from(" ", "", null, "  ", null, " ", null);

        // THEN
        assertThat(criteria).isEqualTo(
                new InboundPaymentSearchCriteria(null, null, null, null, null, null, new SearchLimit(50)));
    }

    @Test
    void from_should_trim_the_creditor_iban_and_keep_its_case() {
        // GIVEN a padded, lower-case IBAN (exact, case-sensitive match)

        // WHEN
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from(" de89370400440532013000 ", null,
                null, null, null, null, null);

        // THEN
        assertThat(criteria.creditorIban()).isEqualTo("de89370400440532013000");
    }

    @Test
    void from_should_trim_and_upper_case_origin_bic_status_and_currency() {
        // GIVEN lower-case, padded values (the origin BIC is then matched as "contains")

        // WHEN
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from(null, " bnpa ", "settled", " eur",
                null, null, null);

        // THEN
        assertThat(criteria).extracting(InboundPaymentSearchCriteria::originBicContains,
                        InboundPaymentSearchCriteria::status, InboundPaymentSearchCriteria::currency)
                .containsExactly("BNPA", "SETTLED", "EUR");
    }

    @Test
    void from_should_parse_value_date_bounds() {
        // GIVEN padded date bounds

        // WHEN
        InboundPaymentSearchCriteria criteria = InboundPaymentSearchCriteria.from(null, null, null, null,
                " 2026-10-01", "2026-10-31 ", null);

        // THEN
        assertThat(criteria).extracting(InboundPaymentSearchCriteria::valueDateFrom,
                InboundPaymentSearchCriteria::valueDateTo).containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    }

    @ParameterizedTest(name = "[{index}] valueDateFrom=\"{0}\", valueDateTo=\"{1}\"")
    @CsvSource({
            "16/10/2026, ",
            ", 2026-10-01T00:00:00",
            "yesterday, 2026-10-31"
    })
    void from_should_reject_a_malformed_value_date_bound(String valueDateFrom, String valueDateTo) {
        // GIVEN a non-blank bound Date.valueOf cannot parse (legacy: HTTP 500)

        // WHEN / THEN
        assertThatThrownBy(() -> InboundPaymentSearchCriteria.from(null, null, null, null, valueDateFrom, valueDateTo,
                null)).isInstanceOf(MalformedValueException.class);
    }

    // Regression check: the absence of cap comes from SearchLimit.uncapped (tested there).
    @Test
    void from_should_not_cap_the_limit() {
        assertThat(InboundPaymentSearchCriteria.from(null, null, null, null, null, null, 500).limit())
                .isEqualTo(new SearchLimit(500));
    }
}
