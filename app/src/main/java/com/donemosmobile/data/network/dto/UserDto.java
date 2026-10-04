package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Personal autenticado. Se mapea de forma defensiva: los campos que no vengan
 * en la respuesta quedan en null y Gson ignora los desconocidos.
 */
public class UserDto {

  @SerializedName("id")
  public Long id;

  @SerializedName("username")
  public String username;

  @SerializedName("nombre")
  public String nombre;

  @SerializedName("rol")
  public String rol;

  @SerializedName("sede")
  public String sede;

  @SerializedName("sede_id")
  public Long sedeId;

  /** Nombre listo para mostrar, con fallbacks si la API no manda todos los campos. */
  public String displayName() {
    if (nombre != null && !nombre.isEmpty()) {
      return nombre;
    }
    if (username != null && !username.isEmpty()) {
      return username;
    }
    return "";
  }
}
