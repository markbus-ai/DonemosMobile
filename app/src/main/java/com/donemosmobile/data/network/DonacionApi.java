package com.donemosmobile.data.network;

import com.donemosmobile.data.network.dto.AutoexclusionDto;
import com.donemosmobile.data.network.dto.CreateDonacionRequest;
import com.donemosmobile.data.network.dto.DonacionDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Contrato de donaciones del backend Laravel. */
public interface DonacionApi {

  @POST("api/donaciones")
  Call<DonacionDto> store(@Body CreateDonacionRequest body);

  /**
   * Autoexclusion confidencial por numero de donacion. El body lleva solo
   * {@code motivo} opcional; no se manda identidad del paciente.
   */
  @POST("api/donaciones/{numero}/autoexclusion")
  Call<AutoexclusionDto> autoexclusion(@Path("numero") String numero, @Body Map<String, String> body);
}
