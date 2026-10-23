package com.donemosmobile.ui;

/**
 * Color por estado de turno, compartido entre la lista de Turnos y la
 * actividad reciente de la Home.
 */
public final class TurnoLabels {

  private TurnoLabels() {}

  public static int estadoColor(String estado) {
    if (estado == null) {
      return com.donemosmobile.ui.theme.AppColors.TEXT_SECONDARY;
    }
    switch (estado) {
      case "ATENDIDO":
        return com.donemosmobile.ui.theme.AppColors.SUCCESS_TEXT;
      case "CANCELADO":
        return com.donemosmobile.ui.theme.AppColors.ERROR;
      case "PENDIENTE":
        return com.donemosmobile.ui.theme.AppColors.WARNING_TEXT;
      default:
        return com.donemosmobile.ui.theme.AppColors.TEXT_SECONDARY;
    }
  }
}
