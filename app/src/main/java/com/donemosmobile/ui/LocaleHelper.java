package com.donemosmobile.ui;

import android.content.Context;
import android.content.res.Configuration;

import java.util.Locale;

/**
 * Fuerza el idioma de la app a español rioplatense.
 *
 * <p>Sin esto, los widgets de Material (por ejemplo el date picker) muestran
 * textos en el idioma del dispositivo —"Selected date"— mezclados con nuestras
 * cadenas en español. Fijar el locale en el contexto base alinea todo.
 */
public final class LocaleHelper {

  public static final Locale SPANISH = new Locale("es", "AR");

  private LocaleHelper() {}

  /** Envuelve el contexto base con el locale español. */
  public static Context wrap(Context base) {
    // Algunos widgets formatean fechas con el locale por defecto de la JVM
    // (no el del contexto): sin esto el date picker muestra "OCTOBER 2026".
    Locale.setDefault(SPANISH);

    Configuration config = new Configuration(base.getResources().getConfiguration());
    config.setLocale(SPANISH);
    return base.createConfigurationContext(config);
  }
}
