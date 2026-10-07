package com.donemosmobile.ui.navigation;

import com.donemosmobile.DonemosMobileActivity;
import com.donemosmobile.ui.aptitud.AptitudActivity;
import com.donemosmobile.ui.donaciones.DonacionesActivity;
import com.donemosmobile.ui.mas.MasActivity;
import com.donemosmobile.ui.pacientes.PacienteLookupActivity;
import com.donemosmobile.ui.turnos.TurnosActivity;

/**
 * Secciones de primer nivel de la app y su pantalla asociada.
 *
 * <p>El orden del enum es el orden de la barra inferior: la Home queda como
 * raíz y el resto como destinos intercambiables. Centralizar la lista acá
 * evita que cada pantalla arme su propia barra con labels distintos.
 */
public enum Section {
  HOME("Inicio", DonemosMobileActivity.class),
  TURNOS("Turnos", TurnosActivity.class),
  PACIENTES("Pacientes", PacienteLookupActivity.class),
  DONACIONES("Donaciones", DonacionesActivity.class),
  APTITUD("Aptitud", AptitudActivity.class),
  MAS("Más", MasActivity.class);

  public final String label;
  public final Class<?> activity;

  Section(String label, Class<?> activity) {
    this.label = label;
    this.activity = activity;
  }

  /** Índice estable dentro de la barra inferior. */
  public int index() {
    return ordinal();
  }

  /** Labels en el orden de la barra, listos para {@code AppNavBar}. */
  public static String[] labels() {
    Section[] sections = values();
    String[] result = new String[sections.length];
    for (int i = 0; i < sections.length; i++) {
      result[i] = sections[i].label;
    }
    return result;
  }
}
