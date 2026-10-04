package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/** Cuerpo de {@code POST /api/pacientes}. Espeja {@code StorePacienteRequest}. */
public class CreatePacienteRequest {

  @SerializedName("dni")
  public final String dni;

  @SerializedName("nombre")
  public final String nombre;

  @SerializedName("apellido")
  public final String apellido;

  @SerializedName("telefono")
  public final String telefono;

  @SerializedName("sexo")
  public final String sexo;

  @SerializedName("altura")
  public final Double altura;

  public CreatePacienteRequest(String dni, String nombre, String apellido, String telefono,
      String sexo, Double altura) {
    this.dni = dni;
    this.nombre = nombre;
    this.apellido = apellido;
    this.telefono = telefono;
    this.sexo = sexo;
    this.altura = altura;
  }
}
