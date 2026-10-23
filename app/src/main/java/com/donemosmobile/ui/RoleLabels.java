package com.donemosmobile.ui;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Human copy for the staff role and its scope, from the raw API values
 * ({@code rol} name, {@code sede} and {@code zona} names). Pure, JVM tested.
 */
public final class RoleLabels {

  public static final String ADMIN_ZONA = "admin-zona";
  public static final String ADMIN_SEDE = "admin-sede";
  public static final String STAFF = "staff";

  private RoleLabels() {}

  /** "Administrador de zona", "Administrador de sede", "Personal de staff". */
  public static String label(@Nullable String rol) {
    String key = normalize(rol);
    if (key.isEmpty()) {
      return "—";
    }
    switch (key) {
      case ADMIN_ZONA:
        return "Administrador de zona";
      case ADMIN_SEDE:
        return "Administrador de sede";
      case STAFF:
        return "Personal de staff";
      default:
        // Unknown role: readable instead of a raw slug.
        String spaced = key.replace('-', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
  }

  /** True when the scope to show is the zona (admin de zona), not a single sede. */
  public static boolean scopedToZona(@Nullable String rol) {
    return ADMIN_ZONA.equals(normalize(rol));
  }

  /** Label of the scope line: "Zona" for an admin de zona with a zona, else "Sede". */
  public static String scopeLabel(@Nullable String rol, @Nullable String zona) {
    return scopedToZona(rol) && !isBlank(zona) ? "Zona" : "Sede";
  }

  /** Value of the scope line, matching {@link #scopeLabel}; {@code null} when unknown. */
  @Nullable
  public static String scopeValue(@Nullable String rol, @Nullable String sede,
      @Nullable String zona) {
    if (scopedToZona(rol) && !isBlank(zona)) {
      return zona.trim();
    }
    return isBlank(sede) ? null : sede.trim();
  }

  private static String normalize(@Nullable String rol) {
    return rol == null ? "" : rol.trim().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static boolean isBlank(@Nullable String value) {
    return value == null || value.trim().isEmpty();
  }
}
