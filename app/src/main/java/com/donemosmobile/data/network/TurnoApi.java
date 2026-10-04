package com.donemosmobile.data.network;

import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.TurnoDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Contrato de turnos del backend Laravel. */
public interface TurnoApi {

  @GET("api/turnos")
  Call<List<TurnoDto>> index(
      @Query("fecha") String fecha,
      @Query("paciente_id") Long pacienteId);

  @POST("api/turnos")
  Call<TurnoDto> store(@Body CreateTurnoRequest body);
}
