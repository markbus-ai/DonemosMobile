package com.donemosmobile.ui.donaciones;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    helper.setText("La posición en el ciclo (ciclo > serie > donación) se calcula "
        + "automáticamente al registrar la donación: 5 donaciones por serie y 3 series "
        + "por ciclo. Dejá los campos vacíos para que la calcule el sistema; completalos "
        + "solo si necesitás fijar la posición manualmente.");
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
