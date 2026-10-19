package fr.kevin.llps.payments.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PaymentUtilsTest {

    @Test
    void formatAmount_null_returnsZero() {
        assertThat(PaymentUtils.formatAmount(null)).isEqualTo("0.00");
    }

    @Test
    void formatAmount_roundsHalfUpToTwoDecimals() {
        assertThat(PaymentUtils.formatAmount(new BigDecimal("12.344"))).isEqualTo("12.34");
        assertThat(PaymentUtils.formatAmount(new BigDecimal("12.345"))).isEqualTo("12.35");
    }

    @Test
    void isValidIban_null_returnsFalse() {
        assertThat(PaymentUtils.isValidIban(null)).isFalse();
    }

    @Test
    void isValidIban_tooShort_returnsFalse() {
        assertThat(PaymentUtils.isValidIban("DE12")).isFalse();
    }

    @Test
    void isValidIban_tooLong_returnsFalse() {
        assertThat(PaymentUtils.isValidIban("X".repeat(35))).isFalse();
    }

    @Test
    void isValidIban_withSpaces_stillValid() {
        assertThat(PaymentUtils.isValidIban("DE89 3704 0044 0532 0130 00")).isTrue();
    }

    @Test
    void isValidIban_validLength_returnsTrue() {
        assertThat(PaymentUtils.isValidIban("DE89370400440532013000")).isTrue();
    }

    @Test
    void parseTimestamp_nullOrBlank_returnsNull() {
        assertThat(PaymentUtils.parseTimestamp(null)).isNull();
        assertThat(PaymentUtils.parseTimestamp("   ")).isNull();
    }

    @Test
    void parseTimestamp_valid_returnsTimestamp() {
        assertThat(PaymentUtils.parseTimestamp("2026-10-15T09:15:00").toString())
                .isEqualTo("2026-10-15 09:15:00.0");
    }

    @Test
    void parseTimestamp_invalid_returnsNull() {
        assertThat(PaymentUtils.parseTimestamp("not-a-date")).isNull();
    }

    @Test
    void now_returnsIsoLocalDateTimeString() {
        assertThat(PaymentUtils.now()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void limitOrDefault_null_returnsDefault() {
        assertThat(PaymentUtils.limitOrDefault(null)).isEqualTo(50);
    }

    @Test
    void limitOrDefault_withinBounds_returnsValue() {
        assertThat(PaymentUtils.limitOrDefault(10)).isEqualTo(10);
    }

    @Test
    void limitOrDefault_aboveMax_returnsMax() {
        assertThat(PaymentUtils.limitOrDefault(5000)).isEqualTo(200);
    }

    @Test
    void likePattern_nullOrBlank_returnsNull() {
        assertThat(PaymentUtils.likePattern(null)).isNull();
        assertThat(PaymentUtils.likePattern("  ")).isNull();
    }

    @Test
    void likePattern_trimsAndUppercases() {
        assertThat(PaymentUtils.likePattern(" de89 ")).isEqualTo("%DE89%");
    }

    @Test
    void randomId_returnsUuid() {
        assertThat(PaymentUtils.randomId()).isNotNull();
    }
}
