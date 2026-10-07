package com.donemosmobile.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.components.AppInput;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Helpers de layout compartidos: dp, formas redondeadas, parametros y parseo.
 * Vive aca para que las pantallas no repitan el mismo boilerplate.
 */
public final class Ui {

  private Ui() {}

  public static int dp(Context context, int value) {
    return AppSpacing.dp(value, context.getResources());
  }

  public static LinearLayout.LayoutParams matchWrap(Context context) {
    return new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  public static LinearLayout.LayoutParams matchWrap(Context context, int topMarginDp) {
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = dp(context, topMarginDp);
    return params;
  }

  public static GradientDrawable rounded(Context context, int color, float radiusDp) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(AppSpacing.dp((int) radiusDp, context.getResources()));
    return drawable;
  }

  /** Valor con fallback a guion para no mostrar celdas vacias. */
  public static String safe(String value) {
    return value != null && !value.isEmpty() ? value : "—";
  }

  /** Titulo de seccion (headline2) con margen superior configurable. */
  public static TextView sectionTitle(LinearLayout parent, Context context, String text,
      int topMarginDp) {
    TextView title = new TextView(context);
    title.setText(text);
    AppTypography.headline2(title);
    parent.addView(title, matchWrap(context, topMarginDp));
    return title;
  }

  /** Etiqueta caption con margen superior configurable. */
  public static TextView captionLabel(LinearLayout parent, Context context, String text,
      int topMarginDp) {
    TextView label = new TextView(context);
    label.setText(text);
    AppTypography.caption(label);
    parent.addView(label, matchWrap(context, topMarginDp));
    return label;
  }

  /** Agrega un {@link AppInput} con tipo de teclado y margen superior. */
  public static AppInput addInput(LinearLayout parent, Context context, String hint,
      int inputType, int topMarginDp) {
    AppInput input = new AppInput(context, hint);
    input.getEditText().setInputType(inputType);
    parent.addView(input, matchWrap(context, topMarginDp));
    return input;
  }

  public static int decimalNumber() {
    return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
  }

  public static int wholeNumber() {
    return InputType.TYPE_CLASS_NUMBER;
  }

  public static String blankToNull(String value) {
    return value != null && !value.trim().isEmpty() ? value.trim() : null;
  }

  public static Double parseDoubleOrNull(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return Double.valueOf(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static Integer parseIntOrNull(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return (int) Double.parseDouble(value.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
