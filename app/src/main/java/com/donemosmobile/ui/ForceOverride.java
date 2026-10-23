package com.donemosmobile.ui;

import androidx.annotation.Nullable;

import java.util.Collection;

/**
 * Pure rules for the audited "forzar" override shared by donations and turnos.
 * Kept free of Android types so it is covered by JVM unit tests.
 */
public final class ForceOverride {

  /** Same bounds the API enforces for {@code motivo} on a forced donation. */
  public static final int MOTIVO_MIN = 5;
  public static final int MOTIVO_MAX = 500;

  /** 409 codes that are hard limits: the backend never lets {@code forzar} bypass them. */
  public static final String HORARIO_OCUPADO = "HORARIO_OCUPADO";
  public static final String CAPACIDAD_EXCEDIDA = "CAPACIDAD_EXCEDIDA";

  private ForceOverride() {}

  /** Spanish error for an invalid motivo, or {@code null} when it can be sent. */
  @Nullable
  public static String checkMotivo(@Nullable String raw) {
    String motivo = raw == null ? "" : raw.trim();
    if (motivo.isEmpty()) {
      return "Escribí el motivo de la autorización";
    }
    if (motivo.length() < MOTIVO_MIN) {
      return "El motivo debe tener al menos " + MOTIVO_MIN + " caracteres";
    }
    if (motivo.length() > MOTIVO_MAX) {
      return "El motivo no puede superar los " + MOTIVO_MAX + " caracteres";
    }
    return null;
  }

  /** True when a single turno 409 code can be overridden with {@code forzar}. */
  public static boolean isForcible(@Nullable String code) {
    if (code == null || code.trim().isEmpty()) {
      // Unknown envelope: never offer a bypass the backend may not honor.
      return false;
    }
    return !HORARIO_OCUPADO.equals(code) && !CAPACIDAD_EXCEDIDA.equals(code);
  }

  /**
   * True only when every code is forcible. One hard limit in the list blocks
   * the whole override, because the backend rejects it again anyway.
   */
  public static boolean allForcible(@Nullable Collection<String> codes) {
    if (codes == null || codes.isEmpty()) {
      return false;
    }
    for (String code : codes) {
      if (!isForcible(code)) {
        return false;
      }
    }
    return true;
  }
}
