package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

/**
 * Fila del catalogo {@code GET /api/tipos-donacion}. Se usa para elegir el tipo
 * de la donacion y, a la vez, como catalogo de componentes (SANGRE, PLASMA,
 * PLAQUETAS): el codigo es el que aplica {@code ComponentRules}. {@code esAferesis}
 * define si la pantalla exige plaquetas + hematocrito.
 */
public class TipoDonacionDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("codigo")
  public String codigo;

  @SerializedName("nombre")
  public String nombre;

  @SerializedName("es_aferesis")
  public boolean esAferesis;

  @SerializedName("duracion_minutos")
  public Integer duracionMinutos;

  /** Nombre listo para mostrar, con fallback al codigo. */
  public String displayName() {
    if (nombre != null && !nombre.isEmpty()) {
      return nombre;
    }
    return codigo != null ? codigo : "";
  }

  /** Codigo en mayusculas, o cadena vacia; estable para comparar reglas. */
  public String codigoUpper() {
    return codigo != null ? codigo.toUpperCase(Locale.ROOT) : "";
  }
}
