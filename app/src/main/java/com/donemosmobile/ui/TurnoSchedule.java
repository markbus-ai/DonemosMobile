package com.donemosmobile.ui;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pure date/time rules for booking a turno, mirrored from the API: the fecha
 * must be between today and today + 1 year, and a time already past today is
 * rejected. Strings only ({@code yyyy-MM-dd}, {@code HH:mm}) so it runs on the JVM.
 */
public final class TurnoSchedule {

  /** Granularity of the suggested next slot, in minutes. */
  public static final int SLOT_MINUTES = 15;

  private TurnoSchedule() {}

  /**
   * The first slot strictly after {@code nowHHmm}, rounded up to
   * {@link #SLOT_MINUTES}, or {@code null} when no slot is left today.
   */
  @Nullable
  public static String nextSlot(@Nullable String nowHHmm) {
    int minutes = toMinutes(nowHHmm);
    if (minutes < 0) {
      return null;
    }
    int next = (minutes / SLOT_MINUTES + 1) * SLOT_MINUTES;
    if (next >= 24 * 60) {
      return null;
    }
    return String.format(Locale.US, "%02d:%02d", next / 60, next % 60);
  }

  /** Last bookable day ({@code yyyy-MM-dd}): one calendar year after {@code today}. */
  @Nullable
  public static String maxFecha(@Nullable String today) {
    Date parsed = parse(today);
    if (parsed == null) {
      return null;
    }
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    cal.setTime(parsed);
    cal.add(Calendar.YEAR, 1);
    return format(cal.getTime());
  }

  /** True when {@code fecha} is later than {@link #maxFecha(String)} of {@code today}. */
  public static boolean isBeyondMax(@Nullable String fecha, @Nullable String today) {
    String max = maxFecha(today);
    return fecha != null && max != null && fecha.compareTo(max) > 0;
  }

  /** {@code yyyy-MM-dd} to {@code dd/MM/yyyy}; returns the input when it does not parse. */
  public static String display(@Nullable String apiDate) {
    Date parsed = parse(apiDate);
    if (parsed == null) {
      return apiDate != null ? apiDate : "";
    }
    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    return fmt.format(parsed);
  }

  private static int toMinutes(@Nullable String hhmm) {
    if (hhmm == null || !hhmm.matches("\\d{1,2}:\\d{2}")) {
      return -1;
    }
    String[] parts = hhmm.split(":");
    int hour = Integer.parseInt(parts[0]);
    int minute = Integer.parseInt(parts[1]);
    if (hour > 23 || minute > 59) {
      return -1;
    }
    return hour * 60 + minute;
  }

  @Nullable
  private static Date parse(@Nullable String apiDate) {
    if (apiDate == null) {
      return null;
    }
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    fmt.setLenient(false);
    try {
      return fmt.parse(apiDate);
    } catch (ParseException e) {
      return null;
    }
  }

  private static String format(Date date) {
    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    return fmt.format(date);
  }
}
