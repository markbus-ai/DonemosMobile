package com.donemosmobile.data.network;

import com.donemosmobile.data.network.dto.CreatePacienteRequest;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.PacientePageDto;
import com.donemosmobile.data.network.dto.UpdateAptitudRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Contrato de pacientes del backend Laravel. */
public interface PacienteApi {

  @GET("api/pacientes/{dni}")
  Call<PacienteDto> show(@Path("dni") String dni);

  /** Listado paginado con la relacion aptitud embebida (PacienteResource). */
  @GET("api/pacientes")
  Call<PacientePageDto> index(@Query("search") String search, @Query("per_page") int perPage);

  @POST("api/pacientes")
  Call<PacienteDto> store(@Body CreatePacienteRequest body);

  @PATCH("api/pacientes/{dni}/aptitud")
  Call<PacienteDto> updateAptitud(@Path("dni") String dni, @Body UpdateAptitudRequest body);
}
