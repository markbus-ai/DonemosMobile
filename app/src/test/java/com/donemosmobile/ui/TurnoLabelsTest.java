package com.donemosmobile.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** JVM tests for the human turno estado label. */
public class TurnoLabelsTest {

  @Test
  public void estadoLabelIsHuman() {
    assertEquals("Pendiente", TurnoLabels.estadoLabel("PENDIENTE"));
    assertEquals("Atendido", TurnoLabels.estadoLabel("ATENDIDO"));
    assertEquals("Cancelado", TurnoLabels.estadoLabel("CANCELADO"));
    assertEquals("OTRO", TurnoLabels.estadoLabel("OTRO"));
    assertEquals("", TurnoLabels.estadoLabel(null));
  }
}
