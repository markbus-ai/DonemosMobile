package com.donemosmobile.ui;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Pure numeric parsing for user input, free of Android types so it runs in
 * JVM unit tests. Accepts a decimal comma and rejects anything that is not a
 * plain decimal number instead of silently truncating or saturating it.
 */
public final class NumberParsing {

  private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)");

  private NumberParsing() {}

  /**
   * Trims and normalizes {@code "12,5"} to {@code "12.5"}. Returns null for
   * blank text or anything with exponents, hex, NaN, suffixes or several
   * separators.
   */
  public static BigDecimal parseDecimal(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim().replace(',', '.');
    if (normalized.isEmpty() || !DECIMAL.matcher(normalized).matches()) {
      return null;
    }
    try {
      return new BigDecimal(normalized);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static Double parseDouble(String value) {
    BigDecimal decimal = parseDecimal(value);
    if (decimal == null) {
      return null;
    }
    double result = decimal.doubleValue();
    return Double.isInfinite(result) ? null : result;
  }

  /** Whole number only: {@code "5.5"} and values beyond int range are null. */
  public static Integer parseInt(String value) {
    BigDecimal decimal = parseDecimal(value);
    if (decimal == null) {
      return null;
    }
    try {
      return decimal.stripTrailingZeros().intValueExact();
    } catch (ArithmeticException e) {
      return null;
    }
  }

  public static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
