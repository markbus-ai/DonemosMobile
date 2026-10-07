package com.donemosmobile.data.aptitud;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.PacienteApi;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.UpdateAptitudRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Actualiza la aptitud de un paciente ({@code PATCH
 * /api/pacientes/{dni}/aptitud}). Corre la red con {@code enqueue} y entrega el
 * resultado por callback, igual que el resto de los repositorios.
 */
public class AptitudRepository {

  public interface Callback<T> {
    void onSuccess(@Nullable T data);
    void onError(@NonNull ApiError.Type type, @Nullable String message);

    /** Errores de validacion por campo (Laravel 422): campo -> mensaje. */
    default void onValidationError(@NonNull Map<String, String> fieldErrors) {
      onError(ApiError.Type.CLIENT, null);
    }
  }

  private final PacienteApi api;
  private final TokenStore tokenStore;

  public AptitudRepository(Context context) {
    ApiClient client = ApiClient.get(context);
    this.api = client.pacientes();
    this.tokenStore = client.tokenStore();
  }

  /**
   * Aplica la nueva aptitud. La respuesta trae la relacion {@code aptitud}
   * embebida, asi que la pantalla puede refrescar el estado sin otro GET.
   */
  public void update(String dni, UpdateAptitudRequest body, final Callback<PacienteDto> callback) {
    api.updateAptitud(dni, body).enqueue(new retrofit2.Callback<PacienteDto>() {
      @Override
      public void onResponse(@NonNull Call<PacienteDto> call,
          @NonNull Response<PacienteDto> response) {
        if (response.isSuccessful() && response.body() != null) {
          // Deja la ficha actualizada para que el lookup por DNI la muestre.
          DataCache.getInstance().put("paciente:" + dni, response.body());
          callback.onSuccess(response.body());
          return;
        }
        if (ApiError.fromStatus(response.code()) == ApiError.Type.CLIENT) {
          Map<String, String> fields = ApiError.fieldErrors(response);
          if (!fields.isEmpty()) {
            callback.onValidationError(fields);
            return;
          }
        }
        handleError(response, callback);
      }

      @Override
      public void onFailure(@NonNull Call<PacienteDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  private <T> void handleError(Response<?> response, Callback<T> callback) {
    ApiError.Type type = ApiError.fromStatus(response.code());
    if (type == ApiError.Type.UNAUTHORIZED) {
      tokenStore.clear();
    }
    callback.onError(type, ApiError.message(response));
  }
}
