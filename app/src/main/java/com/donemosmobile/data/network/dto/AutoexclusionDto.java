package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Respuesta 201 de {@code POST /api/donaciones/{numero}/autoexclusion}. El
 * backend no expone identidad del paciente: solo el registro confidencial.
 */
public class AutoexclusionDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("donacion_id")
  public Long donacionId;

  @SerializedName("motivo")
  public String motivo;
}
