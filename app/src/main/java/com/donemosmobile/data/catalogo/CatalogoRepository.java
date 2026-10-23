package com.donemosmobile.data.catalogo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.ui.auth.SessionExpired;
import com.donemosmobile.data.network.CatalogoApi;
import com.donemosmobile.data.network.dto.MotivoDto;
import com.donemosmobile.data.network.dto.TipoDonacionDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Catalogos de referencia: motivos de observacion/deferral
 * ({@code GET /api/motivos}) y tipos de donacion ({@code GET
 * /api/tipos-donacion}). Se cachean en memoria porque cambian poco.
 */
public class CatalogoRepository {

  public interface Callback<T> {
    void onSuccess(@Nullable T data);
    void onError(@NonNull ApiError.Type type, @Nullable String message);
  }

  private static final String MOTIVOS_KEY = "motivos";
  private static final String TIPOS_DONACION_KEY = "tipos_donacion";

  private final CatalogoApi api;
  private final TokenStore tokenStore;
  private final Context appContext;

  public CatalogoRepository(Context context) {
    this.appContext = context.getApplicationContext();
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
        handleError(response.code(), ApiError.read(response), callback);
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

  /** Lista los tipos de donacion y deja el resultado cacheado. */
  public void listTiposDonacion(final Callback<List<TipoDonacionDto>> callback) {
    api.tiposDonacion().enqueue(new retrofit2.Callback<List<TipoDonacionDto>>() {
      @Override
      public void onResponse(@NonNull Call<List<TipoDonacionDto>> call,
          @NonNull Response<List<TipoDonacionDto>> response) {
        if (response.isSuccessful() && response.body() != null) {
          DataCache.getInstance().put(TIPOS_DONACION_KEY, response.body());
          callback.onSuccess(response.body());
          return;
        }
        handleError(response.code(), ApiError.read(response), callback);
      }

      @Override
      public void onFailure(@NonNull Call<List<TipoDonacionDto>> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  /** Tipos de donacion cacheados, o {@code null} si todavia no se pidieron. */
  @Nullable
  @SuppressWarnings("unchecked")
  public List<TipoDonacionDto> getCachedTiposDonacion() {
    Object value = DataCache.getInstance().get(TIPOS_DONACION_KEY);
    return value instanceof List ? (List<TipoDonacionDto>) value : null;
  }

  private <T> void handleError(int code, ApiError.Body error, Callback<T> callback) {
    ApiError.Type type = ApiError.fromStatus(code);
    if (type == ApiError.Type.UNAUTHORIZED) {
      // Central 401 handling: wipe session + cache and route to login.
      SessionExpired.handle(appContext);
    }
    callback.onError(type, error.message);
  }
}
