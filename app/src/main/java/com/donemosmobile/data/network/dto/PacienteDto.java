package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Paciente devuelto por {@code GET /api/pacientes/{dni}}. El backend serializa
 * el modelo Eloquent: snake_case, {@code sexo} y {@code altura} nullable.
 */
public class PacienteDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("dni")
  public String dni;

  @SerializedName("nombre")
  public String nombre;

  @SerializedName("apellido")
  public String apellido;

  @SerializedName("telefono")
  public String telefono;

  @SerializedName("sexo")
  public String sexo;

  @SerializedName("altura")
  public Double altura;

  @SerializedName("aptitud_id")
  public Long aptitudId;

  /** Nombre listo para mostrar, con fallback al DNI. */
  public String displayName() {
    String first = nombre != null ? nombre : "";
    String last = apellido != null ? apellido : "";
    String full = (first + " " + last).trim();
    if (!full.isEmpty()) {
      return full;
    }
    return dni != null ? dni : "";
  }
}
