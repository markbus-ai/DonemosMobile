package com.donemosmobile.data.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.network.dto.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import retrofit2.Response;

/**
 * Errores de red normalizados para toda la app. Los repositorios traducen el
 * status HTTP a un {@link Type} estable y dejan el mensaje del backend cuando
 * vino; la UI decide el copy final.
 */
public final class ApiError {

  private static final Gson GSON = new Gson();

  private ApiError() {}

  public enum Type {
    /** 401: token inválido o vencido — limpiar sesión y volver al login. */
    UNAUTHORIZED,
    /** 404: el recurso no existe (ej. paciente por DNI). */
    NOT_FOUND,
    /** 4xx: error de negocio, mostrar el mensaje del backend. */
    CLIENT,
    /** 5xx: error del servidor. */
    SERVER,
    /** Sin respuesta: timeout, DNS, sin conexión. */
    NETWORK,
    /** Cualquier otra cosa. */
    UNKNOWN
  }

  /** Traduce un status HTTP a {@link Type}. */
  public static Type fromStatus(int code) {
    if (code == 401) {
      return Type.UNAUTHORIZED;
    }
    if (code == 404) {
      return Type.NOT_FOUND;
    }
    if (code >= 400 && code < 500) {
      return Type.CLIENT;
    }
    if (code >= 500) {
      return Type.SERVER;
    }
    return Type.UNKNOWN;
  }

  /** Extrae {@code {"message": "..."}} del errorBody, o null si no se pudo. */
  @Nullable
  public static String message(@NonNull Response<?> response) {
    if (response.errorBody() == null) {
      return null;
    }
    try {
      ErrorResponse error = GSON.fromJson(response.errorBody().string(), ErrorResponse.class);
      return error != null ? error.message : null;
    } catch (IOException | JsonSyntaxException e) {
      return null;
    }
  }
}
