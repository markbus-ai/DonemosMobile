package com.donemosmobile.data.catalogo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.CatalogoApi;
import com.donemosmobile.data.network.dto.MotivoDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Catalogos de referencia. Hoy solo los motivos de observacion/deferral
 * ({@code GET /api/motivos}); se cachean en memoria porque cambian poco.
 */
public class CatalogoRepository {

  public interface Callback<T> {
    void onSuccess(@Nullable T data);
    void onError(@NonNull ApiError.Type type, @Nullable String message);
  }

  private static final String MOTIVOS_KEY = "motivos";

  private final CatalogoApi api;
  private final TokenStore tokenStore;

  public CatalogoRepository(Context context) {
    ApiClient client = ApiClient.get(context);
    this.api = client.catalogo();
    this.tokenStore = client.tokenStore();
  }

  /** Lista los motivos y deja el resultado cacheado. */
  public void listMotivos(final Callback<List<MotivoDto>> callback) {
    api.motivos().enqueue(new retrofit2.Callback<List<MotivoDto>>() {
      @Override
      public void onResponse(@NonNull Call<List<MotivoDto>> call,
          @NonNull Response<List<MotivoDto>> response) {
        if (response.isSuccessful() && response.body() != null) {
          DataCache.getInstance().put(MOTIVOS_KEY, response.body());
          callback.onSuccess(response.body());
          return;
        }
        handleError(response, callback);
      }

      @Override
      public void onFailure(@NonNull Call<List<MotivoDto>> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  /** Motivos cacheados de la sesion, o {@code null} si todavia no se pidieron. */
  @Nullable
  @SuppressWarnings("unchecked")
  public List<MotivoDto> getCachedMotivos() {
    Object value = DataCache.getInstance().get(MOTIVOS_KEY);
    return value instanceof List ? (List<MotivoDto>) value : null;
  }

  private <T> void handleError(Response<?> response, Callback<T> callback) {
    ApiError.Type type = ApiError.fromStatus(response.code());
    if (type == ApiError.Type.UNAUTHORIZED) {
      tokenStore.clear();
    }
    callback.onError(type, ApiError.message(response));
  }
}
