package com.donemosmobile.data.donaciones;

import android.content.Context;

import androidx.annotation.Nullable;

import com.donemosmobile.data.auth.SessionInvalidator;
import com.donemosmobile.data.cache.CacheKeys;
import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiCalls;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.DonacionApi;
import com.donemosmobile.data.network.WarningCallback;
import com.donemosmobile.data.network.dto.AutoexclusionDto;
import com.donemosmobile.data.network.dto.CreateDonacionRequest;
import com.donemosmobile.data.network.dto.DonacionDto;
import com.donemosmobile.data.network.dto.DonacionWarningResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.Map;

/**
 * Acceso a donaciones. Corre la red con {@code enqueue} y entrega el resultado
 * por callback, igual que el resto de los repositorios. Distingue tres finales:
 * 201 (creada), 409 con warnings overrideables y 422 con errores por campo.
 */
public class DonacionRepository {

  private static final Gson GSON = new Gson();

  private final DonacionApi api;
  private final ApiCalls calls;

  public DonacionRepository(Context context) {
    this.api = ApiClient.get(context).donaciones();
    this.calls = new ApiCalls(SessionInvalidator::invalidate);
  }

  /**
   * Crea una donacion para el paciente {@code pacienteDni}. 409 ->
   * {@link WarningCallback#onWarning}, 422 -> por campo. Al crearse, la ficha
   * cacheada del paciente queda vieja (posicion del ciclo, ultima donacion), asi
   * que se invalida y el proximo lookup la vuelve a pedir.
   */
  public void create(String pacienteDni, CreateDonacionRequest body,
      final WarningCallback<DonacionDto, DonacionWarningResponse> callback) {
    calls.enqueueWrite(api.store(body), donacion -> {
      if (pacienteDni != null) {
        DataCache.getInstance().invalidate(CacheKeys.paciente(pacienteDni));
      }
      return donacion;
    }, DonacionRepository::parseWarnings, callback);
  }

  /** Marca la autoexclusion confidencial de una donacion por su numero. */
  public void marcarAutoexclusion(String numero, @Nullable String motivo,
      final ApiCallback<AutoexclusionDto> callback) {
    Map<String, String> body = new HashMap<>();
    if (motivo != null && !motivo.trim().isEmpty()) {
      body.put("motivo", motivo.trim());
    }
    calls.enqueueWrite(api.autoexclusion(numero, body), result -> result, callback);
  }

  /** The 409 envelope, or {@code null} when the body is missing or not JSON. */
  @Nullable
  static DonacionWarningResponse parseWarnings(ApiError.Body error) {
    if (error.raw == null) {
      return null;
    }
    try {
      return GSON.fromJson(error.raw, DonacionWarningResponse.class);
    } catch (JsonSyntaxException | IllegalStateException e) {
      return null;
    }
  }
}
