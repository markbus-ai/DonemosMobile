package com.donemosmobile.ui;

import com.donemosmobile.ui.theme.AppColors;

/**
 * Color y etiqueta por estado de turno, compartidos entre la lista de Turnos y
 * la actividad reciente de la Home. Los colores son tokens de texto (AA sobre
 * blanco), nunca los de relleno.
 */
public final class TurnoLabels {

  public static final String PENDIENTE = "PENDIENTE";
  public static final String ATENDIDO = "ATENDIDO";
  public static final String CANCELADO = "CANCELADO";

  private TurnoLabels() {}

  public static int estadoColor(String estado) {
    if (estado == null) {
      return AppColors.TEXT_SECONDARY;
    }
    switch (estado) {
      case ATENDIDO:
        return AppColors.SUCCESS_TEXT;
      case CANCELADO:
        return AppColors.ERROR;
      case PENDIENTE:
        return AppColors.WARNING_TEXT;
      default:
        return AppColors.TEXT_SECONDARY;
    }
  }

  /** Human label for the raw API estado. */
  public static String estadoLabel(String estado) {
    if (estado == null || estado.isEmpty()) {
      return "";
    }
    switch (estado) {
      case PENDIENTE:
        return "Pendiente";
      case ATENDIDO:
        return "Atendido";
      case CANCELADO:
        return "Cancelado";
      default:
        return estado;
    }
  }
}
