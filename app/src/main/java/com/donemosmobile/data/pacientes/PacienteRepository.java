package com.donemosmobile.data.pacientes;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.ApiError;
import com.donemosmobile.data.network.PacienteApi;
import com.donemosmobile.data.network.dto.CreatePacienteRequest;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.PacientePageDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Acceso a pacientes. Corre la red con {@code enqueue} de Retrofit (fuera del
 * hilo principal) y entrega el resultado por callback en el hilo principal.
 * Reutiliza el mismo patrón que {@link com.donemosmobile.data.auth.AuthRepository}.
 */
public class PacienteRepository {

  public interface Callback<T> {
    void onSuccess(@Nullable T data);
    void onError(@NonNull ApiError.Type type, @Nullable String message);

    /**
     * Errores de validacion por campo (Laravel 422): campo -> mensaje. Por
     * defecto cae a {@link #onError} para que ninguna pantalla quede muda.
     */
    default void onValidationError(@NonNull Map<String, String> fieldErrors) {
      onError(ApiError.Type.CLIENT, null);
    }
  }

  private final PacienteApi api;
  private final TokenStore tokenStore;

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  /**
   * Gson propio de esta peticion: serializa explicitamente los {@code null}
   * para que vaciar un contador lo reinicie en el backend.
   */
  private static final Gson EXPLICIT_NULLS = new GsonBuilder().serializeNulls().create();

  public PacienteRepository(Context context) {
    ApiClient client = ApiClient.get(context);
    this.api = client.pacientes();
    this.tokenStore = client.tokenStore();
  }

  /** Busca un paciente por DNI. 404 responde {@link ApiError.Type#NOT_FOUND}. */
  public void findByDni(String dni, final Callback<PacienteDto> callback) {
    final String key = cacheKey(dni);
    api.show(dni).enqueue(new retrofit2.Callback<PacienteDto>() {
      @Override
      public void onResponse(@NonNull Call<PacienteDto> call,
          @NonNull Response<PacienteDto> response) {
        if (response.isSuccessful() && response.body() != null) {
          DataCache.getInstance().put(key, response.body());
          callback.onSuccess(response.body());
          return;
        }
        handleError(response, callback);
      }

      @Override
      public void onFailure(@NonNull Call<PacienteDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  /**
   * Devuelve la ficha cacheada de ese DNI, o {@code null} si todavia no se
   * busco en esta sesion. La pantalla la pinta al instante y revalida detras.
   */
  @Nullable
  public PacienteDto getCachedByDni(String dni) {
    Object value = DataCache.getInstance().get(cacheKey(dni));
    return value instanceof PacienteDto ? (PacienteDto) value : null;
  }

  /**
   * Busca por DNI con la relacion {@code aptitud} embebida. El detalle
   * ({@code GET /api/pacientes/{dni}}) devuelve el modelo crudo, solo
   * {@code aptitud_id}; el listado con {@code search=DNI} trae el tipo real,
   * que es lo que la pantalla de aptitud necesita mostrar.
   */
  public void findByDniWithAptitud(String dni, final Callback<PacienteDto> callback) {
    final String key = cacheKey(dni);
    api.index(dni, 50).enqueue(new retrofit2.Callback<PacientePageDto>() {
      @Override
      public void onResponse(@NonNull Call<PacientePageDto> call,
          @NonNull Response<PacientePageDto> response) {
        if (response.isSuccessful() && response.body() != null
            && response.body().data != null) {
          for (PacienteDto paciente : response.body().data) {
            if (dni.equals(paciente.dni)) {
              DataCache.getInstance().put(key, paciente);
              callback.onSuccess(paciente);
              return;
            }
          }
          // No vino coincidencia exacta: el paciente no existe.
          callback.onError(ApiError.Type.NOT_FOUND, null);
          return;
        }
        handleError(response, callback);
      }

      @Override
      public void onFailure(@NonNull Call<PacientePageDto> call, @NonNull Throwable t) {
        callback.onError(ApiError.Type.NETWORK, t.getMessage());
      }
    });
  }

  private String cacheKey(String dni) {
    return "paciente:" + dni;
  }

  /** Da de alta un paciente nuevo. 422 responde {@link ApiError.Type#CLIENT}. */
  public void create(CreatePacienteRequest body, final Callback<PacienteDto> callback) {
    api.store(body).enqueue(new retrofit2.Callback<PacienteDto>() {
      @Override
      public void onResponse(@NonNull Call<PacienteDto> call,
          @NonNull Response<PacienteDto> response) {
        if (response.isSuccessful() && response.body() != null) {
          // Deja la ficha fresca para que la busqueda por DNI sea instantanea.
          if (response.body().dni != null) {
            DataCache.getInstance().put(cacheKey(response.body().dni), response.body());
          }
          callback.onSuccess(response.body());
          return;
        }
        // 422: errores por campo para que la pantalla los pinte inline.
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

  /**
   * Actualiza los contadores manuales del ciclo ({@code PATCH
   * /api/pacientes/{dni}}). Manda las tres claves siempre, con {@code null}
   * explicito cuando el campo quedo vacio: el contrato usa
   * {@code sometimes|nullable}, asi que omitir una clave la dejaria intacta y
   * no la reiniciaria. La respuesta trae el modelo crudo y reemplaza la ficha
   * cacheada del DNI.
   */
  public void updateCiclo(String dni, Integer ciclo, Integer serie, Integer donacion,
      final Callback<PacienteDto> callback) {
    JsonObject body = new JsonObject();
    body.add("ciclo_actual", cycleValue(ciclo));
    body.add("serie_actual", cycleValue(serie));
    body.add("donacion_actual", cycleValue(donacion));
    RequestBody request = RequestBody.create(EXPLICIT_NULLS.toJson(body), JSON);

    api.update(dni, request).enqueue(new retrofit2.Callback<PacienteDto>() {
      @Override
      public void onResponse(@NonNull Call<PacienteDto> call,
          @NonNull Response<PacienteDto> response) {
        if (response.isSuccessful() && response.body() != null) {
          DataCache.getInstance().put(cacheKey(dni), response.body());
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

  private static JsonElement cycleValue(Integer value) {
    return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
  }

  private <T> void handleError(Response<?> response, Callback<T> callback) {
    ApiError.Type type = ApiError.fromStatus(response.code());
    if (type == ApiError.Type.UNAUTHORIZED) {
      tokenStore.clear();
    }
    callback.onError(type, ApiError.message(response));
  }
}
