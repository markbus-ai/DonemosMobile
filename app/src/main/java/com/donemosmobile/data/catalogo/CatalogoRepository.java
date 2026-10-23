package com.donemosmobile.data.catalogo;

import android.content.Context;

import androidx.annotation.Nullable;

import com.donemosmobile.data.auth.SessionInvalidator;
import com.donemosmobile.data.cache.CacheKeys;
import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiCalls;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.CatalogoApi;
import com.donemosmobile.data.network.dto.MotivoDto;
import com.donemosmobile.data.network.dto.TipoDonacionDto;

import java.util.List;

/**
 * Catalogos de referencia: motivos de observacion/deferral
 * ({@code GET /api/motivos}) y tipos de donacion ({@code GET
 * /api/tipos-donacion}). Se cachean en memoria porque cambian poco.
 */
public class CatalogoRepository {

  private final CatalogoApi api;
  private final ApiCalls calls;

  public CatalogoRepository(Context context) {
    this.api = ApiClient.get(context).catalogo();
    this.calls = new ApiCalls(SessionInvalidator::invalidate);
  }

  /** Lista los motivos y deja el resultado cacheado. */
  public void listMotivos(final ApiCallback<List<MotivoDto>> callback) {
    calls.enqueue(api.motivos(), body -> {
      DataCache.getInstance().put(CacheKeys.MOTIVOS, body);
      return body;
    }, callback);
  }

  /** Motivos cacheados de la sesion, o {@code null} si todavia no se pidieron. */
  @Nullable
  public List<MotivoDto> getCachedMotivos() {
    return DataCache.getInstance().getList(CacheKeys.MOTIVOS, MotivoDto.class);
  }

  /** Lista los tipos de donacion y deja el resultado cacheado. */
  public void listTiposDonacion(final ApiCallback<List<TipoDonacionDto>> callback) {
    calls.enqueue(api.tiposDonacion(), body -> {
      DataCache.getInstance().put(CacheKeys.TIPOS_DONACION, body);
      return body;
    }, callback);
  }

  /** Tipos de donacion cacheados, o {@code null} si todavia no se pidieron. */
  @Nullable
  public List<TipoDonacionDto> getCachedTiposDonacion() {
    return DataCache.getInstance().getList(CacheKeys.TIPOS_DONACION, TipoDonacionDto.class);
  }
}
