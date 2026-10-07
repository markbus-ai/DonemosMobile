package com.donemosmobile.ui.components;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Campo de formulario con etiqueta fija arriba, input en el medio y error
 * abajo. La etiqueta es un TextView aparte, así el texto que se escribe nunca
 * la tapa. No depende del tema Material para posicionar la etiqueta: el theme
 * de la app es AppCompat, y por eso el hint flotante de TextInputLayout no
 * funcionaba y el texto se montaba encima del label.
 */
public class AppInput extends LinearLayout {

  private final TextView labelView;
  private final TextInputEditText editText;
  private final TextView errorView;

  public AppInput(Context context, String hint) {
    super(context);
    setOrientation(VERTICAL);

    labelView = new TextView(context);
    labelView.setText(hint);
    AppTypography.caption(labelView);
    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    labelParams.bottomMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    addView(labelView, labelParams);

    editText = new TextInputEditText(context);
    editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    editText.setTextColor(AppColors.TEXT_PRIMARY);
    editText.setHintTextColor(AppColors.TEXT_SECONDARY);
    editText.setSingleLine(true);
    // Gris en reposo, acento al enfocar: mismo lenguaje que el resto de la app.
    editText.setBackgroundTintList(new ColorStateList(
        new int[][]{new int[]{android.R.attr.state_focused}, new int[]{}},
        new int[]{AppColors.PRIMARY, AppColors.DIVIDER}));
    addView(editText, new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    errorView = new TextView(context);
    errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    errorView.setTextColor(AppColors.ERROR);
    errorView.setVisibility(GONE);
    LinearLayout.LayoutParams errorParams = new LinearLayout.LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    errorParams.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    addView(errorView, errorParams);
  }

  public TextInputEditText getEditText() {
    return editText;
  }

  public String getText() {
    return editText.getText() != null ? editText.getText().toString().trim() : "";
  }

  /** Muestra el error debajo del campo. {@code null} o vacío lo oculta. */
  public void setError(CharSequence error) {
    if (error == null || error.length() == 0) {
      errorView.setText("");
      errorView.setVisibility(GONE);
      return;
    }
    errorView.setText(error);
    errorView.setVisibility(VISIBLE);
  }

  /** Propaga el enable al input: la etiqueta sola no alcanza. */
  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    editText.setEnabled(enabled);
    labelView.setEnabled(enabled);
  }
}
