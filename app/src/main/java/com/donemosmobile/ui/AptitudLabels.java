package com.donemosmobile.ui;

import com.donemosmobile.data.network.dto.PacienteDto;

/** Etiquetas legibles de aptitud, compartidas entre Donaciones y Aptitud. */
public final class AptitudLabels {

  private AptitudLabels() {}

  public static String tipoLabel(String tipo) {
    if ("APTO".equals(tipo)) {
      return "Apto";
    }
    if ("APTO_OBSERVACION".equals(tipo)) {
      return "Apto con observación";
    }
    if ("NO_APTO".equals(tipo)) {
      return "No apto";
    }
    return tipo;
  }

  public static String current(PacienteDto paciente) {
    if (paciente != null && paciente.aptitud != null && paciente.aptitud.tipo != null) {
      return tipoLabel(paciente.aptitud.tipo);
    }
    return "No disponible";
  }
}
