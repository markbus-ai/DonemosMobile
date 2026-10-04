package com.donemosmobile.data.network;

import com.donemosmobile.data.network.dto.CreatePacienteRequest;
import com.donemosmobile.data.network.dto.PacienteDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Contrato de pacientes del backend Laravel. */
public interface PacienteApi {

  @GET("api/pacientes/{dni}")
  Call<PacienteDto> show(@Path("dni") String dni);

  @POST("api/pacientes")
  Call<PacienteDto> store(@Body CreatePacienteRequest body);
}
