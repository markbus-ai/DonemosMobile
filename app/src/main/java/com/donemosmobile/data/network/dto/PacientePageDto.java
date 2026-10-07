package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Envelope paginado de {@code GET /api/pacientes} (Laravel
 * {@code LengthAwarePaginator}). Solo se usa la lista {@code data}.
 */
public class PacientePageDto {

  @SerializedName("data")
  public List<PacienteDto> data;
}
