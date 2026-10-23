package com.donemosmobile.ui;

import java.math.BigDecimal;

/**
 * Plausibility bounds shared with the API (it rejects the same values with
 * 422). They reject impossible values; they are not clinical gating.
 */
public final class NumericBounds {

  /** A named inclusive range with its Spanish label and unit. */
  public static final class Range {
    public final String label;
    public final BigDecimal min;
    public final BigDecimal max;
    public final String unit;
    public final boolean integer;
    /** When true the value must be strictly greater than {@link #min}. */
    public final boolean minExclusive;

    Range(String label, String min, String max, String unit, boolean integer) {
      this(label, min, max, unit, integer, false);
    }

    Range(String label, String min, String max, String unit, boolean integer,
        boolean minExclusive) {
      this.minExclusive = minExclusive;
      this.label = label;
      this.min = new BigDecimal(min);
      this.max = max != null ? new BigDecimal(max) : null;
      this.unit = unit;
      this.integer = integer;
    }
  }

  // Physical-possibility bounds only: capture-only signs are never clinically gated.
  public static final Range HEMOGLOBINA = new Range("Hemoglobina", "0", "30", "g/dL", false, true);
  public static final Range HEMATOCRITO = new Range("Hematocrito", "0", "100", "%", false, true);
  public static final Range PLAQUETAS = new Range("Plaquetas", "1", "5000000", "", true);
  public static final Range SISTOLICA = new Range("Presión sistólica", "1", "350", "mmHg", true);
  public static final Range DIASTOLICA = new Range("Presión diastólica", "1", "250", "mmHg", true);
  public static final Range FRECUENCIA = new Range("Frecuencia cardíaca", "1", "350", "lpm", true);
  public static final Range PESO = new Range("Peso", "0", "500", "kg", false, true);
  public static final Range CICLO = new Range("Ciclo", "1", null, "", true);
  public static final Range SERIE = new Range("Serie", "1", "3", "", true);
  public static final Range DONACION = new Range("Donación", "1", "5", "", true);

  private NumericBounds() {}

  /**
   * Validates optional text against a range. Blank is valid (field not
   * loaded). Returns the Spanish error to show on the field, or null if valid.
   */
  public static String check(Range range, String text) {
    if (NumberParsing.isBlank(text)) {
      return null;
    }
    BigDecimal value = NumberParsing.parseDecimal(text);
    if (value == null) {
      return range.label + ": ingresá un número válido";
    }
    if (range.integer && value.stripTrailingZeros().scale() > 0) {
      return range.label + " debe ser un número entero";
    }
    int vsMin = value.compareTo(range.min);
    boolean belowMin = range.minExclusive ? vsMin <= 0 : vsMin < 0;
    boolean aboveMax = range.max != null && value.compareTo(range.max) > 0;
    if (belowMin || aboveMax) {
      if (range.max == null) {
        return range.label + " debe ser mayor o igual a " + range.min.toPlainString();
      }
      String unit = range.unit.isEmpty() ? "" : " " + range.unit;
      if (range.minExclusive) {
        return range.label + " debe ser mayor a " + range.min.toPlainString() + " y hasta "
            + range.max.toPlainString() + unit;
      }
      return range.label + " debe estar entre " + range.min.toPlainString() + " y "
          + range.max.toPlainString() + unit;
    }
    return null;
  }

  /**
   * Validates the manual cycle counters (ciclo &gt;= 1, serie 1-3, donacion
   * 1-5). All three must be filled or all three blank (a blank set resets the
   * cycle); a partial override is rejected like the API does. First error or null.
   */
  public static String checkCycle(String ciclo, String serie, String donacion) {
    int filled = (NumberParsing.isBlank(ciclo) ? 0 : 1) + (NumberParsing.isBlank(serie) ? 0 : 1)
        + (NumberParsing.isBlank(donacion) ? 0 : 1);
    if (filled != 0 && filled != 3) {
      return "Completá ciclo, serie y donación, o dejá los tres vacíos";
    }
    String error = check(CICLO, ciclo);
    if (error == null) {
      error = check(SERIE, serie);
    }
    if (error == null) {
      error = check(DONACION, donacion);
    }
    return error;
  }

  private static final java.util.regex.Pattern DNI =
      java.util.regex.Pattern.compile("[1-9][0-9]{6,7}");
  private static final java.util.regex.Pattern TELEFONO =
      java.util.regex.Pattern.compile("\\+?[0-9 ()-]{6,20}");

  /** Same DNI rule as the API: 7 or 8 digits, not starting with 0. */
  public static boolean isValidDni(String dni) {
    return dni != null && DNI.matcher(dni.trim()).matches();
  }

  /** Same phone rule as the API: optional +, digits, spaces, parentheses, dashes. */
  public static boolean isValidTelefono(String telefono) {
    return telefono != null && TELEFONO.matcher(telefono.trim()).matches();
  }

  /** Diastolic must be strictly lower than systolic when both are present. */
  public static String checkPresion(Integer sistolica, Integer diastolica) {
    if (sistolica != null && diastolica != null && diastolica >= sistolica) {
      return "La presión diastólica debe ser menor que la sistólica";
    }
    return null;
  }
}
