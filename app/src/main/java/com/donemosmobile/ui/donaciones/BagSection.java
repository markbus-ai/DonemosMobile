package com.donemosmobile.ui.donaciones;

import android.content.Context;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.components.ErrorText;
import com.donemosmobile.ui.components.SelectGroup;
import com.donemosmobile.ui.components.SelectableRow;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;

/** Paso 5: datos de la bolsa (tipo, brazo, insumos y doble etiqueta). */
final class BagSection extends LinearLayout {

  private static final String[] TIPOS_BOLSA = {"doble", "triple", "cuadruple", "quintuple"};
  private static final String[] BRAZOS = {"izquierdo", "derecho"};

  private final SelectGroup bolsaGroup;
  private final SelectGroup brazoGroup;
  private final AppInput anticoagulante;
  private final AppInput lote;
  private final AppInput tubuladura;
  private final AppInput dificultad;
  private final CheckBox dobleEtiqueta;
  private final ErrorText error;

  private String selectedTipoBolsa = DonacionForm.DEFAULT_TIPO_BOLSA;
  private String selectedBrazo = DonacionForm.DEFAULT_BRAZO;

  BagSection(Context context) {
    super(context);
    setOrientation(VERTICAL);
    Ui.sectionTitle(this, context, "Datos de la bolsa", AppSpacing.LG);

    bolsaGroup = new SelectGroup(context, "Tipo de bolsa", 0);
    addView(bolsaGroup);

    brazoGroup = new SelectGroup(context, "Brazo", AppSpacing.SM);
    addView(brazoGroup);

    anticoagulante = Ui.addInput(this, context, "Anticoagulante", InputType.TYPE_CLASS_TEXT,
        AppSpacing.SM);
    lote = Ui.addInput(this, context, "Lote", InputType.TYPE_CLASS_TEXT, AppSpacing.SM);
    tubuladura = Ui.addInput(this, context, "Tubuladura", InputType.TYPE_CLASS_TEXT,
        AppSpacing.SM);
    dificultad = Ui.addInput(this, context, "Dificultad", InputType.TYPE_CLASS_TEXT,
        AppSpacing.SM);

    dobleEtiqueta = new CheckBox(context);
    dobleEtiqueta.setText("Doble etiqueta");
    dobleEtiqueta.setTextColor(AppColors.TEXT_PRIMARY);
    dobleEtiqueta.setTextSize(14);
    addView(dobleEtiqueta, Ui.matchWrap(context, AppSpacing.SM));

    error = new ErrorText(context);
    addView(error);

    renderBolsa();
    renderBrazo();
  }

  ErrorText error() {
    return error;
  }

  void writeTo(DonacionForm form) {
    form.tipoBolsa = selectedTipoBolsa;
    form.brazo = selectedBrazo;
    form.anticoagulante = anticoagulante.getText();
    form.lote = lote.getText();
    form.tubuladura = tubuladura.getText();
    form.dificultad = dificultad.getText();
    form.dobleEtiqueta = dobleEtiqueta.isChecked();
  }

  /** Back to the default selections; the free-text supplies are left as they are. */
  void reset() {
    selectedTipoBolsa = DonacionForm.DEFAULT_TIPO_BOLSA;
    selectedBrazo = DonacionForm.DEFAULT_BRAZO;
    dobleEtiqueta.setChecked(false);
    renderBolsa();
    renderBrazo();
  }

  private void renderBolsa() {
    bolsaGroup.options().removeAllViews();
    for (final String value : TIPOS_BOLSA) {
      bolsaGroup.options().addView(new SelectableRow(getContext(), value,
          value.equals(selectedTipoBolsa), v -> {
            selectedTipoBolsa = value;
            error.hide();
            renderBolsa();
          }));
    }
  }

  private void renderBrazo() {
    brazoGroup.options().removeAllViews();
    for (final String value : BRAZOS) {
      brazoGroup.options().addView(new SelectableRow(getContext(), value,
          value.equals(selectedBrazo), v -> {
            selectedBrazo = value;
            error.hide();
            renderBrazo();
          }));
    }
  }
}
