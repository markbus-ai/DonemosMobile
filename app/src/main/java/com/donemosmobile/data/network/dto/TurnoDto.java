package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Turno crudo devuelto por {@code GET /api/turnos} / {@code POST /api/turnos}.
 *
 * <p>El backend hoy serializa el modelo Eloquent sin relaciones eager-loaded,
 * así que solo vienen los ids ({@code paciente_id}, {@code sede_id},
 * {@code tipo_id}) y no los nombres. Se mapea defensivo: los campos ausentes
 * quedan en null y Gson ignora los desconocidos.
 */
public class TurnoDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("paciente_id")
  public Long pacienteId;

  @SerializedName("sede_id")
  public Long sedeId;

  @SerializedName("tipo_id")
  public Long tipoId;

  /** Formato {@code YYYY-MM-DD}. */
  @SerializedName("fecha")
  public String fecha;

  /** Formato {@code H:i}. */
  @SerializedName("hora")
  public String hora;

  /** PENDIENTE | ATENDIDO | CANCELADO. */
  @SerializedName("estado")
  public String estado;

  /** Hora recortada a {@code HH:mm} para mostrar; vacío si no vino. */
  public String horaCorta() {
    if (hora == null || hora.isEmpty()) {
      return "";
    }
    return hora.length() >= 5 ? hora.substring(0, 5) : hora;
  }
}
