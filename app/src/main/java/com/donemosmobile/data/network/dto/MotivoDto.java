package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/** Fila del catalogo de motivos ({@code GET /api/motivos}). */
public class MotivoDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("codigo")
  public String codigo;

  @SerializedName("nombre")
  public String nombre;

  @SerializedName("plazo_meses")
  public Integer plazoMeses;

  /** Nombre listo para mostrar, con fallback al codigo y a "Motivo". */
  public String displayName() {
    if (nombre != null && !nombre.isEmpty()) {
      return nombre;
    }
    if (codigo != null && !codigo.isEmpty()) {
      return codigo;
    }
    return "Motivo";
  }
}
