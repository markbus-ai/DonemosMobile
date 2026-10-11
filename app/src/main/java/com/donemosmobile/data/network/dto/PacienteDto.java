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

  /**
   * Contadores manuales del ciclo actual del paciente. Los carga el personal
   * y son nullable: {@code null} significa "sin dato". El backend nunca los
   * deriva del historial de donaciones.
   */
  @SerializedName("ciclo_actual")
  public Integer cicloActual;

  @SerializedName("serie_actual")
  public Integer serieActual;

  @SerializedName("donacion_actual")
  public Integer donacionActual;

  /**
   * Relacion embebida solo en la respuesta de PATCH aptitud (y del listado).
   * El endpoint {@code GET /api/pacientes/{dni}} devuelve el modelo crudo y no
   * la incluye, por eso queda {@code null} tras un lookup comun.
   */
  @SerializedName("aptitud")
  public AptitudDto aptitud;

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
