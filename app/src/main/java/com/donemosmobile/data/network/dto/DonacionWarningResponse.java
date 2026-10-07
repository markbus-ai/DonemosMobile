package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Envelope 409 de {@code POST /api/donaciones}. El backend devuelve
 * {@code {"warnings":[{"code","message"}]}}; ademas se toleran las claves
 * {@code warning/code/message} de nivel superior por si el contrato se amplia.
 * Cada warning es overrideable con {@code forzar:true}.
 */
public class DonacionWarningResponse {

  @SerializedName("warning")
  public boolean warning;

  @SerializedName("code")
  public String code;

  @SerializedName("message")
  public String message;

  @SerializedName("warnings")
  public Warning[] warnings;

  /** Lista de mensajes lista para pintar; nunca nula. */
  public java.util.List<String> messages() {
    java.util.ArrayList<String> result = new java.util.ArrayList<>();
    if (warnings != null) {
      for (Warning item : warnings) {
        if (item != null && item.message != null && !item.message.isEmpty()) {
          result.add(item.message);
        } else if (item != null && item.code != null) {
          result.add(item.code);
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
