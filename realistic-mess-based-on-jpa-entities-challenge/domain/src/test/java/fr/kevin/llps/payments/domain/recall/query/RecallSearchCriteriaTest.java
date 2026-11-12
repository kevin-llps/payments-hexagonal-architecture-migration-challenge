package fr.kevin.llps.payments.domain.recall.query;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.kevin.llps.payments.domain.recall.UnknownRecallTypeException;
import fr.kevin.llps.payments.domain.sharedkernel.CodeFilter;
import fr.kevin.llps.payments.domain.sharedkernel.InvalidPaymentIdException;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.sharedkernel.SearchLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RecallSearchCriteriaTest {

    @ParameterizedTest(name = "recallType \"{0}\" is accepted and filters on {1}")
    @CsvSource({
            "R-REJ,     R-REJ",
            "R-COR,     R-COR",
            "R-UPD,     R-UPD",
            "' r-rej ', R-REJ",
            "r-cor,     R-COR",
            "R-upd,     R-UPD"
    })
    void should_accept_allowed_recall_types_trimmed_and_case_insensitive(String recallType, String expected) {
        // GIVEN (remapped from PaymentServiceUnitTest.should_accept_allowed_recall_types_trimmed_and_case_insensitive)
        String raw = recallType;

        // WHEN
        RecallSearchCriteria actual = RecallSearchCriteria.from(raw, null, null, null, null, null, null);

        // THEN
        assertThat(actual).returns(Optional.of(new CodeFilter(expected)), RecallSearchCriteria::recallType);
    }

    @ParameterizedTest(name = "{0}: \"{1}\" is rejected with \"{2}\"")
    @CsvSource(delimiter = '|', value = {
            "upper-case unknown type (remapped recallSearch_invalidType_throws) | NOPE    | Unknown recall type: NOPE",
            "raw untrimmed input in the message (remapped should_reject_unknown_recall_type_with_raw_input_in_message) | ' nope ' | 'Unknown recall type:  nope '"
    })
    void should_reject_an_unknown_recall_type_with_the_raw_input_in_the_message(String testCase, String recallType,
            String expectedMessage) {
        // GIVEN
        String raw = recallType;

        // WHEN
        Throwable actual = catchThrowable(() -> RecallSearchCriteria.from(raw, null, null, null, null, null, null));

        // THEN
        assertThat(actual).isInstanceOf(UnknownRecallTypeException.class).hasMessage(expectedMessage);
    }

    @Test
    void should_ignore_a_blank_recall_type_filter() {
        // GIVEN (remapped from PaymentServiceUnitTest.should_ignore_blank_recall_type_filter)
        String blank = "  ";

        // WHEN
        RecallSearchCriteria actual = RecallSearchCriteria.from(blank, null, null, null, null, null, null);

        // THEN
        assertThat(actual).returns(Optional.empty(), RecallSearchCriteria::recallType);
    }

    @Test
    void should_reject_a_malformed_original_payment_id_with_the_uuid_message() {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_reject_malformed_original_payment_id_filter_with_invalid_uuid_message)
        String malformed = "xyz";

        // WHEN
        Throwable actual = catchThrowable(() -> RecallSearchCriteria.from(null, malformed, null, null, null, null, null));

        // THEN
        assertThat(actual).isInstanceOf(InvalidPaymentIdException.class).hasMessage("Invalid UUID string: xyz");
    }

    @Test
    void should_check_the_recall_type_before_the_original_payment_id() {
        // GIVEN (DR-R1 legacy error order)
        String unknownType = "NOPE";

        // WHEN
        Throwable actual = catchThrowable(() -> RecallSearchCriteria.from(unknownType, "xyz", null, null, null, null, null));

        // THEN
        assertThat(actual).isInstanceOf(UnknownRecallTypeException.class).hasMessage("Unknown recall type: NOPE");
    }

    @Test
    void should_trim_the_original_payment_id_and_normalize_the_codes() {
        // GIVEN
        String originalPaymentId = " 00000000-0000-0000-0000-000000000401 ";

        // WHEN
        RecallSearchCriteria actual = RecallSearchCriteria.from(null, originalPaymentId, "outbound", "ac04", "initiated",
                null, null);

        // THEN
        assertThat(actual)
                .returns(Optional.of(PaymentId.parse("00000000-0000-0000-0000-000000000401")), RecallSearchCriteria::originalPaymentId)
                .returns(Optional.of(new CodeFilter("OUTBOUND")), RecallSearchCriteria::originalPaymentType)
                .returns(Optional.of(new CodeFilter("AC04")), RecallSearchCriteria::reasonCode)
                .returns(Optional.of(new CodeFilter("INITIATED")), RecallSearchCriteria::status);
    }

    @ParameterizedTest(name = "{0}: from={1} to={2}")
    @CsvSource(value = {
            "date-only bounds both start the day,    2026-10-16,          2026-10-16,          2026-10-16T00:00,    2026-10-16T00:00",
            "full timestamps are kept,               2026-10-16T12:00:00, 2026-10-16T12:00:00, 2026-10-16T12:00,    2026-10-16T12:00",
            "unparsable bounds are ignored,          garbage,             garbage,             NULL,                NULL"
    }, nullValues = "NULL")
    void should_parse_initiated_bounds_with_the_lenient_grammar_and_start_of_day(String testCase, String from,
            String to, LocalDateTime expectedFrom, LocalDateTime expectedTo) {
        // GIVEN
        String rawFrom = from;

        // WHEN
        RecallSearchCriteria actual = RecallSearchCriteria.from(null, null, null, null, null, rawFrom, to);

        // THEN
        assertThat(actual)
                .returns(Optional.ofNullable(expectedFrom), criteria -> criteria.initiatedFrom().value())
                .returns(Optional.ofNullable(expectedTo), criteria -> criteria.initiatedTo().value());
    }

    @Test
    void should_always_use_the_fixed_limit_of_fifty() {
        // GIVEN (BR-RCL-05: the request limit is never passed in)
        int expected = 50;

        // WHEN
        RecallSearchCriteria actual = RecallSearchCriteria.from(null, null, null, null, null, null, null);

        // THEN
        assertThat(actual).returns(new SearchLimit(expected), RecallSearchCriteria::limit);
    }
}
