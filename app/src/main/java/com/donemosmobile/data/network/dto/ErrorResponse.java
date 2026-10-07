package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

/** Cuerpo de error estandar de la API ({@code {"message": "..."}}). */
public class ErrorResponse {

  @SerializedName("message")
  public String message;

  /**
   * Errores de validacion de Laravel: campo -> lista de mensajes. Solo viene
   * en respuestas 422 (por ejemplo, DNI duplicado).
   */
  @SerializedName("errors")
  public Map<String, List<String>> errors;
}
