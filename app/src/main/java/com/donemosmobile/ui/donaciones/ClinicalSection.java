package com.donemosmobile.ui.donaciones;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.donaciones.DonacionForm.ClinicalField;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;

import java.util.EnumMap;
import java.util.Map;

/** Paso 4: signos clinicos, with the aferesis hint and inline bounds errors. */
final class ClinicalSection extends LinearLayout {

  private final LinearLayout aferesisHint;
  private final Map<ClinicalField, AppInput> inputs = new EnumMap<>(ClinicalField.class);
  private final ErrorText error;

  ClinicalSection(Context context) {
    super(context);
    setOrientation(VERTICAL);
    Ui.sectionTitle(this, context, "Signos clínicos", AppSpacing.LG);

    aferesisHint = new LinearLayout(context);
    aferesisHint.setOrientation(VERTICAL);
    TextView hint = new TextView(context);
    hint.setText("Donación por aféresis: plaquetas y hematocrito son obligatorios.");
    hint.setTextSize(13);
    hint.setTextColor(AppColors.WARNING_TEXT);
    aferesisHint.addView(hint);
    aferesisHint.setVisibility(View.GONE);
    addView(aferesisHint);

    add(ClinicalField.HEMOGLOBINA, "Hemoglobina (g/dL)", Ui.decimalNumber());
    add(ClinicalField.HEMATOCRITO, "Hematocrito (%)", Ui.decimalNumber());
    add(ClinicalField.PLAQUETAS, "Plaquetas (/mm³)", Ui.wholeNumber());
    add(ClinicalField.SISTOLICA, "Presión sistólica", Ui.wholeNumber());
    add(ClinicalField.DIASTOLICA, "Presión diastólica", Ui.wholeNumber());
    add(ClinicalField.FRECUENCIA, "Frecuencia cardíaca", Ui.wholeNumber());
    add(ClinicalField.PESO, "Peso del donante (kg)", Ui.decimalNumber());

    error = new ErrorText(context);
    addView(error);
  }

  private void add(ClinicalField field, String hint, int inputType) {
    inputs.put(field, Ui.addInput(this, getContext(), hint, inputType, AppSpacing.SM));
  }

  ErrorText error() {
    return error;
  }

  void setAferesis(boolean aferesis) {
    aferesisHint.setVisibility(aferesis ? View.VISIBLE : View.GONE);
  }

  void writeTo(DonacionForm form) {
    for (Map.Entry<ClinicalField, AppInput> entry : inputs.entrySet()) {
      form.clinical.put(entry.getKey(), entry.getValue().getText());
    }
  }

  /** Inline errors per input; a {@code null} message clears that input. */
  void showFieldErrors(Map<ClinicalField, String> errors) {
    for (Map.Entry<ClinicalField, String> entry : errors.entrySet()) {
      inputs.get(entry.getKey()).setError(entry.getValue());
    }
  }

  void clearFieldErrors() {
    for (AppInput input : inputs.values()) {
      input.setError(null);
    }
  }

  void clearInputs() {
    for (AppInput input : inputs.values()) {
      input.getEditText().setText("");
      input.setError(null);
    }
  }
}
