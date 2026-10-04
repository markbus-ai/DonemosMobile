package com.donemosmobile.data.network;

import com.donemosmobile.data.network.dto.LoginRequest;
import com.donemosmobile.data.network.dto.LoginResponse;
import com.donemosmobile.data.network.dto.UserDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/** Contrato de auth del backend Laravel. */
public interface AuthApi {

  @POST("api/auth/login")
  Call<LoginResponse> login(@Body LoginRequest body);

  @GET("api/auth/me")
  Call<UserDto> me();

  @POST("api/auth/logout")
  Call<Void> logout();
}
