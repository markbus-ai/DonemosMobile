package com.donemosmobile.ui.donaciones;

import androidx.annotation.Nullable;

import com.donemosmobile.data.network.dto.CreateDonacionRequest;
import com.donemosmobile.ui.NumberParsing;
import com.donemosmobile.ui.donaciones.DonacionForm.ClinicalField;

import java.util.ArrayList;
import java.util.Map;

/** Builds the {@code POST /api/donaciones} body from a {@link DonacionForm}. */
public final class DonacionRequestMapper {

  private DonacionRequestMapper() {}

  /**
   * @param forzar resend after an accepted 409 with an audited authorization
   * @param motivo the authorization reason, sent only when forcing
   */
  public static CreateDonacionRequest toRequest(DonacionForm form, boolean forzar,
      @Nullable String motivo) {
    CreateDonacionRequest body = new CreateDonacionRequest();
    body.pacienteId = form.pacienteId;
    body.tipoId = form.tipoId;
    body.tipoBolsa = form.tipoBolsa;
    body.brazo = form.brazo;
    body.anticoagulante = blankToNull(form.anticoagulante);
    body.lote = blankToNull(form.lote);
    body.tubuladura = blankToNull(form.tubuladura);
    body.dificultad = blankToNull(form.dificultad);
    body.dobleEtiqueta = form.dobleEtiqueta;

    body.hemoglobina = NumberParsing.parseDouble(form.clinicalText(ClinicalField.HEMOGLOBINA));
    body.hematocrito = NumberParsing.parseDouble(form.clinicalText(ClinicalField.HEMATOCRITO));
    body.plaquetas = NumberParsing.parseInt(form.clinicalText(ClinicalField.PLAQUETAS));
    body.presionSistolica = NumberParsing.parseInt(form.clinicalText(ClinicalField.SISTOLICA));
    body.presionDiastolica = NumberParsing.parseInt(form.clinicalText(ClinicalField.DIASTOLICA));
    body.frecuenciaCardiaca = NumberParsing.parseInt(form.clinicalText(ClinicalField.FRECUENCIA));
    body.pesoDonante = NumberParsing.parseDouble(form.clinicalText(ClinicalField.PESO));

    // Override opcional de la posicion: nulo cuando el campo queda vacío (Gson
    // lo omite). Solo los tres juntos cuentan como override; si falta alguno, el
    // backend calcula la posicion.
    body.ciclo = NumberParsing.parseInt(form.ciclo);
    body.serie = NumberParsing.parseInt(form.serie);
    body.donacion = NumberParsing.parseInt(form.donacion);

    // One entry per unit: PLAQUETAS x3 sends three componentes of that tipo.
    body.componentes = new ArrayList<>();
    for (Map.Entry<Long, Integer> entry : form.cantidades.entrySet()) {
      Integer qty = entry.getValue();
      if (qty == null || qty <= 0 || entry.getKey() == null) {
        continue;
      }
      for (int i = 0; i < qty; i++) {
        body.componentes.add(new CreateDonacionRequest.Componente(entry.getKey()));
      }
    }

    body.forzar = forzar ? Boolean.TRUE : null;
    // The API requires motivo (5..500) whenever forzar is true; it lands in the audit.
    body.motivo = forzar ? motivo : null;
    return body;
  }

  @Nullable
  private static String blankToNull(@Nullable String value) {
    return value != null && !value.trim().isEmpty() ? value.trim() : null;
  }
}
