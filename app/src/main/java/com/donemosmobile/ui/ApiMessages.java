package com.donemosmobile.ui;

import com.donemosmobile.data.network.ApiError;

/**
 * Copy consistente para los errores de red por tipo. Mantiene el mismo texto
 * que antes tenia cada pantalla, con su fallback propio para CLIENT.
 */
public final class ApiMessages {

  private ApiMessages() {}

  public static String forType(ApiError.Type type, String message, String clientFallback,
      String defaultFallback) {
    switch (type) {
      case UNAUTHORIZED:
        return "Sesión vencida, volvé a ingresar";
      case NETWORK:
        return "No se pudo conectar";
      case SERVER:
        return "Error del servidor, intentá de nuevo";
      case CLIENT:
        return nonEmpty(message) ? message : clientFallback;
      default:
        return nonEmpty(message) ? message : defaultFallback;
    }
  }

  public static String forType(ApiError.Type type, String message, String fallback) {
    return forType(type, message, fallback, fallback);
  }

  /**
   * 422 field copy: the API already answers in Spanish (e.g. "Ya existe un
   * paciente con ese DNI."), so show it as is; the fallback covers a blank one.
   */
  public static String fieldOr(String backendMessage, String fallback) {
    return backendMessage != null && !backendMessage.trim().isEmpty()
        ? backendMessage.trim() : fallback;
  }

  private static boolean nonEmpty(String value) {
    return value != null && !value.isEmpty();
  }
}
