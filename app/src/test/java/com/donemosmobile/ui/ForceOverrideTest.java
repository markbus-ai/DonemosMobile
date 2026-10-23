package com.donemosmobile.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/** JVM tests for the audited override rules (motivo + forcible 409 codes). */
public class ForceOverrideTest {

  private static String repeat(char c, int times) {
    char[] chars = new char[times];
    Arrays.fill(chars, c);
    return new String(chars);
  }

  @Test
  public void motivoIsMandatory() {
    assertEquals("Escribí el motivo de la autorización", ForceOverride.checkMotivo(null));
    assertEquals("Escribí el motivo de la autorización", ForceOverride.checkMotivo("   "));
  }

  @Test
  public void motivoNeedsAtLeastFiveCharsAfterTrim() {
    assertEquals("El motivo debe tener al menos 5 caracteres",
        ForceOverride.checkMotivo("  abcd  "));
    assertNull(ForceOverride.checkMotivo("abcde"));
  }

  @Test
  public void motivoAcceptsUpToFiveHundredChars() {
    assertNull(ForceOverride.checkMotivo(repeat('a', 500)));
    assertEquals("El motivo no puede superar los 500 caracteres",
        ForceOverride.checkMotivo(repeat('a', 501)));
  }

  @Test
  public void hardLimitsAreNotForcible() {
    assertFalse(ForceOverride.isForcible("HORARIO_OCUPADO"));
    assertFalse(ForceOverride.isForcible("CAPACIDAD_EXCEDIDA"));
    assertFalse(ForceOverride.isForcible(null));
    assertFalse(ForceOverride.isForcible(""));
  }

  @Test
  public void ruleWarningsAreForcible() {
    assertTrue(ForceOverride.isForcible("INTERVALO_MINIMO"));
    assertTrue(ForceOverride.isForcible("TURNO_PENDIENTE"));
  }

  @Test
  public void oneHardLimitBlocksTheWholeOverride() {
    assertTrue(ForceOverride.allForcible(Arrays.asList("INTERVALO_MINIMO", "LIMITE_ANUAL")));
    assertFalse(ForceOverride.allForcible(Arrays.asList("INTERVALO_MINIMO", "HORARIO_OCUPADO")));
    assertFalse(ForceOverride.allForcible(Collections.<String>emptyList()));
    assertFalse(ForceOverride.allForcible(null));
  }
}
