package com.donemosmobile;

import android.content.Context;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.turnos.TurnoRepository;
import com.donemosmobile.ui.TurnoSchedule;

/**
 * Precalienta en segundo plano los datos que necesitan las primeras pantallas.
 *
 * <p>Se dispara al entrar a la Home autenticada. Fire-and-forget: los
 * repositorios escriben en {@link DataCache} y las pantallas levantan el dato
 * al instante cuando se abren. Vive en el paquete raiz (composicion de la app)
 * porque usa el "hoy" de las pantallas y la capa de datos no depende de ui.
 */
public final class Prefetcher {

  private Prefetcher() {}

  /** Lanza los fetch de fondo. No bloquea: nadie espera el resultado. */
  public static void warmUp(Context context) {
    Context app = context.getApplicationContext();

    // Turnos de hoy: es la primera pantalla que suele abrir el mostrador.
    // Same day key as TurnosActivity and the Home, from TurnoSchedule.today().
    new TurnoRepository(app).prefetch(TurnoSchedule.today(), null);

    // Pacientes: el backend solo expone GET /api/pacientes/{dni} (no hay
    // listado), asi que no hay lista para precalentar. Las busquedas por DNI
    // se cachean solas al primer lookup y quedan instantaneas despues.
  }
}
