package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Aptitud embebida en la respuesta de {@code PATCH /api/pacientes/{dni}/aptitud}
 * (y del listado). Solo interesa el tipo para mostrar la situacion actual.
 */
public class AptitudDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("tipo")
  public String tipo;
}
