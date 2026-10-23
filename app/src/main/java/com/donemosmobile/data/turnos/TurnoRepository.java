package com.donemosmobile.data.turnos;

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
import com.donemosmobile.data.network.TurnoApi;
import com.donemosmobile.data.network.WarningCallback;
import com.donemosmobile.data.network.dto.CreateTurnoRequest;
import com.donemosmobile.data.network.dto.TurnoDto;
import com.donemosmobile.data.network.dto.TurnoWarningResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

/**
 * Acceso a turnos. Corre la red con {@code enqueue} (fuera del hilo principal)
 * y entrega el resultado por callback. El alta que viola reglas de negocio
 * responde {@code 409} con un envelope accionable ({@link WarningCallback#onWarning}).
 */
public class TurnoRepository {

  private static final Gson GSON = new Gson();

  private final TurnoApi api;
  private final ApiCalls calls;

  public TurnoRepository(Context context) {
    this.api = ApiClient.get(context).turnos();
    this.calls = new ApiCalls(SessionInvalidator::invalidate);
  }

  /**
   * Lista turnos, opcionalmente filtrados por fecha ({@code YYYY-MM-DD}) y/o
   * paciente. El backend ordena por fecha y hora.
   */
  public void list(@Nullable String fecha, @Nullable Long pacienteId,
      final ApiCallback<List<TurnoDto>> callback) {
    fetch(api.index(fecha, null, pacienteId), CacheKeys.turnos(fecha, pacienteId), callback);
  }

  /** Lists turnos from {@code desde} ({@code YYYY-MM-DD}) onwards, ordered by fecha/hora. */
  public void listDesde(@NonNull String desde, final ApiCallback<List<TurnoDto>> callback) {
    fetch(api.index(null, desde, null), CacheKeys.turnosDesde(desde), callback);
  }

  /** Last cached {@link #listDesde} result, or {@code null}. */
  @Nullable
  public List<TurnoDto> getCachedDesde(@NonNull String desde) {
    return DataCache.getInstance().getList(CacheKeys.turnosDesde(desde), TurnoDto.class);
  }

  private void fetch(Call<List<TurnoDto>> request, final String key,
      final ApiCallback<List<TurnoDto>> callback) {
    calls.enqueue(request, turnos -> {
      DataCache.getInstance().put(key, turnos);
      return turnos;
    }, callback);
  }

  /**
   * Devuelve la ultima lista cacheada para ese filtro, o {@code null} si no hay
   * nada. La usa la pantalla para pintar al instante mientras revalida.
   */
  @Nullable
  public List<TurnoDto> getCachedList(@Nullable String fecha, @Nullable Long pacienteId) {
    return DataCache.getInstance().getList(CacheKeys.turnos(fecha, pacienteId), TurnoDto.class);
  }

  /** {@code true} si el cache de esa lista sigue fresco (dentro del umbral). */
  public boolean isCachedListFresh(@Nullable String fecha, @Nullable Long pacienteId) {
    return DataCache.getInstance().isFresh(CacheKeys.turnos(fecha, pacienteId));
  }

  /**
   * Calienta el cache en segundo plano. Fire-and-forget: no expone callback
   * porque nadie espera el resultado. Si ya hay datos frescos no vuelve a pedir.
   */
  public void prefetch(@Nullable String fecha, @Nullable Long pacienteId) {
    if (isCachedListFresh(fecha, pacienteId)) {
      return;
    }
    list(fecha, pacienteId, new ApiCallback<List<TurnoDto>>() {
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

  /** Crea un turno. Un 409 trae el mensaje de negocio (capacidad, intervalo). */
  public void create(CreateTurnoRequest body,
      final WarningCallback<TurnoDto, TurnoWarningResponse> callback) {
    calls.enqueueWrite(api.store(body), turno -> turno, TurnoRepository::parseWarning, callback);
  }

  /**
   * Moves a PENDIENTE turno to {@code ATENDIDO} or {@code CANCELADO}. Any other
   * origin state answers 422 {@code estado}, routed to onValidationError.
   */
  public void updateEstado(long id, @NonNull String estado, final ApiCallback<TurnoDto> callback) {
    Map<String, String> body = new HashMap<>();
    body.put("estado", estado);
    calls.enqueueWriteAllowingEmpty(api.update(id, body), callback);
  }

  /**
   * The 409 rule envelope, or {@code null} when the body is not one (no
   * {@code warning} flag and no rule code): that 409 is a plain error.
   */
  @Nullable
  static TurnoWarningResponse parseWarning(ApiError.Body error) {
    if (error.raw == null) {
      return null;
    }
    try {
      TurnoWarningResponse warning = GSON.fromJson(error.raw, TurnoWarningResponse.class);
      return warning != null && (warning.warning || !warning.codes().isEmpty()) ? warning : null;
    } catch (JsonSyntaxException | IllegalStateException e) {
      return null;
    }
  }
}
