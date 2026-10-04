package com.donemosmobile.data.cache;

import android.content.Context;

import com.donemosmobile.data.turnos.TurnoRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Precalienta en segundo plano los datos que necesitan las primeras pantallas.
 *
 * <p>Se dispara al entrar a la Home autenticada. Fire-and-forget: los
 * repositorios escriben en {@link DataCache} y las pantallas levantan el dato
 * al instante cuando se abren.
 */
public final class Prefetcher {

  private Prefetcher() {}

  /** Lanza los fetch de fondo. No bloquea: nadie espera el resultado. */
  public static void warmUp(Context context) {
    Context app = context.getApplicationContext();

    // Turnos de hoy: es la primera pantalla que suele abrir el mostrador.
    new TurnoRepository(app).prefetch(hoy(), null);

    // Pacientes: el backend solo expone GET /api/pacientes/{dni} (no hay
    // listado), asi que no hay lista para precalentar. Las busquedas por DNI
    // se cachean solas al primer lookup y quedan instantaneas despues.
  }

  /** Misma fecha que usa TurnosActivity, para no errarle a la clave del cache. */
  private static String hoy() {
    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
  }
}
