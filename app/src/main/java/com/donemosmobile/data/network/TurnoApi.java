package com.donemosmobile.data.network;

import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.TurnoDto;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Contrato de turnos del backend Laravel. */
public interface TurnoApi {

  /** {@code fecha} filters one day; {@code desde} returns that day onwards. */
  @GET("api/turnos")
  Call<List<TurnoDto>> index(
      @Query("fecha") String fecha,
      @Query("desde") String desde,
      @Query("paciente_id") Long pacienteId);

  @POST("api/turnos")
  Call<TurnoDto> store(@Body CreateTurnoRequest body);

  /** {@code {"estado": "ATENDIDO"|"CANCELADO"}}; only valid from PENDIENTE (else 422). */
  @PATCH("api/turnos/{id}")
  Call<TurnoDto> update(@Path("id") long id, @Body Map<String, String> body);
}
