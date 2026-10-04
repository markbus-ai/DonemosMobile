package com.donemosmobile.data.turnos;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.TurnoApi;
import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.data.network.dto.TurnoWarningResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Acceso a turnos. Corre la red con {@code enqueue} (fuera del hilo principal)
 * y entrega el resultado por callback. El alta que viola reglas de negocio
 * responde {@code 409} con un envelope accionable ({@link ApiError.Type#CLIENT}).
 */
public class TurnoRepository {

  public interface Callback<T> {
    void onSuccess(@Nullable T data);
    void onError(@NonNull ApiError.Type type, @Nullable String message);
  }

  private final TurnoApi api;
  private final TokenStore tokenStore;
  private final Gson gson = new Gson();

  public TurnoRepository(Context context) {
    ApiClient client = ApiClient.get(context);
    this.api = client.turnos();
    this.tokenStore = client.tokenStore();
  }

  /**
   * Lista turnos, opcionalmente filtrados por fecha ({@code YYYY-MM-DD}) y/o
   * paciente. El backend ordena por fecha y hora.
   */
  public void list(@Nullable String fecha, @Nullable Long pacienteId,
      final Callback<List<TurnoDto>> callback) {
    api.index(fecha, pacienteId).enqueue(new retrofit2.Callback<List<TurnoDto>>() {
      @Override
      public void onResponse(@NonNull Call<List<TurnoDto>> call,
          @NonNull Response<List<TurnoDto>> response) {
        if (response.isSuccessful() && response.body() != null) {
          callback.onSuccess(response.body());
          return;
        }
        handleError(response, callback);
      }

      @Override
      public void onFailure(@NonNull Call<List<TurnoDto>> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  /** Crea un turno. Un 409 trae el mensaje de negocio (capacidad, intervalo). */
  public void create(CreateTurnoRequest body, final Callback<TurnoDto> callback) {
    api.store(body).enqueue(new retrofit2.Callback<TurnoDto>() {
      @Override
      public void onResponse(@NonNull Call<TurnoDto> call,
          @NonNull Response<TurnoDto> response) {
        if (response.isSuccessful() && response.body() != null) {
          callback.onSuccess(response.body());
          return;
        }
        handleError(response, callback);
      }

      @Override
      public void onFailure(@NonNull Call<TurnoDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  private <T> void handleError(Response<?> response, Callback<T> callback) {
    ApiError.Type type = ApiError.fromStatus(response.code());
    if (type == ApiError.Type.UNAUTHORIZED) {
      tokenStore.clear();
    }
    callback.onError(type, extractMessage(response));
  }

  /**
   * Igual que {@link ApiError#message(Response)}, pero priorizando el envelope
   * de warning de turnos ({@code code}/{@code message}) para que la UI muestre
   * el motivo real de un 409.
   */
  @Nullable
  private String extractMessage(Response<?> response) {
    if (response.errorBody() == null) {
      return null;
    }
    String raw;
    try {
      raw = response.errorBody().string();
    } catch (IOException e) {
      return null;
    }
    try {
      TurnoWarningResponse warning = gson.fromJson(raw, TurnoWarningResponse.class);
      if (warning != null && warning.message != null && !warning.message.isEmpty()) {
        return warning.message;
      }
    } catch (JsonSyntaxException ignored) {
      // Cae al parser de error estándar.
    }
    try {
      com.donemosmobile.data.network.dto.ErrorResponse error =
          gson.fromJson(raw, com.donemosmobile.data.network.dto.ErrorResponse.class);
      return error != null ? error.message : null;
    } catch (JsonSyntaxException e) {
      return null;
    }
  }
}
