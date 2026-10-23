package com.donemosmobile.data.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.network.dto.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    UNKNOWN,
    /** Login answered 401: wrong user or password (no session to expire). */
    INVALID_CREDENTIALS,
    /** Login answered 429: throttled. */
    TOO_MANY_ATTEMPTS
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

  /**
   * Error body parsed once. OkHttp's {@code errorBody()} is a one-shot stream,
   * so callers must read it a single time and derive everything from here.
   */
  public static final class Body {
    @Nullable public final String raw;
    @Nullable public final String message;
    @NonNull public final Map<String, String> fieldErrors;

    Body(@Nullable String raw, @Nullable String message, @NonNull Map<String, String> fieldErrors) {
      this.raw = raw;
      this.message = message;
      this.fieldErrors = fieldErrors;
    }
  }

  /** Reads {@code errorBody()} exactly once and parses message + field errors. */
  @NonNull
  public static Body read(@NonNull Response<?> response) {
    String raw = null;
    if (response.errorBody() != null) {
      try {
        raw = response.errorBody().string();
      } catch (IOException e) {
        raw = null;
      }
    }
    return parse(raw);
  }

  /**
   * Parses a Laravel error envelope ({@code {"message": ..., "errors": {campo: [...]}}}).
   * Pure function so it can be unit tested on the JVM.
   */
  @NonNull
  public static Body parse(@Nullable String raw) {
    Map<String, String> fields = new LinkedHashMap<>();
    String message = null;
    if (raw != null && !raw.isEmpty()) {
      try {
        ErrorResponse error = GSON.fromJson(raw, ErrorResponse.class);
        if (error != null) {
          message = error.message;
          if (error.errors != null) {
            for (Map.Entry<String, List<String>> entry : error.errors.entrySet()) {
              List<String> messages = entry.getValue();
              if (messages != null && !messages.isEmpty() && messages.get(0) != null) {
                fields.put(entry.getKey(), messages.get(0));
              }
            }
          }
        }
      } catch (JsonSyntaxException | IllegalStateException e) {
        // Body is not a JSON envelope (HTML error page, plain text): no details.
      }
    }
    return new Body(raw, message, fields);
  }
}
