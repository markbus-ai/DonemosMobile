package com.donemosmobile.data.cache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Every {@link DataCache} key in one place, so writers and readers agree. */
public final class CacheKeys {

  public static final String MOTIVOS = "motivos";
  public static final String TIPOS_DONACION = "tipos_donacion";

  private CacheKeys() {}

  /** Patient record looked up by DNI. */
  @NonNull
  public static String paciente(@NonNull String dni) {
    return "paciente:" + dni;
  }

  /** Turnos list for a fecha and/or patient filter ({@code all} when absent). */
  @NonNull
  public static String turnos(@Nullable String fecha, @Nullable Long pacienteId) {
    return "turnos:" + (fecha != null ? fecha : "all")
        + ":" + (pacienteId != null ? pacienteId : "all");
  }

  /** Turnos from {@code desde} onwards. */
  @NonNull
  public static String turnosDesde(@NonNull String desde) {
    return "turnos:desde:" + desde;
  }
}
