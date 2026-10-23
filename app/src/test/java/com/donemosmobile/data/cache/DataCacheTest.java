package com.donemosmobile.data.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/** JVM tests for cache freshness (injected clock), typed reads and keys. */
public class DataCacheTest {

  private final AtomicLong now = new AtomicLong(1_000_000L);
  private final DataCache cache = new DataCache(DataCache.DEFAULT_STALE_MS, now::get);

  @Test
  public void entryIsFreshUntilTheThreshold() {
    cache.put("k", "v");
    now.addAndGet(DataCache.DEFAULT_STALE_MS - 1);
    assertTrue(cache.isFresh("k"));
    now.addAndGet(1);
    assertFalse(cache.isFresh("k"));
  }

  @Test
  public void staleEntryIsStillReadable() {
    cache.put("k", "v");
    now.addAndGet(DataCache.DEFAULT_STALE_MS * 10);
    assertEquals("v", cache.get("k", String.class));
  }

  @Test
  public void putRefreshesTheTimestamp() {
    cache.put("k", "v1");
    now.addAndGet(DataCache.DEFAULT_STALE_MS);
    cache.put("k", "v2");
    assertTrue(cache.isFresh("k"));
  }

  @Test
  public void missingKeyIsNeitherFreshNorReadable() {
    assertFalse(cache.isFresh("missing"));
    assertNull(cache.get("missing", String.class));
  }

  @Test
  public void typedReadRejectsAnotherType() {
    cache.put("k", 42);
    assertNull(cache.get("k", String.class));
    assertEquals(Integer.valueOf(42), cache.get("k", Integer.class));
  }

  @Test
  public void typedListReadChecksEveryElement() {
    cache.put("ok", Arrays.asList("a", "b"));
    cache.put("mixed", Arrays.asList("a", 1));
    cache.put("notAList", "a");
    assertEquals(List.of("a", "b"), cache.getList("ok", String.class));
    assertNull(cache.getList("mixed", String.class));
    assertNull(cache.getList("notAList", String.class));
  }

  @Test
  public void putNullRemovesAndInvalidateDrops() {
    cache.put("a", "v");
    cache.put("a", null);
    assertNull(cache.get("a", String.class));
    cache.put("b", "v");
    cache.invalidate("b");
    assertNull(cache.get("b", String.class));
    cache.put("c", "v");
    cache.clear();
    assertNull(cache.get("c", String.class));
  }

  @Test
  public void keysKeepTheirFormat() {
    assertEquals("paciente:30111222", CacheKeys.paciente("30111222"));
    assertEquals("turnos:2026-10-23:all", CacheKeys.turnos("2026-10-23", null));
    assertEquals("turnos:all:7", CacheKeys.turnos(null, 7L));
    assertEquals("turnos:desde:2026-10-23", CacheKeys.turnosDesde("2026-10-23"));
  }
}
