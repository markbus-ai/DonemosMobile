package com.donemosmobile.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JVM tests for the physical-possibility bounds shared with the API. */
public class NumericBoundsTest {

  @Test
  public void blankIsValidBecauseClinicalFieldsAreOptional() {
    assertNull(NumericBounds.check(NumericBounds.HEMOGLOBINA, ""));
    assertNull(NumericBounds.check(NumericBounds.PESO, null));
  }

  @Test
  public void exclusiveMinimumRejectsZeroAndAcceptsMax() {
    assertEquals("Hemoglobina debe ser mayor a 0 y hasta 30 g/dL",
        NumericBounds.check(NumericBounds.HEMOGLOBINA, "0"));
    assertNull(NumericBounds.check(NumericBounds.HEMOGLOBINA, "0,1"));
    assertNull(NumericBounds.check(NumericBounds.HEMOGLOBINA, "30"));
    assertNotNull(NumericBounds.check(NumericBounds.HEMOGLOBINA, "30.01"));
    assertNull(NumericBounds.check(NumericBounds.HEMATOCRITO, "100"));
    assertNotNull(NumericBounds.check(NumericBounds.HEMATOCRITO, "100.5"));
    assertNull(NumericBounds.check(NumericBounds.PESO, "500"));
    assertNotNull(NumericBounds.check(NumericBounds.PESO, "-1"));
  }

  @Test
  public void integerRangesAreInclusive() {
    assertNull(NumericBounds.check(NumericBounds.PLAQUETAS, "1"));
    assertNull(NumericBounds.check(NumericBounds.PLAQUETAS, "5000000"));
    assertEquals("Plaquetas debe estar entre 1 y 5000000",
        NumericBounds.check(NumericBounds.PLAQUETAS, "5000001"));
    assertNull(NumericBounds.check(NumericBounds.SISTOLICA, "350"));
    assertNotNull(NumericBounds.check(NumericBounds.SISTOLICA, "351"));
    assertNull(NumericBounds.check(NumericBounds.DIASTOLICA, "250"));
    assertNotNull(NumericBounds.check(NumericBounds.DIASTOLICA, "0"));
    assertNull(NumericBounds.check(NumericBounds.FRECUENCIA, "1"));
    assertNotNull(NumericBounds.check(NumericBounds.FRECUENCIA, "999"));
  }

  @Test
  public void integerFieldRejectsFractionAndGarbage() {
    assertEquals("Presión sistólica debe ser un número entero",
        NumericBounds.check(NumericBounds.SISTOLICA, "120.5"));
    assertEquals("Hemoglobina: ingresá un número válido",
        NumericBounds.check(NumericBounds.HEMOGLOBINA, "abc"));
  }

  @Test
  public void diastolicMustBeStrictlyLowerThanSystolic() {
    assertNull(NumericBounds.checkPresion(120, 80));
    assertNotNull(NumericBounds.checkPresion(120, 120));
    assertNotNull(NumericBounds.checkPresion(80, 120));
    assertNull(NumericBounds.checkPresion(null, 80));
  }

  @Test
  public void cycleRequiresAllThreeOrNone() {
    assertNull(NumericBounds.checkCycle("", "", ""));
    assertNull(NumericBounds.checkCycle("2", "3", "5"));
    assertEquals("Completá ciclo, serie y donación, o dejá los tres vacíos",
        NumericBounds.checkCycle("2", "", ""));
    assertNotNull(NumericBounds.checkCycle("", "1", "1"));
  }

  @Test
  public void cycleCountersEnforceSerieAndDonacionRanges() {
    assertEquals("Serie debe estar entre 1 y 3", NumericBounds.checkCycle("1", "4", "1"));
    assertEquals("Donación debe estar entre 1 y 5", NumericBounds.checkCycle("1", "1", "6"));
    assertEquals("Ciclo debe ser mayor o igual a 1", NumericBounds.checkCycle("0", "1", "1"));
    assertNotNull(NumericBounds.checkCycle("1", "1", "1.5"));
  }

  @Test
  public void dniAndTelefonoMatchApiRules() {
    assertTrue(NumericBounds.isValidDni("1234567"));
    assertTrue(NumericBounds.isValidDni("12345678"));
    assertFalse(NumericBounds.isValidDni("01234567"));
    assertFalse(NumericBounds.isValidDni("123456"));
    assertFalse(NumericBounds.isValidDni("123456789"));
    assertTrue(NumericBounds.isValidTelefono("+54 (11) 4444-5555"));
    assertFalse(NumericBounds.isValidTelefono("12345"));
    assertFalse(NumericBounds.isValidTelefono("abc123456"));
  }
}
