package com.donemosmobile.data.cache;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache en memoria de alcance aplicacion para datos de red.
 *
 * <p>Guarda el ultimo resultado exitoso por clave junto a su timestamp y una
 * nocion de "vencido" ({@link #DEFAULT_STALE_MS}). Habilita el patron
 * stale-while-revalidate: la pantalla pinta lo cacheado al toque y refresca
 * en segundo plano.
 *
 * <p>Vive solo en memoria (sin disco): si muere el proceso se pierde. Se vacia
 * al cerrar sesion con {@link #clear()} para no filtrar datos entre usuarios.
 */
public final class DataCache {

  /** Un resultado cacheado se considera viejo despues de 2 minutos. */
  public static final long DEFAULT_STALE_MS = 2 * 60 * 1000L;

  private static volatile DataCache instance;

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();
  private final long staleMs;

  private DataCache(long staleMs) {
    this.staleMs = staleMs;
  }

  public static DataCache getInstance() {
    DataCache local = instance;
    if (local == null) {
      synchronized (DataCache.class) {
        local = instance;
        if (local == null) {
          local = new DataCache(DEFAULT_STALE_MS);
          instance = local;
        }
      }
    }
    return local;
  }

  /** Guarda (o reemplaza) el valor de una clave y refresca su timestamp. */
  public void put(String key, @Nullable Object value) {
    if (key == null) {
      return;
    }
    if (value == null) {
      entries.remove(key);
      return;
    }
    entries.put(key, new Entry(value, System.currentTimeMillis()));
  }

  /** Devuelve el valor cacheado o {@code null} si no hay nada para esa clave. */
  @Nullable
  public Object get(String key) {
    Entry entry = entries.get(key);
    return entry != null ? entry.value : null;
  }

  /** {@code true} si hay valor y todavia no paso el umbral de vencimiento. */
  public boolean isFresh(String key) {
    Entry entry = entries.get(key);
    return entry != null && (System.currentTimeMillis() - entry.timestamp) < staleMs;
  }

  /** Borra una clave puntual. */
  public void invalidate(String key) {
    entries.remove(key);
  }

  /** Borra todo el cache. Usar al cerrar sesion. */
  public void clear() {
    entries.clear();
  }

  private static final class Entry {
    final Object value;
    final long timestamp;

    Entry(Object value, long timestamp) {
      this.value = value;
      this.timestamp = timestamp;
    }
  }
}
