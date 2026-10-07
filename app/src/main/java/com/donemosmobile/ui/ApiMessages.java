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

  private static boolean nonEmpty(String value) {
    return value != null && !value.isEmpty();
  }
}
