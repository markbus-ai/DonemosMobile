package com.donemosmobile.data.cache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

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
 * Las claves salen de {@link CacheKeys}.
 */
public final class DataCache {

  /** Un resultado cacheado se considera viejo despues de 2 minutos. */
  public static final long DEFAULT_STALE_MS = 2 * 60 * 1000L;

  private static volatile DataCache instance;

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();
  private final long staleMs;
  private final LongSupplier clock;

  /** @param clock current time in milliseconds; injectable for tests. */
  DataCache(long staleMs, @NonNull LongSupplier clock) {
    this.staleMs = staleMs;
    this.clock = clock;
  }

  public static DataCache getInstance() {
    DataCache local = instance;
    if (local == null) {
      synchronized (DataCache.class) {
        local = instance;
        if (local == null) {
          local = new DataCache(DEFAULT_STALE_MS, System::currentTimeMillis);
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
    entries.put(key, new Entry(value, clock.getAsLong()));
  }

  /** Valor cacheado de ese tipo, o {@code null} si no hay o es de otro tipo. */
  @Nullable
  public <T> T get(String key, @NonNull Class<T> type) {
    Entry entry = entries.get(key);
    return entry != null && type.isInstance(entry.value) ? type.cast(entry.value) : null;
  }

  /**
   * Lista cacheada cuyos elementos son todos de {@code elementType}, como copia
   * nueva; {@code null} si no hay lista o algun elemento es de otro tipo.
   */
  @Nullable
  public <E> List<E> getList(String key, @NonNull Class<E> elementType) {
    Entry entry = entries.get(key);
    if (entry == null || !(entry.value instanceof List)) {
      return null;
    }
    List<?> raw = (List<?>) entry.value;
    List<E> typed = new ArrayList<>(raw.size());
    for (Object item : raw) {
      if (item != null && !elementType.isInstance(item)) {
        return null;
      }
      typed.add(elementType.cast(item));
    }
    return typed;
  }

  /** {@code true} si hay valor y todavia no paso el umbral de vencimiento. */
  public boolean isFresh(String key) {
    Entry entry = entries.get(key);
    return entry != null && (clock.getAsLong() - entry.timestamp) < staleMs;
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
