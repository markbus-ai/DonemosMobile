package com.donemosmobile.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** JVM tests for the human role label and sede/zona scope line on "Más". */
public class RoleLabelsTest {

  @Test
  public void knownRolesGetHumanLabels() {
    assertEquals("Administrador de zona", RoleLabels.label("admin-zona"));
    assertEquals("Administrador de sede", RoleLabels.label("admin-sede"));
    assertEquals("Personal de staff", RoleLabels.label("staff"));
    assertEquals("Administrador de zona", RoleLabels.label(" ADMIN_ZONA "));
  }

  @Test
  public void unknownOrMissingRolesStayReadable() {
    assertEquals("Super admin", RoleLabels.label("super-admin"));
    assertEquals("—", RoleLabels.label(null));
    assertEquals("—", RoleLabels.label(""));
  }

  @Test
  public void adminZonaShowsTheZona() {
    assertEquals("Zona", RoleLabels.scopeLabel("admin-zona", "Zona Norte"));
    assertEquals("Zona Norte", RoleLabels.scopeValue("admin-zona", null, "Zona Norte"));
  }

  @Test
  public void everyoneElseShowsTheSede() {
    assertEquals("Sede", RoleLabels.scopeLabel("staff", "Zona Norte"));
    assertEquals("Sede Central", RoleLabels.scopeValue("staff", "Sede Central", "Zona Norte"));
    assertEquals("Sede", RoleLabels.scopeLabel("admin-zona", null));
    assertEquals("Sede Central", RoleLabels.scopeValue("admin-zona", "Sede Central", " "));
    assertNull(RoleLabels.scopeValue("admin-sede", null, null));
  }
}
