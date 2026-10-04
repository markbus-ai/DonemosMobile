package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Envelope 409 de {@code POST /api/turnos} cuando el turno viola reglas de
 * donación (intervalo, límite, turno pendiente) o excede la capacidad diaria.
 * El mensaje trae {@code code} y {@code message}; {@code warnings} puede venir
 * con el detalle para mostrar.
 */
public class TurnoWarningResponse {

  @SerializedName("warning")
  public boolean warning;

  @SerializedName("code")
  public String code;

  @SerializedName("message")
  public String message;

  @SerializedName("warnings")
  public Warning[] warnings;

  public static class Warning {
    @SerializedName("code")
    public String code;

    @SerializedName("message")
    public String message;
  }
}
