package com.donemosmobile.ui.donaciones;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.R;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/** Paso 6: ciclo calculado por el backend, con override manual opcional. */
final class CycleSection extends LinearLayout {

  private final AppInput ciclo;
  private final AppInput serie;
  private final AppInput donacion;
  private final ErrorText error;

  CycleSection(Context context) {
    super(context);
    setOrientation(VERTICAL);
    Ui.sectionTitle(this, context, "Ciclo de donaciones", AppSpacing.LG);

    TextView helper = new TextView(context);
    helper.setText(R.string.donacion_ciclo_ayuda);
    AppTypography.caption(helper);
    addView(helper);

    ciclo = Ui.addInput(this, context, "Ciclo", Ui.wholeNumber(), AppSpacing.SM);
    serie = Ui.addInput(this, context, "Serie", Ui.wholeNumber(), AppSpacing.SM);
    donacion = Ui.addInput(this, context, "Donación (orden dentro de la serie)",
        Ui.wholeNumber(), AppSpacing.SM);

    error = new ErrorText(context);
    addView(error);
  }

  ErrorText error() {
    return error;
  }

  void writeTo(DonacionForm form) {
    form.ciclo = ciclo.getText();
    form.serie = serie.getText();
    form.donacion = donacion.getText();
  }

  void clearInputs() {
    for (AppInput input : new AppInput[] {ciclo, serie, donacion}) {
      input.getEditText().setText("");
      input.setError(null);
    }
  }
}
