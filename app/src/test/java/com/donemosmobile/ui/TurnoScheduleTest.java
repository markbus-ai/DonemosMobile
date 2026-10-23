package com.donemosmobile.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JVM tests for the turno booking window and next-slot hint. */
public class TurnoScheduleTest {

  @Test
  public void nextSlotRoundsUpToTheNextQuarter() {
    assertEquals("14:15", TurnoSchedule.nextSlot("14:07"));
    assertEquals("14:30", TurnoSchedule.nextSlot("14:15"));
    assertEquals("09:00", TurnoSchedule.nextSlot("08:59"));
  }

  @Test
  public void nextSlotIsNullWhenTheDayIsOverOrInputIsBad() {
    assertNull(TurnoSchedule.nextSlot("23:45"));
    assertNull(TurnoSchedule.nextSlot("23:59"));
    assertNull(TurnoSchedule.nextSlot("25:00"));
    assertNull(TurnoSchedule.nextSlot(null));
  }

  @Test
  public void maxFechaIsOneYearAhead() {
    assertEquals("2027-10-22", TurnoSchedule.maxFecha("2026-10-22"));
    assertEquals("2029-02-28", TurnoSchedule.maxFecha("2028-02-29"));
    assertNull(TurnoSchedule.maxFecha("not-a-date"));
  }

  @Test
  public void fechaBeyondOneYearIsRejected() {
    assertFalse(TurnoSchedule.isBeyondMax("2027-10-22", "2026-10-22"));
    assertTrue(TurnoSchedule.isBeyondMax("2027-10-23", "2026-10-22"));
    assertFalse(TurnoSchedule.isBeyondMax("2026-10-22", "2026-10-22"));
  }

  @Test
  public void displayFormatsApiDates() {
    assertEquals("22/10/2027", TurnoSchedule.display("2027-10-22"));
    assertEquals("x", TurnoSchedule.display("x"));
  }

  @Test
  public void todayFollowsTheInjectedClock() {
    java.util.Calendar noon = java.util.Calendar.getInstance();
    noon.clear();
    noon.set(2026, java.util.Calendar.OCTOBER, 23, 12, 0);
    TurnoSchedule.setClock(noon::getTimeInMillis);
    try {
      assertEquals("2026-10-23", TurnoSchedule.today());
    } finally {
      TurnoSchedule.setClock(null);
    }
  }
}
