package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/** Cuerpo de error estándar de la API ({@code {"message": "..."}}). */
public class ErrorResponse {

  @SerializedName("message")
  public String message;
}
