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

  /** Every rule code in the envelope (top-level included); never null. */
  public java.util.List<String> codes() {
    java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
    if (code != null && !code.isEmpty()) {
      result.add(code);
    }
    if (warnings != null) {
      for (Warning item : warnings) {
        if (item != null && item.code != null && !item.code.isEmpty()) {
          result.add(item.code);
        }
      }
    }
    return new java.util.ArrayList<>(result);
  }

  /** Messages ready to show, falling back to the top-level message; never null. */
  public java.util.List<String> messages() {
    java.util.ArrayList<String> result = new java.util.ArrayList<>();
    if (warnings != null) {
      for (Warning item : warnings) {
        if (item != null && item.message != null && !item.message.isEmpty()) {
          result.add(item.message);
        }
      }
    }
    if (result.isEmpty() && message != null && !message.isEmpty()) {
      result.add(message);
    }
    return result;
  }

  public static class Warning {
    @SerializedName("code")
    public String code;

    @SerializedName("message")
    public String message;
  }
}
