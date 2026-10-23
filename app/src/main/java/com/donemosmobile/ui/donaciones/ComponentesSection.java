package com.donemosmobile.ui.donaciones;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.data.network.dto.TipoDonacionDto;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.components.StepperField;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

import java.util.List;

/** Paso 3: one stepper per component tipo, writing quantities into the form. */
final class ComponentesSection extends LinearLayout {

  private final LinearLayout container;
  private final ErrorText error;

  ComponentesSection(Context context) {
    super(context);
    setOrientation(VERTICAL);
    Ui.sectionTitle(this, context, "Componentes", AppSpacing.LG);

    TextView hint = new TextView(context);
    hint.setText("SANGRE máx. 1 · PLASMA máx. 1 · PLAQUETAS máx. 3");
    AppTypography.caption(hint);
    addView(hint);

    container = new LinearLayout(context);
    container.setOrientation(VERTICAL);
    addView(container);

    error = new ErrorText(context);
    addView(error);
  }

  ErrorText error() {
    return error;
  }

  /** Repaints the steppers for {@code tipos}; a change updates {@code form}. */
  void render(List<TipoDonacionDto> tipos, DonacionForm form) {
    container.removeAllViews();
    if (tipos == null) {
      return;
    }
    for (final TipoDonacionDto tipo : tipos) {
      if (tipo.id == null || !DonacionForm.isComponente(tipo.codigoUpper())) {
        continue;
      }
      StepperField stepper = new StepperField(getContext(), tipo.displayName(),
          form.cantidad(tipo.id), DonacionForm.maxFor(tipo.codigoUpper()));
      stepper.setOnChange(value -> {
        form.cantidades.put(tipo.id, value);
        error.hide();
      });
      container.addView(stepper);
    }
  }
}
