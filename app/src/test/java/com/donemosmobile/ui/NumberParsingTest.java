package com.donemosmobile.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** JVM tests for the user-input number parsing used by every numeric field. */
public class NumberParsingTest {

  @Test
  public void parseDoubleAcceptsDecimalComma() {
    assertEquals(12.5, NumberParsing.parseDouble("12,5"), 0.0);
    assertEquals(12.5, NumberParsing.parseDouble(" 12.5 "), 0.0);
    assertEquals(0.5, NumberParsing.parseDouble(",5"), 0.0);
  }

  @Test
  public void parseDoubleRejectsNonPlainNumbers() {
    assertNull(NumberParsing.parseDouble(""));
    assertNull(NumberParsing.parseDouble("   "));
    assertNull(NumberParsing.parseDouble(null));
    assertNull(NumberParsing.parseDouble("abc"));
    assertNull(NumberParsing.parseDouble("1.2.3"));
    assertNull(NumberParsing.parseDouble("1,2,3"));
    assertNull(NumberParsing.parseDouble("NaN"));
    assertNull(NumberParsing.parseDouble("Infinity"));
    assertNull(NumberParsing.parseDouble("12f"));
    assertNull(NumberParsing.parseDouble("1e5"));
    assertNull(NumberParsing.parseDouble("0x1A"));
  }

  @Test
  public void parseIntAcceptsWholeNumbers() {
    assertEquals(Integer.valueOf(120), NumberParsing.parseInt("120"));
    assertEquals(Integer.valueOf(5), NumberParsing.parseInt("5,0"));
    assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberParsing.parseInt("2147483647"));
  }

  @Test
  public void parseIntRejectsFractionsInsteadOfTruncating() {
    assertNull(NumberParsing.parseInt("5.5"));
    assertNull(NumberParsing.parseInt("5,5"));
  }

  @Test
  public void parseIntRejectsOverflowInsteadOfSaturating() {
    assertNull(NumberParsing.parseInt("2147483648"));
    assertNull(NumberParsing.parseInt("99999999999999999999"));
    assertNull(NumberParsing.parseInt("-2147483649"));
  }
}
