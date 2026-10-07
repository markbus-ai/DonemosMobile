package com.donemosmobile.data.network;

import com.donemosmobile.data.network.dto.MotivoDto;
import com.donemosmobile.data.network.dto.TipoDonacionDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/** Contrato de catalogos de referencia del backend Laravel. */
public interface CatalogoApi {

  @GET("api/motivos")
  Call<List<MotivoDto>> motivos();

  @GET("api/tipos-donacion")
  Call<List<TipoDonacionDto>> tiposDonacion();
}
