package fr.kevin.llps.payments.domain.outbound.query;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.IsoDateBound;
import fr.kevin.llps.payments.domain.sharedkernel.MalformedDateException;
import fr.kevin.llps.payments.domain.sharedkernel.NegativeSearchLimitException;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OutboundPaymentSearchCriteriaTest {

    @Test
    void should_build_every_filter_from_the_raw_values() {
        // GIVEN (values of PaymentRepositoryUnitTest.outboundSearch_appliesParameterizedClauses)
        Integer limit = 20;

        // WHEN
        OutboundPaymentSearchCriteria actual = OutboundPaymentSearchCriteria.from(" DE89370400440532013000 ", " abna ",
                "sha", "sent", "2026-10-01", "2026-10-31", limit);

        // THEN
        assertThat(actual)
                .returns(Optional.of("DE89370400440532013000"), OutboundPaymentSearchCriteria::debtorIban)
                .returns(Optional.of("ABNA"), OutboundPaymentSearchCriteria::destinationBicFragment)
                .returns(Optional.of(new CodeFilter("SHA")), OutboundPaymentSearchCriteria::chargesType)
                .returns(Optional.of(new CodeFilter("SENT")), OutboundPaymentSearchCriteria::status)
                .returns(Optional.of(new IsoDateBound(LocalDate.of(2026, 10, 1))), OutboundPaymentSearchCriteria::executionFrom)
                .returns(Optional.of(new IsoDateBound(LocalDate.of(2026, 10, 31))), OutboundPaymentSearchCriteria::executionTo)
                .returns(new SearchLimit(20), OutboundPaymentSearchCriteria::limit);
    }

    @ParameterizedTest(name = "requested {0} gives {1}")
    @CsvSource({", 50", "2, 2", "500, 200"})
    void should_apply_the_capped_limit(Integer limit, int expected) {
        // GIVEN
        Integer requested = limit;

        // WHEN
        OutboundPaymentSearchCriteria actual = OutboundPaymentSearchCriteria.from(null, null, null, null, null, null, requested);

        // THEN
        assertThat(actual).returns(new SearchLimit(expected), OutboundPaymentSearchCriteria::limit);
    }

    @ParameterizedTest(name = "[{index}] executionFrom={0} executionTo={1}")
    @CsvSource(value = {
            "garbage, NULL",
            "NULL, 2026-02-30x",
            "2026-10-16T00:00:00, NULL"
    }, nullValues = "NULL")
    void should_reject_an_execution_bound_not_in_strict_iso_date_format_with_a_message_less_error(String from,
            String to) {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_reject_outbound_execution_bound_not_in_strict_iso_date_format_with_message_less_error)
        String rawFrom = from;

        // WHEN
        Throwable actual = catchThrowable(() -> OutboundPaymentSearchCriteria.from(null, null, null, null, rawFrom, to, null));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class).hasMessage(null);
    }

    @Test
    void should_keep_the_number_format_message_of_a_non_numeric_date_field() {
        // GIVEN (F-01, same input as the HTTP characterization test)
        String nonNumeric = "20x6-10-16";

        // WHEN
        Throwable actual = catchThrowable(() -> OutboundPaymentSearchCriteria.from(null, null, null, null, null,
                nonNumeric, null));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class).hasMessage("Error at index 2 in: \"20x6\"");
    }

    @Test
    void should_check_the_dates_before_the_limit() {
        // GIVEN (DR-R1: the limit is validated last)
        Integer negativeLimit = -1;

        // WHEN
        Throwable actual = catchThrowable(() -> OutboundPaymentSearchCriteria.from(null, null, null, null, "garbage",
                null, negativeLimit));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class);
    }

    @Test
    void should_reject_a_negative_limit_with_the_legacy_message() {
        // GIVEN
        Integer negativeLimit = -1;

        // WHEN
        Throwable actual = catchThrowable(() -> OutboundPaymentSearchCriteria.from(null, null, null, null, null, null,
                negativeLimit));

        // THEN
        assertThat(actual).isInstanceOf(NegativeSearchLimitException.class).hasMessage("max-results cannot be negative");
    }
}
