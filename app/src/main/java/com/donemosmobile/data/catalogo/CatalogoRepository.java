package com.donemosmobile.data.catalogo;

import android.content.Context;

import androidx.annotation.Nullable;

import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiCalls;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.CatalogoApi;
import com.donemosmobile.data.network.dto.MotivoDto;
import com.donemosmobile.data.network.dto.TipoDonacionDto;
import com.donemosmobile.ui.auth.SessionExpired;

import java.util.List;

/**
 * Catalogos de referencia: motivos de observacion/deferral
 * ({@code GET /api/motivos}) y tipos de donacion ({@code GET
 * /api/tipos-donacion}). Se cachean en memoria porque cambian poco.
 */
public class CatalogoRepository {

  private static final String MOTIVOS_KEY = "motivos";
  private static final String TIPOS_DONACION_KEY = "tipos_donacion";

  private final CatalogoApi api;
  private final ApiCalls calls;

  public CatalogoRepository(Context context) {
    Context appContext = context.getApplicationContext();
    this.api = ApiClient.get(context).catalogo();
    this.calls = new ApiCalls(() -> SessionExpired.handle(appContext));
  }

  /** Lista los motivos y deja el resultado cacheado. */
  public void listMotivos(final ApiCallback<List<MotivoDto>> callback) {
    calls.enqueue(api.motivos(), body -> {
      DataCache.getInstance().put(MOTIVOS_KEY, body);
      return body;
    }, callback);
  }

  /** Motivos cacheados de la sesion, o {@code null} si todavia no se pidieron. */
  @Nullable
  @SuppressWarnings("unchecked")
  public List<MotivoDto> getCachedMotivos() {
    Object value = DataCache.getInstance().get(MOTIVOS_KEY);
    return value instanceof List ? (List<MotivoDto>) value : null;
  }

  /** Lista los tipos de donacion y deja el resultado cacheado. */
  public void listTiposDonacion(final ApiCallback<List<TipoDonacionDto>> callback) {
    calls.enqueue(api.tiposDonacion(), body -> {
      DataCache.getInstance().put(TIPOS_DONACION_KEY, body);
      return body;
    }, callback);
  }

  /** Tipos de donacion cacheados, o {@code null} si todavia no se pidieron. */
  @Nullable
  @SuppressWarnings("unchecked")
  public List<TipoDonacionDto> getCachedTiposDonacion() {
    Object value = DataCache.getInstance().get(TIPOS_DONACION_KEY);
    return value instanceof List ? (List<TipoDonacionDto>) value : null;
  }
}
