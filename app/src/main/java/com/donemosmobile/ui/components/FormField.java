package com.donemosmobile.ui.components;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Campo clickable con etiqueta y valor dentro de una caja de 64dp, con el error
 * debajo. Base de fecha, hora y resultado de busqueda.
 */
public class FormField extends LinearLayout {

  private final LinearLayout field;
  private final TextView valueView;
  private final ErrorText errorView;

  public FormField(Context context, String label) {
    this(context, label, false);
  }

  public FormField(Context context, String label, boolean mediumValue) {
    super(context);
    setOrientation(VERTICAL);
    setGravity(Gravity.CENTER_VERTICAL);

    field = new LinearLayout(context);
    field.setOrientation(VERTICAL);
    field.setGravity(Gravity.CENTER_VERTICAL);
    field.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()), 0,
        AppSpacing.dp(AppSpacing.MD, getResources()), 0);
    field.setBackground(Ui.rounded(context, AppColors.SURFACE, 16));
    field.setClickable(true);
    field.setFocusable(true);

    TextView labelView = new TextView(context);
    labelView.setText(label);
    AppTypography.caption(labelView);
    field.addView(labelView);

    valueView = new TextView(context);
    valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    valueView.setTextColor(AppColors.TEXT_SECONDARY);
    if (mediumValue) {
      valueView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }
    field.addView(valueView);

    addView(field, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(64, getResources())));

    errorView = new ErrorText(context);
    addView(errorView);

    setLayoutParams(Ui.matchWrap(context, AppSpacing.SM));
  }

  public TextView value() {
    return valueView;
  }

  public ErrorText error() {
    return errorView;
  }

  /** Setea el valor y lo pinta como elegido. */
  public void setValue(String text) {
    valueView.setText(text);
    valueView.setTextColor(AppColors.TEXT_PRIMARY);
  }

  /** Limpia el valor y lo vuelve a estado placeholder. */
  public void resetValue() {
    valueView.setText("");
    valueView.setTextColor(AppColors.TEXT_SECONDARY);
  }

  public void setFieldOnClickListener(OnClickListener listener) {
    field.setOnClickListener(listener);
  }

  public void setFieldEnabled(boolean enabled) {
    field.setEnabled(enabled);
    field.setAlpha(enabled ? 1f : 0.5f);
  }
}
