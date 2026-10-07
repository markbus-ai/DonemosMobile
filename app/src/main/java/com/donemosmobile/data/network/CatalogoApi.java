package com.donemosmobile.data.network;

import com.donemosmobile.data.network.dto.MotivoDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/** Contrato de catalogos de referencia del backend Laravel. */
public interface CatalogoApi {

  @GET("api/motivos")
  Call<List<MotivoDto>> motivos();
}
