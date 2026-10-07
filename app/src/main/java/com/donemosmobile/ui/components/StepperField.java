package com.donemosmobile.ui.components;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;

/** Control de cantidad etiquetado con botones menos/plus. */
public class StepperField extends LinearLayout {

  public interface OnChange {
    void onChange(int value);
  }

  private final TextView valueView;
  private final int max;
  private int value;
  private OnChange onChange;

  public StepperField(Context context, String label, int value, int max) {
    super(context);
    this.max = max;
    this.value = clamp(value);

    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL);
    setBackground(Ui.rounded(context, AppColors.SURFACE, 12));
    setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()));
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    rowParams.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    setLayoutParams(rowParams);

    TextView labelView = new TextView(context);
    labelView.setText(label);
    labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    labelView.setTextColor(AppColors.TEXT_PRIMARY);
    addView(labelView, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    AppButton minus = new AppButton(context, "\u2212", AppButton.Style.TEXT, v -> change(-1));
    minus.setLayoutParams(new LinearLayout.LayoutParams(
        AppSpacing.dp(44, getResources()), AppSpacing.dp(44, getResources())));
    addView(minus);

    valueView = new TextView(context);
    valueView.setText(String.valueOf(this.value));
    valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    valueView.setTextColor(AppColors.TEXT_PRIMARY);
    valueView.setGravity(Gravity.CENTER);
    addView(valueView, new LinearLayout.LayoutParams(
        AppSpacing.dp(36, getResources()), ViewGroup.LayoutParams.WRAP_CONTENT));

    AppButton plus = new AppButton(context, "+", AppButton.Style.TEXT, v -> change(1));
    plus.setLayoutParams(new LinearLayout.LayoutParams(
        AppSpacing.dp(44, getResources()), AppSpacing.dp(44, getResources())));
    addView(plus);
  }

  public int value() {
    return value;
  }

  public void setOnChange(OnChange listener) {
    this.onChange = listener;
  }

  private void change(int delta) {
    value = clamp(value + delta);
    valueView.setText(String.valueOf(value));
    if (onChange != null) {
      onChange.onChange(value);
    }
  }

  private int clamp(int candidate) {
    return Math.max(0, Math.min(max, candidate));
  }
}
