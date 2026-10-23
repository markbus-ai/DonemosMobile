package com.donemosmobile.data.turnos;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.ui.auth.SessionExpired;
import com.donemosmobile.data.network.TurnoApi;
import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.data.network.dto.TurnoWarningResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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

    /** 422 de Laravel: campo -> primer mensaje. Cae a onError por defecto. */
    default void onValidationError(@NonNull java.util.Map<String, String> fieldErrors) {
      onError(ApiError.Type.CLIENT, null);
    }

    /** 409 rule envelope of {@code POST /api/turnos}. Falls back to onError. */
    default void onWarning(@NonNull TurnoWarningResponse warning) {
      onError(ApiError.Type.CLIENT, warning.message);
    }
  }

  private final TurnoApi api;
  private final TokenStore tokenStore;
  private final Context appContext;
  private final Gson gson = new Gson();

  public TurnoRepository(Context context) {
    this.appContext = context.getApplicationContext();
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
    fetch(api.index(fecha, null, pacienteId), cacheKey(fecha, pacienteId), callback);
  }

  /** Lists turnos from {@code desde} ({@code YYYY-MM-DD}) onwards, ordered by fecha/hora. */
  public void listDesde(@NonNull String desde, final Callback<List<TurnoDto>> callback) {
    fetch(api.index(null, desde, null), desdeKey(desde), callback);
  }

  /** Last cached {@link #listDesde} result, or {@code null}. */
  @Nullable
  @SuppressWarnings("unchecked")
  public List<TurnoDto> getCachedDesde(@NonNull String desde) {
    Object value = DataCache.getInstance().get(desdeKey(desde));
    return value instanceof List ? (List<TurnoDto>) value : null;
  }

  private void fetch(Call<List<TurnoDto>> request, final String key,
      final Callback<List<TurnoDto>> callback) {
    request.enqueue(new retrofit2.Callback<List<TurnoDto>>() {
      @Override
      public void onResponse(@NonNull Call<List<TurnoDto>> call,
          @NonNull Response<List<TurnoDto>> response) {
        if (response.isSuccessful() && response.body() != null) {
          DataCache.getInstance().put(key, response.body());
          callback.onSuccess(response.body());
          return;
        }
        handleError(response.code(), ApiError.read(response), callback);
      }

      @Override
      public void onFailure(@NonNull Call<List<TurnoDto>> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  /**
   * Devuelve la ultima lista cacheada para ese filtro, o {@code null} si no hay
   * nada. La usa la pantalla para pintar al instante mientras revalida.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public List<TurnoDto> getCachedList(@Nullable String fecha, @Nullable Long pacienteId) {
    Object value = DataCache.getInstance().get(cacheKey(fecha, pacienteId));
    return value instanceof List ? (List<TurnoDto>) value : null;
  }

  /** {@code true} si el cache de esa lista sigue fresco (dentro del umbral). */
  public boolean isCachedListFresh(@Nullable String fecha, @Nullable Long pacienteId) {
    return DataCache.getInstance().isFresh(cacheKey(fecha, pacienteId));
  }

  /**
   * Calienta el cache en segundo plano. Fire-and-forget: no expone callback
   * porque nadie espera el resultado. Si ya hay datos frescos no vuelve a pedir.
   */
  public void prefetch(@Nullable String fecha, @Nullable Long pacienteId) {
    if (isCachedListFresh(fecha, pacienteId)) {
      return;
    }
    list(fecha, pacienteId, new Callback<List<TurnoDto>>() {
      @Override
      public void onSuccess(@Nullable List<TurnoDto> data) {
        // El cache ya se actualizo dentro de list().
      }

      @Override
      public void onError(@NonNull ApiError.Type type, @Nullable String message) {
        // Prefetch best-effort: si falla, la pantalla lo reintenta al abrir.
      }
    });
  }

  private String desdeKey(String desde) {
    return "turnos:desde:" + desde;
  }

  private String cacheKey(@Nullable String fecha, @Nullable Long pacienteId) {
    return "turnos:" + (fecha != null ? fecha : "all")
        + ":" + (pacienteId != null ? pacienteId : "all");
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
        ApiError.Body error = ApiError.read(response);
        if (response.code() == 409) {
          TurnoWarningResponse warning = parseWarning(error);
          if (warning != null && (warning.warning || !warning.codes().isEmpty())) {
            callback.onWarning(warning);
            return;
          }
        }
        if (response.code() == 422 && !error.fieldErrors.isEmpty()) {
          callback.onValidationError(error.fieldErrors);
          return;
        }
        handleError(response.code(), error, callback);
      }

      @Override
      public void onFailure(@NonNull Call<TurnoDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  /**
   * Moves a PENDIENTE turno to {@code ATENDIDO} or {@code CANCELADO}. Any other
   * origin state answers 422 {@code estado}, routed to onValidationError.
   */
  public void updateEstado(long id, @NonNull String estado, final Callback<TurnoDto> callback) {
    java.util.Map<String, String> body = new java.util.HashMap<>();
    body.put("estado", estado);
    api.update(id, body).enqueue(new retrofit2.Callback<TurnoDto>() {
      @Override
      public void onResponse(@NonNull Call<TurnoDto> call,
          @NonNull Response<TurnoDto> response) {
        if (response.isSuccessful()) {
          callback.onSuccess(response.body());
          return;
        }
        ApiError.Body error = ApiError.read(response);
        if (response.code() == 422 && !error.fieldErrors.isEmpty()) {
          callback.onValidationError(error.fieldErrors);
          return;
        }
        handleError(response.code(), error, callback);
      }

      @Override
      public void onFailure(@NonNull Call<TurnoDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  @Nullable
  private TurnoWarningResponse parseWarning(ApiError.Body error) {
    if (error.raw == null) {
      return null;
    }
    try {
      return gson.fromJson(error.raw, TurnoWarningResponse.class);
    } catch (JsonSyntaxException | IllegalStateException e) {
      return null;
    }
  }

  private <T> void handleError(int code, ApiError.Body error, Callback<T> callback) {
    ApiError.Type type = ApiError.fromStatus(code);
    if (type == ApiError.Type.UNAUTHORIZED) {
      // Central 401 handling: wipe session + cache and route to login.
      SessionExpired.handle(appContext);
    }
    callback.onError(type, extractMessage(error));
  }

  /**
   * Prefers the turnos warning envelope ({@code code}/{@code message}) so the
   * UI shows the real reason for a 409, falling back to the standard message.
   */
  @Nullable
  private String extractMessage(ApiError.Body error) {
    if (error.raw != null) {
      try {
        TurnoWarningResponse warning = gson.fromJson(error.raw, TurnoWarningResponse.class);
        if (warning != null && warning.message != null && !warning.message.isEmpty()) {
          return warning.message;
        }
      } catch (JsonSyntaxException | IllegalStateException ignored) {
        // Falls back to the standard parser.
      }
    }
    return error.message;
  }
}
