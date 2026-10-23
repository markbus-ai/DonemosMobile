package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de {@code POST /api/turnos}. Espeja {@code StoreTurnoRequest}.
 * {@code sede_id} es opcional: el backend resuelve la sede del staff o la default.
 */
public class CreateTurnoRequest {

  @SerializedName("paciente_id")
  public final long pacienteId;

  @SerializedName("fecha")
  public final String fecha;

  @SerializedName("hora")
  public final String hora;

  @SerializedName("tipo_id")
  public final Long tipoId;

  /** Only sent to override a forcible 409; Gson omits it when null. */
  @SerializedName("forzar")
  public Boolean forzar;

  /** Mandatory with {@code forzar=true} (max 500 chars); stored in the audit. */
  @SerializedName("motivo")
  public String motivo;

  public CreateTurnoRequest(long pacienteId, String fecha, String hora, Long tipoId) {
    this.pacienteId = pacienteId;
    this.fecha = fecha;
    this.hora = hora;
    this.tipoId = tipoId;
  }
}
