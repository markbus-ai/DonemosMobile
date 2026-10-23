package com.donemosmobile.data.donaciones;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.ui.auth.SessionExpired;
import com.donemosmobile.data.network.DonacionApi;
import com.donemosmobile.data.network.dto.AutoexclusionDto;
import com.donemosmobile.data.network.dto.CreateDonacionRequest;
import com.donemosmobile.data.network.dto.DonacionDto;
import com.donemosmobile.data.network.dto.DonacionWarningResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Acceso a donaciones. Corre la red con {@code enqueue} y entrega el resultado
 * por callback, igual que el resto de los repositorios. Distingue tres finales:
 * 201 (creada), 409 con warnings overrideables y 422 con errores por campo.
 */
public class DonacionRepository {

  public interface Callback<T> {
    void onSuccess(@Nullable T data);
    void onError(@NonNull ApiError.Type type, @Nullable String message);

    /** 422 de Laravel: campo -> primer mensaje. */
    default void onValidationError(@NonNull Map<String, String> fieldErrors) {
      onError(ApiError.Type.CLIENT, null);
    }

    /** 409: reglas de negocio violadas, overrideables con {@code forzar}. */
    default void onWarning(@NonNull DonacionWarningResponse warnings) {
      onError(ApiError.Type.CLIENT, null);
    }
  }

  private final DonacionApi api;
  private final TokenStore tokenStore;
  private final Context appContext;
  private final Gson gson = new Gson();

  public DonacionRepository(Context context) {
    this.appContext = context.getApplicationContext();
    ApiClient client = ApiClient.get(context);
    this.api = client.donaciones();
    this.tokenStore = client.tokenStore();
  }

  /** Crea una donacion. 409 -> {@link Callback#onWarning}, 422 -> por campo. */
  public void create(CreateDonacionRequest body, final Callback<DonacionDto> callback) {
    api.store(body).enqueue(new retrofit2.Callback<DonacionDto>() {
      @Override
      public void onResponse(@NonNull Call<DonacionDto> call,
          @NonNull Response<DonacionDto> response) {
        if (response.isSuccessful() && response.body() != null) {
          callback.onSuccess(response.body());
          return;
        }
        if (response.code() == 409 && response.errorBody() != null) {
          DonacionWarningResponse warnings = parseWarnings(response);
          callback.onWarning(warnings != null ? warnings : new DonacionWarningResponse());
          return;
        }
        ApiError.Body error = ApiError.read(response);
        if (ApiError.fromStatus(response.code()) == ApiError.Type.CLIENT
            && !error.fieldErrors.isEmpty()) {
          callback.onValidationError(error.fieldErrors);
          return;
        }
        handleError(response.code(), error, callback);
      }

      @Override
      public void onFailure(@NonNull Call<DonacionDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  /** Marca la autoexclusion confidencial de una donacion por su numero. */
  public void marcarAutoexclusion(String numero, @Nullable String motivo,
      final Callback<AutoexclusionDto> callback) {
    Map<String, String> body = new HashMap<>();
    if (motivo != null && !motivo.trim().isEmpty()) {
      body.put("motivo", motivo.trim());
    }
    api.autoexclusion(numero, body).enqueue(new retrofit2.Callback<AutoexclusionDto>() {
      @Override
      public void onResponse(@NonNull Call<AutoexclusionDto> call,
          @NonNull Response<AutoexclusionDto> response) {
        if (response.isSuccessful() && response.body() != null) {
          callback.onSuccess(response.body());
          return;
        }
        ApiError.Body error = ApiError.read(response);
        if (ApiError.fromStatus(response.code()) == ApiError.Type.CLIENT
            && !error.fieldErrors.isEmpty()) {
          callback.onValidationError(error.fieldErrors);
          return;
        }
        handleError(response.code(), error, callback);
      }

      @Override
      public void onFailure(@NonNull Call<AutoexclusionDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  @Nullable
  private DonacionWarningResponse parseWarnings(Response<?> response) {
    if (response.errorBody() == null) {
      return null;
    }
    try {
      return gson.fromJson(response.errorBody().string(), DonacionWarningResponse.class);
    } catch (IOException | com.google.gson.JsonSyntaxException e) {
      return null;
    }
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
