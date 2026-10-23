package com.donemosmobile.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** JVM tests for the 422 field copy: the backend Spanish message wins. */
public class ApiMessagesTest {

  @Test
  public void duplicateDniShowsTheBackendMessage() {
    assertEquals("Ya existe un paciente con ese DNI.",
        ApiMessages.fieldOr("Ya existe un paciente con ese DNI.", "DNI inválido"));
  }

  @Test
  public void blankBackendMessageFallsBack() {
    assertEquals("DNI inválido", ApiMessages.fieldOr(null, "DNI inválido"));
    assertEquals("DNI inválido", ApiMessages.fieldOr("  ", "DNI inválido"));
  }
}
