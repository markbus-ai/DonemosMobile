package com.donemosmobile.data.pacientes;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.auth.SessionInvalidator;
import com.donemosmobile.data.cache.CacheKeys;
import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiCalls;
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

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * Acceso a pacientes. Corre la red con {@code enqueue} de Retrofit (fuera del
 * hilo principal) y entrega el resultado por callback en el hilo principal,
 * con el ruteo de errores comun de {@link ApiCalls}.
 */
public class PacienteRepository {

  private final PacienteApi api;
  private final ApiCalls calls;

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  /**
   * Gson propio de esta peticion: serializa explicitamente los {@code null}
   * para que vaciar un contador lo reinicie en el backend.
   */
  private static final Gson EXPLICIT_NULLS = new GsonBuilder().serializeNulls().create();

  public PacienteRepository(Context context) {
    this.api = ApiClient.get(context).pacientes();
    this.calls = new ApiCalls(SessionInvalidator::invalidate);
  }

  /** Busca un paciente por DNI. 404 responde {@link ApiError.Type#NOT_FOUND}. */
  public void findByDni(String dni, final ApiCallback<PacienteDto> callback) {
    final String key = CacheKeys.paciente(dni);
    calls.enqueue(api.show(dni), paciente -> {
      DataCache.getInstance().put(key, paciente);
      return paciente;
    }, callback);
  }

  /**
   * Devuelve la ficha cacheada de ese DNI, o {@code null} si todavia no se
   * busco en esta sesion. La pantalla la pinta al instante y revalida detras.
   */
  @Nullable
  public PacienteDto getCachedByDni(String dni) {
    return DataCache.getInstance().get(CacheKeys.paciente(dni), PacienteDto.class);
  }

  /**
   * Finds a patient by exact DNI with the {@code aptitud} relation embedded.
   * Existence comes from the exact detail endpoint ({@code GET
   * /api/pacientes/{dni}}), so a 404 is authoritative and a DNI is never missed
   * because a broad search page did not include it. The detail returns only
   * {@code aptitud_id}; the relation is enriched from the search listing when
   * it contains the exact match, otherwise the detail is returned as is.
   */
  public void findByDniWithAptitud(String dni, final ApiCallback<PacienteDto> callback) {
    final String key = CacheKeys.paciente(dni);
    calls.enqueue(api.show(dni), paciente -> paciente, new ApiCallback<PacienteDto>() {
      @Override
      public void onSuccess(@Nullable PacienteDto detail) {
        if (detail != null && detail.aptitud != null) {
          DataCache.getInstance().put(key, detail);
          callback.onSuccess(detail);
          return;
        }
        enrichAptitud(dni, detail, callback);
      }

      @Override
      public void onError(@NonNull ApiError.Type type, @Nullable String message) {
        callback.onError(type, message);
      }
    });
  }

  private void enrichAptitud(String dni, PacienteDto detail, ApiCallback<PacienteDto> callback) {
    final String key = CacheKeys.paciente(dni);
    calls.enqueue(api.index(dni, 50), page -> page, new ApiCallback<PacientePageDto>() {
      @Override
      public void onSuccess(@Nullable PacientePageDto page) {
        PacienteDto result = detail;
        if (page != null && page.data != null) {
          for (PacienteDto paciente : page.data) {
            if (dni.equals(paciente.dni)) {
              result = paciente;
              break;
            }
          }
        }
        DataCache.getInstance().put(key, result);
        callback.onSuccess(result);
      }

      @Override
      public void onError(@NonNull ApiError.Type type, @Nullable String message) {
        if (type == ApiError.Type.UNAUTHORIZED) {
          callback.onError(type, message);
          return;
        }
        // The patient exists (detail succeeded): degrade to the detail only.
        if (type != ApiError.Type.NETWORK) {
          DataCache.getInstance().put(key, detail);
        }
        callback.onSuccess(detail);
      }
    });
  }

  /** Da de alta un paciente nuevo. 422 responde por campo. */
  public void create(CreatePacienteRequest body, final ApiCallback<PacienteDto> callback) {
    calls.enqueueWrite(api.store(body), paciente -> {
      // Deja la ficha fresca para que la busqueda por DNI sea instantanea.
      if (paciente.dni != null) {
        DataCache.getInstance().put(CacheKeys.paciente(paciente.dni), paciente);
      }
      return paciente;
    }, callback);
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
      final ApiCallback<PacienteDto> callback) {
    JsonObject body = new JsonObject();
    body.add("ciclo_actual", cycleValue(ciclo));
    body.add("serie_actual", cycleValue(serie));
    body.add("donacion_actual", cycleValue(donacion));
    RequestBody request = RequestBody.create(EXPLICIT_NULLS.toJson(body), JSON);

    calls.enqueueWrite(api.update(dni, request), paciente -> {
      DataCache.getInstance().put(CacheKeys.paciente(dni), paciente);
      return paciente;
    }, callback);
  }

  private static JsonElement cycleValue(Integer value) {
    return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
  }
}
