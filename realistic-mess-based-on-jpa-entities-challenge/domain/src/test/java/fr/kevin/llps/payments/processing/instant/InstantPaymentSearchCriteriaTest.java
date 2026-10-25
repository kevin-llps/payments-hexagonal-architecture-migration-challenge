package fr.kevin.llps.payments.processing.instant;

import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import fr.kevin.llps.payments.shared.domain.SearchLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstantPaymentSearchCriteriaTest {

    @Test
    void from_should_ignore_blank_filters_and_default_the_limit_to_50() {
        // GIVEN no usable filter and no limit

        // WHEN
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(" ", null, "   ", "", null, " ", "",
                null);

        // THEN
        assertThat(criteria).isEqualTo(
                new InstantPaymentSearchCriteria(null, null, null, null, null, null, null, new SearchLimit(50)));
    }

    @ParameterizedTest(name = "[{index}] debtorIban=\"{0}\"")
    @ValueSource(strings = {"DE1", "AAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
    void from_should_reject_an_implausible_debtor_iban(String debtorIban) {
        // GIVEN a debtor IBAN shorter than 15 or longer than 34 characters

        // WHEN / THEN
        assertThatThrownBy(() -> InstantPaymentSearchCriteria.from(null, debtorIban, null, null, null, null, null, null))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessage("Invalid debtor IBAN");
    }

    @ParameterizedTest(name = "[{index}] creditorIban=\"{0}\"")
    @ValueSource(strings = {"X", "AAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "DE89 3704 0044"})
    void from_should_reject_an_implausible_creditor_iban(String creditorIban) {
        // GIVEN a creditor IBAN shorter than 15 or longer than 34 characters once spaces are removed

        // WHEN / THEN
        assertThatThrownBy(() -> InstantPaymentSearchCriteria.from(null, null, creditorIban, null, null, null, null, null))
                .isInstanceOf(InvalidSearchFilterException.class)
                .hasMessage("Invalid creditor IBAN");
    }

    @Test
    void from_should_keep_plausible_ibans_trimmed_and_upper_cased_for_a_contains_match() {
        // GIVEN padded, lower-case IBANs (one with inner spaces, kept as typed)

        // WHEN
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, " de89370400440532013000 ",
                "fr76 3000 6000 0112 3456 7890 189", null, null, null, null, null);

        // THEN
        assertThat(criteria).extracting(InstantPaymentSearchCriteria::debtorIbanContains,
                        InstantPaymentSearchCriteria::creditorIbanContains)
                .containsExactly("DE89370400440532013000", "FR76 3000 6000 0112 3456 7890 189");
    }

    @Test
    void from_should_trim_the_end_to_end_id_and_keep_its_case() {
        // GIVEN

        // WHEN
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(" e2e-inst-0001 ", null, null, null,
                null, null, null, null);

        // THEN
        assertThat(criteria.endToEndId()).isEqualTo("e2e-inst-0001");
    }

    @Test
    void from_should_trim_and_upper_case_status_and_currency() {
        // GIVEN

        // WHEN
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, null, null, " completed ",
                "eur", null, null, null);

        // THEN
        assertThat(criteria).extracting(InstantPaymentSearchCriteria::status, InstantPaymentSearchCriteria::currency)
                .containsExactly("COMPLETED", "EUR");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"2026-10-15T09:15:00", "2026-10-15 09:15:00", " 2026-10-15T09:15:00 "})
    void from_should_parse_execution_bounds_with_t_or_space_separator(String bound) {
        // GIVEN the same timestamp bound for both ends

        // WHEN
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, null, null, null, null, bound,
                bound, null);

        // THEN
        LocalDateTime expected = LocalDateTime.of(2026, 10, 15, 9, 15, 0);
        assertThat(criteria).extracting(InstantPaymentSearchCriteria::executionFrom,
                InstantPaymentSearchCriteria::executionTo).containsExactly(expected, expected);
    }

    @Test
    void from_should_read_a_date_only_execution_range_as_the_whole_day() {
        // GIVEN date-only bounds

        // WHEN
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, null, null, null, null,
                "2026-10-15", "2026-10-15", null);

        // THEN
        assertThat(criteria).extracting(InstantPaymentSearchCriteria::executionFrom,
                        InstantPaymentSearchCriteria::executionTo)
                .containsExactly(LocalDateTime.of(2026, 10, 15, 0, 0, 0), LocalDateTime.of(2026, 10, 15, 23, 59, 59));
    }

    // Regression checks below: these behaviors come from the order of checks, LenientTimestamp and SearchLimit
    // (tested there); they are pinned here because the criteria must keep combining them this way.

    @Test
    void from_should_check_the_debtor_iban_before_the_creditor_iban() {
        assertThatThrownBy(() -> InstantPaymentSearchCriteria.from(null, "DE1", "X", null, null, null, null, null))
                .hasMessage("Invalid debtor IBAN");
    }

    @Test
    void from_should_drop_unparseable_execution_bounds() {
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(null, null, null, null, null,
                "yesterday", "2026/10/01", null);

        assertThat(criteria).extracting(InstantPaymentSearchCriteria::executionFrom,
                InstantPaymentSearchCriteria::executionTo).containsExactly(null, null);
    }

    @Test
    void from_should_cap_the_limit_at_200() {
        assertThat(InstantPaymentSearchCriteria.from(null, null, null, null, null, null, null, 500).limit())
                .isEqualTo(new SearchLimit(200));
    }
}
