package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/** Respuesta 200 de POST /api/auth/login. */
public class LoginResponse {

  @SerializedName("token")
  public String token;

  @SerializedName("user")
  public UserDto user;
}
