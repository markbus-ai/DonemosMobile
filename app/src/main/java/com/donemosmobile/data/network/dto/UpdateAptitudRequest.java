package com.donemosmobile.data.network.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * Cuerpo de {@code PATCH /api/pacientes/{dni}/aptitud}. Los campos nulos se
 * omiten del JSON (Gson), lo que respeta el contrato: {@code motivo_id} y
 * {@code desde} solo hacen falta para APTO_OBSERVACION y NO_APTO.
 */
public class UpdateAptitudRequest {

  @SerializedName("tipo")
  public final String tipo;

  @SerializedName("motivo_id")
  public final Long motivoId;

  @SerializedName("desde")
  public final String desde;

  @SerializedName("hasta")
  public final String hasta;

  @SerializedName("permanente")
  public final Boolean permanente;

  public UpdateAptitudRequest(String tipo, @Nullable Long motivoId, @Nullable String desde,
      @Nullable String hasta, @Nullable Boolean permanente) {
    this.tipo = tipo;
    this.motivoId = motivoId;
    this.desde = desde;
    this.hasta = hasta;
    this.permanente = permanente;
  }
}
