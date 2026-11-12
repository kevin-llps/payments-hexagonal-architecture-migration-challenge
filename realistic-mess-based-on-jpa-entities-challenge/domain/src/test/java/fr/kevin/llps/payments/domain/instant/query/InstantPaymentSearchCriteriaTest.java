package fr.kevin.llps.payments.domain.instant.query;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.kevin.llps.payments.domain.instant.InvalidIbanFilterException;
import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.NegativeSearchLimitException;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InstantPaymentSearchCriteriaTest {

    @ParameterizedTest(name = "{0}: debtorIban={1} creditorIban={2} fails with \"{3}\"")
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
            "invalid debtor IBAN (remapped instantSearch_invalidDebtorIban_throws)                   | DE1                    | NULL | Invalid debtor IBAN",
            "invalid creditor IBAN (remapped instantSearch_invalidCreditorIban_throws)               | NULL                   | X    | Invalid creditor IBAN",
            "debtor reported first (remapped should_report_debtor_iban_first_when_both_iban_filters_are_invalid) | DE1    | FR1  | Invalid debtor IBAN",
            "valid debtor, invalid creditor (remapped should_not_query_repository_when_creditor_iban_is_invalid) | DE89370400440532013000 | FR1 | Invalid creditor IBAN"
    })
    void should_reject_an_invalid_iban_filter_in_the_legacy_order(String testCase, String debtorIban,
            String creditorIban, String expectedMessage) {
        // GIVEN
        String debtor = debtorIban;

        // WHEN
        Throwable actual = catchThrowable(() -> InstantPaymentSearchCriteria.from(null, debtor, creditorIban, null, null,
                null, null, null));

        // THEN
        assertThat(actual).isInstanceOf(InvalidIbanFilterException.class).hasMessage(expectedMessage);
    }

    @Test
    void should_check_the_ibans_before_the_limit() {
        // GIVEN (DR-R1: the limit is validated last)
        Integer negativeLimit = -1;

        // WHEN
        Throwable actual = catchThrowable(() -> InstantPaymentSearchCriteria.from(null, "DE1", null, null, null, null,
                null, negativeLimit));

        // THEN
        assertThat(actual).isInstanceOf(InvalidIbanFilterException.class).hasMessage("Invalid debtor IBAN");
    }

    @Test
    void should_reject_a_negative_limit_with_the_legacy_message() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_reject_negative_instant_limit_with_illegal_argument_root_cause)
        Integer negativeLimit = -1;

        // WHEN
        Throwable actual = catchThrowable(() -> InstantPaymentSearchCriteria.from(null, null, null, null, null, null,
                null, negativeLimit));

        // THEN
        assertThat(actual).isInstanceOf(NegativeSearchLimitException.class).hasMessage("max-results cannot be negative");
    }

    @Test
    void should_ignore_blank_iban_filters() {
        // GIVEN (remapped from PaymentServiceUnitTest.should_ignore_blank_iban_filters_and_delegate_to_repository)
        String blankDebtor = "   ";
        String emptyCreditor = "";

        // WHEN
        InstantPaymentSearchCriteria actual = InstantPaymentSearchCriteria.from(null, blankDebtor, emptyCreditor, null,
                null, null, null, null);

        // THEN
        assertThat(actual)
                .returns(Optional.empty(), InstantPaymentSearchCriteria::debtorIbanFragment)
                .returns(Optional.empty(), InstantPaymentSearchCriteria::creditorIbanFragment);
    }

    @Test
    void should_accept_an_iban_filter_with_spaces_and_keep_its_inner_spaces() {
        // GIVEN (remapped from PaymentServiceUnitTest.should_accept_iban_filter_containing_spaces_when_length_without_spaces_is_valid; N-03)
        String spacedIban = "DE89 3704 0044 0532 0130 00";

        // WHEN
        InstantPaymentSearchCriteria actual = InstantPaymentSearchCriteria.from(null, spacedIban, null, null, null,
                null, null, null);

        // THEN
        assertThat(actual).returns(Optional.of("DE89 3704 0044 0532 0130 00"), InstantPaymentSearchCriteria::debtorIbanFragment);
    }

    @Test
    void should_build_every_filter_from_the_raw_values() {
        // GIVEN (values of PaymentRepositoryUnitTest.instantSearch_appliesAllFilters_andFormatsExecutionDatetime)
        String executionTo = "2026-10-31";

        // WHEN
        InstantPaymentSearchCriteria actual = InstantPaymentSearchCriteria.from(" E2E-T1 ", "DE89370400440532013000",
                " fr7630006000011234567890189 ", "completed", "eur", "2026-10-01T00:00:00", executionTo, 10);

        // THEN
        assertThat(actual)
                .returns(Optional.of("E2E-T1"), InstantPaymentSearchCriteria::endToEndId)
                .returns(Optional.of("DE89370400440532013000"), InstantPaymentSearchCriteria::debtorIbanFragment)
                .returns(Optional.of("FR7630006000011234567890189"), InstantPaymentSearchCriteria::creditorIbanFragment)
                .returns(Optional.of(new CodeFilter("COMPLETED")), InstantPaymentSearchCriteria::status)
                .returns(Optional.of(new CodeFilter("EUR")), InstantPaymentSearchCriteria::currency)
                .returns(Optional.of(LocalDateTime.of(2026, 10, 1, 0, 0)), criteria -> criteria.executionFrom().value())
                .returns(Optional.of(LocalDateTime.of(2026, 10, 31, 23, 59, 59)), criteria -> criteria.executionTo().value())
                .returns(new SearchLimit(10), InstantPaymentSearchCriteria::limit);
    }

    @ParameterizedTest(name = "{0}: from={1} to={2}")
    @CsvSource(nullValues = "NULL", value = {
            "unparsable bounds are ignored, garbage,    garbage,    NULL,             NULL",
            "date-only bounds,              2026-10-16, 2026-10-15, 2026-10-16T00:00, 2026-10-15T23:59:59"
    })
    void should_parse_execution_bounds_with_start_and_end_of_day(String testCase, String from, String to,
            LocalDateTime expectedFrom, LocalDateTime expectedTo) {
        // GIVEN
        String rawFrom = from;

        // WHEN
        InstantPaymentSearchCriteria actual = InstantPaymentSearchCriteria.from(null, null, null, null, null, rawFrom,
                to, null);

        // THEN
        assertThat(actual)
                .returns(Optional.ofNullable(expectedFrom), criteria -> criteria.executionFrom().value())
                .returns(Optional.ofNullable(expectedTo), criteria -> criteria.executionTo().value());
    }

    @ParameterizedTest(name = "requested {0} gives {1}")
    @CsvSource({", 50", "0, 0", "500, 200"})
    void should_apply_the_capped_limit(Integer limit, int expected) {
        // GIVEN
        Integer requested = limit;

        // WHEN
        InstantPaymentSearchCriteria actual = InstantPaymentSearchCriteria.from(null, null, null, null, null, null,
                null, requested);

        // THEN
        assertThat(actual).returns(new SearchLimit(expected), InstantPaymentSearchCriteria::limit);
    }
}
