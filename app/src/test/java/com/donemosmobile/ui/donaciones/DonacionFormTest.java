package com.donemosmobile.ui.donaciones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.donemosmobile.ui.NumericBounds;
import com.donemosmobile.ui.donaciones.DonacionForm.ClinicalField;

import org.junit.Test;

/** JVM tests for the client-side validation of the donation form. */
public class DonacionFormTest {

  /** A form that passes every check. */
  private static DonacionForm valid() {
    DonacionForm form = new DonacionForm();
    form.pacienteId = 1L;
    form.tipoId = 10L;
    form.cantidades.put(10L, 1);
    return form;
  }

  @Test
  public void completeFormIsValid() {
    DonacionForm.Errors errors = valid().validate();
    assertTrue(errors.ok());
    assertNull(errors.form);
  }

  @Test
  public void missingPatientStopsBeforeAnyOtherCheck() {
    DonacionForm form = new DonacionForm();
    DonacionForm.Errors errors = form.validate();
    assertTrue(errors.missingPaciente);
    assertFalse(errors.ok());
    assertNull(errors.tipo);
    assertNull(errors.componentes);
  }

  @Test
  public void tipoAndComponentesAreRequired() {
    DonacionForm form = new DonacionForm();
    form.pacienteId = 1L;
    DonacionForm.Errors errors = form.validate();
    assertEquals("Elegí el tipo de donación", errors.tipo);
    assertEquals("Agregá al menos un componente", errors.componentes);
    assertFalse(errors.ok());
  }

  @Test
  public void outOfBoundsClinicalValueFlagsItsFieldAndTheSection() {
    DonacionForm form = valid();
    form.clinical.put(ClinicalField.HEMOGLOBINA, "31");
    DonacionForm.Errors errors = form.validate();
    assertEquals(NumericBounds.check(NumericBounds.HEMOGLOBINA, "31"),
        errors.fields.get(ClinicalField.HEMOGLOBINA));
    assertNull(errors.fields.get(ClinicalField.PESO));
    assertEquals("Revisá los valores clínicos marcados", errors.clinical);
    assertFalse(errors.ok());
  }

  @Test
  public void aferesisRequiresPlaquetasAndHematocrito() {
    DonacionForm form = valid();
    form.tipoAferesis = true;
    assertEquals("Aféresis: cargá plaquetas y hematocrito", form.validate().clinical);
    form.clinical.put(ClinicalField.PLAQUETAS, "250000");
    form.clinical.put(ClinicalField.HEMATOCRITO, "42");
    assertTrue(form.validate().ok());
  }

  @Test
  public void diastolicAboveSystolicFlagsDiastolica() {
    DonacionForm form = valid();
    form.clinical.put(ClinicalField.SISTOLICA, "80");
    form.clinical.put(ClinicalField.DIASTOLICA, "120");
    DonacionForm.Errors errors = form.validate();
    assertEquals("La presión diastólica debe ser menor que la sistólica",
        errors.fields.get(ClinicalField.DIASTOLICA));
    assertEquals("La presión diastólica debe ser menor que la sistólica", errors.clinical);
  }

  @Test
  public void partialCycleOverrideIsRejected() {
    DonacionForm form = valid();
    form.ciclo = "2";
    DonacionForm.Errors errors = form.validate();
    assertEquals("Completá ciclo, serie y donación, o dejá los tres vacíos", errors.cycle);
    assertFalse(errors.ok());
    form.serie = "1";
    form.donacion = "3";
    assertTrue(form.validate().ok());
  }

  @Test
  public void componentLimitsFollowTheBackendRules() {
    assertEquals(1, DonacionForm.maxFor("SANGRE"));
    assertEquals(1, DonacionForm.maxFor("PLASMA"));
    assertEquals(3, DonacionForm.maxFor("PLAQUETAS"));
    assertTrue(DonacionForm.isComponente("PLAQUETAS"));
    assertFalse(DonacionForm.isComponente("CRIO"));
    assertNotNull(DonacionForm.DEFAULT_TIPO_BOLSA);
  }
}
