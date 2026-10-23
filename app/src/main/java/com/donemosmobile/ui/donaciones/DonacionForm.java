package com.donemosmobile.ui.donaciones;

import androidx.annotation.Nullable;

import com.donemosmobile.ui.NumberParsing;
import com.donemosmobile.ui.NumericBounds;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * State of the donation form as plain Java: the selection (patient, tipo,
 * component quantities, bag options) plus the raw text of every input, and the
 * client-side validation that runs before any request is sent.
 */
public final class DonacionForm {

  /** Maximo de unidades por codigo de componente (ComponentRules del backend). */
  private static final Map<String, Integer> MAXIMOS;
  static {
    Map<String, Integer> maximos = new LinkedHashMap<>();
    maximos.put("SANGRE", 1);
    maximos.put("PLASMA", 1);
    maximos.put("PLAQUETAS", 3);
    MAXIMOS = Collections.unmodifiableMap(maximos);
  }

  public static final String DEFAULT_TIPO_BOLSA = "doble";
  public static final String DEFAULT_BRAZO = "izquierdo";

  /** Clinical inputs that carry their own inline error. */
  public enum ClinicalField {
    HEMOGLOBINA, HEMATOCRITO, PLAQUETAS, SISTOLICA, DIASTOLICA, FRECUENCIA, PESO
  }

  @Nullable public Long pacienteId;
  @Nullable public Long tipoId;
  public boolean tipoAferesis;
  /** Units per component tipo id, in the order they were first set. */
  public final Map<Long, Integer> cantidades = new LinkedHashMap<>();

  public String tipoBolsa = DEFAULT_TIPO_BOLSA;
  public String brazo = DEFAULT_BRAZO;
  public String anticoagulante = "";
  public String lote = "";
  public String tubuladura = "";
  public String dificultad = "";
  public boolean dobleEtiqueta;

  /** Raw text of each clinical input. */
  public final Map<ClinicalField, String> clinical = new EnumMap<>(ClinicalField.class);

  public String ciclo = "";
  public String serie = "";
  public String donacion = "";

  /** True when {@code codigo} is a component the form counts units for. */
  public static boolean isComponente(@Nullable String codigo) {
    return codigo != null && MAXIMOS.containsKey(codigo);
  }

  /** Maximum units for a component code (1 when unknown). */
  public static int maxFor(@Nullable String codigo) {
    Integer max = codigo != null ? MAXIMOS.get(codigo) : null;
    return max != null ? max : 1;
  }

  public int cantidad(@Nullable Long tipoId) {
    Integer value = tipoId != null ? cantidades.get(tipoId) : null;
    return value != null ? value : 0;
  }

  public int totalComponentes() {
    int total = 0;
    for (Integer value : cantidades.values()) {
      if (value != null) {
        total += value;
      }
    }
    return total;
  }

  public String clinicalText(ClinicalField field) {
    String value = clinical.get(field);
    return value != null ? value : "";
  }

  /** Every check of the form, in the order the screen shows them. */
  public Errors validate() {
    Errors errors = new Errors();
    if (pacienteId == null) {
      errors.missingPaciente = true;
      return errors;
    }
    if (tipoId == null) {
      errors.tipo = "Elegí el tipo de donación";
    }
    if (totalComponentes() < 1) {
      errors.componentes = "Agregá al menos un componente";
    }

    boolean clinicalOk = checkBounds(errors, ClinicalField.HEMOGLOBINA, NumericBounds.HEMOGLOBINA);
    clinicalOk &= checkBounds(errors, ClinicalField.HEMATOCRITO, NumericBounds.HEMATOCRITO);
    clinicalOk &= checkBounds(errors, ClinicalField.PLAQUETAS, NumericBounds.PLAQUETAS);
    clinicalOk &= checkBounds(errors, ClinicalField.SISTOLICA, NumericBounds.SISTOLICA);
    clinicalOk &= checkBounds(errors, ClinicalField.DIASTOLICA, NumericBounds.DIASTOLICA);
    clinicalOk &= checkBounds(errors, ClinicalField.FRECUENCIA, NumericBounds.FRECUENCIA);
    clinicalOk &= checkBounds(errors, ClinicalField.PESO, NumericBounds.PESO);
    if (!clinicalOk) {
      errors.clinical = "Revisá los valores clínicos marcados";
    }

    if (tipoAferesis
        && (NumberParsing.parseDouble(clinicalText(ClinicalField.PLAQUETAS)) == null
        || NumberParsing.parseDouble(clinicalText(ClinicalField.HEMATOCRITO)) == null)) {
      errors.clinical = "Aféresis: cargá plaquetas y hematocrito";
    }

    String presionError = NumericBounds.checkPresion(
        NumberParsing.parseInt(clinicalText(ClinicalField.SISTOLICA)),
        NumberParsing.parseInt(clinicalText(ClinicalField.DIASTOLICA)));
    if (presionError != null) {
      errors.fields.put(ClinicalField.DIASTOLICA, presionError);
      errors.clinical = presionError;
    }

    errors.cycle = NumericBounds.checkCycle(ciclo, serie, donacion);

    if (!errors.ok() && errors.clinical == null && errors.componentes == null
        && errors.cycle == null && errors.tipo == null) {
      errors.form = "Revisá los datos antes de confirmar";
    }
    return errors;
  }

  private boolean checkBounds(Errors errors, ClinicalField field, NumericBounds.Range range) {
    String error = NumericBounds.check(range, clinicalText(field));
    errors.fields.put(field, error);
    return error == null;
  }

  /** Outcome of {@link #validate()}; {@code null} messages mean "no error". */
  public static final class Errors {
    public boolean missingPaciente;
    @Nullable public String tipo;
    @Nullable public String componentes;
    @Nullable public String clinical;
    @Nullable public String cycle;
    @Nullable public String form;
    /** Inline error per clinical input; a {@code null} value clears it. */
    public final Map<ClinicalField, String> fields = new EnumMap<>(ClinicalField.class);

    public boolean ok() {
      if (missingPaciente || tipo != null || componentes != null || clinical != null
          || cycle != null) {
        return false;
      }
      for (String error : fields.values()) {
        if (error != null) {
          return false;
        }
      }
      return true;
    }
  }
}
