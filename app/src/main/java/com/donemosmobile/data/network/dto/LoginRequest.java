package com.donemosmobile.data.network.dto;

import com.google.gson.annotations.SerializedName;

/** Cuerpo de POST /api/auth/login. */
public class LoginRequest {

  @SerializedName("username")
  public final String username;

  @SerializedName("password")
  public final String password;

  public LoginRequest(String username, String password) {
    this.username = username;
    this.password = password;
  }
}
