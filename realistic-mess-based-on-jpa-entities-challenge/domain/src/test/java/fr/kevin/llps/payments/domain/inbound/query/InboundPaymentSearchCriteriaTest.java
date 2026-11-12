package fr.kevin.llps.payments.domain.inbound.query;

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

class InboundPaymentSearchCriteriaTest {

    @Test
    void should_build_every_filter_from_the_raw_values() {
        // GIVEN (values of PaymentRepositoryUnitTest.inboundSearch_appliesFiltersAndDefaultLimit)
        String valueDateTo = " 2026-10-31 ";

        // WHEN
        InboundPaymentSearchCriteria actual = InboundPaymentSearchCriteria.from(" DE89370400440532013000 ", " bnpa ",
                "settled", "eur", "2026-10-01", valueDateTo, null);

        // THEN
        assertThat(actual)
                .returns(Optional.of("DE89370400440532013000"), InboundPaymentSearchCriteria::creditorIban)
                .returns(Optional.of("BNPA"), InboundPaymentSearchCriteria::originBicFragment)
                .returns(Optional.of(new CodeFilter("SETTLED")), InboundPaymentSearchCriteria::status)
                .returns(Optional.of(new CodeFilter("EUR")), InboundPaymentSearchCriteria::currency)
                .returns(Optional.of(new IsoDateBound(LocalDate.of(2026, 10, 1))), InboundPaymentSearchCriteria::valueDateFrom)
                .returns(Optional.of(new IsoDateBound(LocalDate.of(2026, 10, 31))), InboundPaymentSearchCriteria::valueDateTo)
                .returns(new SearchLimit(50), InboundPaymentSearchCriteria::limit);
    }

    @ParameterizedTest(name = "requested {0} gives {1} (not capped)")
    @CsvSource({", 50", "201, 201", "0, 0"})
    void should_default_the_limit_to_fifty_without_capping_it(Integer limit, int expected) {
        // GIVEN (limits of PaymentRepositoryUnitTest.should_default_inbound_limit_to_50_without_capping_at_200)
        Integer requested = limit;

        // WHEN
        InboundPaymentSearchCriteria actual = InboundPaymentSearchCriteria.from(null, null, null, null, null, null, requested);

        // THEN
        assertThat(actual).returns(new SearchLimit(expected), InboundPaymentSearchCriteria::limit);
    }

    @ParameterizedTest(name = "[{index}] valueDateFrom={0} valueDateTo={1}")
    @CsvSource(value = {
            "2026-13-45, NULL",
            "NULL, garbage",
            "2026-10-15T00:00:00, NULL"
    }, nullValues = "NULL")
    void should_reject_a_value_date_bound_not_in_strict_iso_date_format_with_a_message_less_error(String from,
            String to) {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_reject_inbound_value_date_bound_not_in_strict_iso_date_format_with_message_less_error)
        String rawFrom = from;

        // WHEN
        Throwable actual = catchThrowable(() -> InboundPaymentSearchCriteria.from(null, null, null, null, rawFrom, to, null));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class).hasMessage(null);
    }

    @Test
    void should_keep_the_number_format_message_of_a_non_numeric_date_field() {
        // GIVEN (F-01)
        String nonNumeric = "2026-1x-05";

        // WHEN
        Throwable actual = catchThrowable(() -> InboundPaymentSearchCriteria.from(null, null, null, null, nonNumeric,
                null, null));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class).hasMessage("Error at index 1 in: \"1x\"");
    }

    @Test
    void should_parse_the_lower_bound_before_the_upper_bound() {
        // GIVEN (DR-R1 legacy error order)
        String from = "2026-1x-05";
        String to = "20x6-10-16";

        // WHEN
        Throwable actual = catchThrowable(() -> InboundPaymentSearchCriteria.from(null, null, null, null, from, to, null));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class).hasMessage("Error at index 1 in: \"1x\"");
    }

    @Test
    void should_check_the_dates_before_the_limit() {
        // GIVEN (DR-R1: the limit is validated last)
        Integer negativeLimit = -1;

        // WHEN
        Throwable actual = catchThrowable(() -> InboundPaymentSearchCriteria.from(null, null, null, null, "garbage",
                null, negativeLimit));

        // THEN
        assertThat(actual).isInstanceOf(MalformedDateException.class);
    }

    @Test
    void should_reject_a_negative_limit_with_the_legacy_message() {
        // GIVEN
        Integer negativeLimit = -1;

        // WHEN
        Throwable actual = catchThrowable(() -> InboundPaymentSearchCriteria.from(null, null, null, null, null, null,
                negativeLimit));

        // THEN
        assertThat(actual).isInstanceOf(NegativeSearchLimitException.class).hasMessage("max-results cannot be negative");
    }
}
