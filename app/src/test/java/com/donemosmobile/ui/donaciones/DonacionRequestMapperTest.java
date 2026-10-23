package com.donemosmobile.ui.donaciones;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.donemosmobile.data.network.dto.CreateDonacionRequest;
import com.donemosmobile.ui.donaciones.DonacionForm.ClinicalField;

import org.junit.Test;

/** JVM tests for the POST /api/donaciones body built from the form. */
public class DonacionRequestMapperTest {

  private static DonacionForm form() {
    DonacionForm form = new DonacionForm();
    form.pacienteId = 1L;
    form.tipoId = 10L;
    return form;
  }

  @Test
  public void expandsEachUnitIntoItsOwnComponente() {
    DonacionForm form = form();
    form.cantidades.put(20L, 3);
    form.cantidades.put(21L, 0);
    form.cantidades.put(22L, 1);
    CreateDonacionRequest body = DonacionRequestMapper.toRequest(form, false, null);
    assertEquals(4, body.componentes.size());
    assertEquals(Long.valueOf(20L), body.componentes.get(0).tipoId);
    assertEquals(Long.valueOf(20L), body.componentes.get(2).tipoId);
    assertEquals(Long.valueOf(22L), body.componentes.get(3).tipoId);
  }

  @Test
  public void mapsSelectionClinicalValuesAndBlankTextAsNull() {
    DonacionForm form = form();
    form.clinical.put(ClinicalField.HEMOGLOBINA, "13,5");
    form.clinical.put(ClinicalField.SISTOLICA, "120");
    form.lote = "  L-7 ";
    form.anticoagulante = "  ";
    CreateDonacionRequest body = DonacionRequestMapper.toRequest(form, false, null);
    assertEquals(Long.valueOf(1L), body.pacienteId);
    assertEquals(Long.valueOf(10L), body.tipoId);
    assertEquals("doble", body.tipoBolsa);
    assertEquals("izquierdo", body.brazo);
    assertEquals(Double.valueOf(13.5), body.hemoglobina);
    assertEquals(Integer.valueOf(120), body.presionSistolica);
    assertNull(body.hematocrito);
    assertEquals("L-7", body.lote);
    assertNull(body.anticoagulante);
    assertNull(body.ciclo);
  }

  @Test
  public void forzarSendsTheMotivoAndOtherwiseBothAreOmitted() {
    CreateDonacionRequest forced = DonacionRequestMapper.toRequest(form(), true, "Autorizado");
    assertEquals(Boolean.TRUE, forced.forzar);
    assertEquals("Autorizado", forced.motivo);
    CreateDonacionRequest normal = DonacionRequestMapper.toRequest(form(), false, "ignored");
    assertNull(normal.forzar);
    assertNull(normal.motivo);
  }
}
