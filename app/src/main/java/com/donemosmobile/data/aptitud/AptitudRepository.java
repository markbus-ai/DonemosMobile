package com.donemosmobile.data.aptitud;

import android.content.Context;

import com.donemosmobile.data.auth.SessionInvalidator;
import com.donemosmobile.data.cache.CacheKeys;
import com.donemosmobile.data.cache.DataCache;
import com.donemosmobile.data.network.ApiCallback;
import com.donemosmobile.data.network.ApiCalls;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.PacienteApi;
import com.donemosmobile.data.network.dto.PacienteDto;
import com.donemosmobile.data.network.dto.UpdateAptitudRequest;

/**
 * Actualiza la aptitud de un paciente ({@code PATCH
 * /api/pacientes/{dni}/aptitud}). Corre la red con {@code enqueue} y entrega el
 * resultado por callback, igual que el resto de los repositorios.
 */
public class AptitudRepository {

  private final PacienteApi api;
  private final ApiCalls calls;

  public AptitudRepository(Context context) {
    this.api = ApiClient.get(context).pacientes();
    this.calls = new ApiCalls(SessionInvalidator::invalidate);
  }

  /**
   * Aplica la nueva aptitud. La respuesta trae la relacion {@code aptitud}
   * embebida, asi que la pantalla puede refrescar el estado sin otro GET.
   */
  public void update(String dni, UpdateAptitudRequest body, final ApiCallback<PacienteDto> callback) {
    calls.enqueueWrite(api.updateAptitud(dni, body), paciente -> {
      // Deja la ficha actualizada para que el lookup por DNI la muestre.
      DataCache.getInstance().put(CacheKeys.paciente(dni), paciente);
      return paciente;
    }, callback);
  }
}
