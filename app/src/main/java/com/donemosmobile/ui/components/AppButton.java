package com.donemosmobile.ui.components;

import android.content.Context;
import android.view.ViewGroup;

import com.donemosmobile.ui.theme.AppColors;
import com.google.android.material.button.MaterialButton;

public class AppButton extends MaterialButton {

  public enum Style { FILLED, OUTLINED, TEXT }

  public AppButton(Context context, String text, Style style, OnClickListener listener) {
    super(context);

    setText(text);
    setOnClickListener(listener);

    int height = dp(48);
    setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        height));

    switch (style) {
      case FILLED:
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(AppColors.TEXT_ACCENT));
        setTextColor(AppColors.TEXT_ON_PRIMARY);
        break;

      case OUTLINED:
        setStrokeColor(android.content.res.ColorStateList.valueOf(AppColors.PRIMARY));
        setStrokeWidth(dp(1));
        setTextColor(AppColors.TEXT_ACCENT);
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(0));
        removeShadow();
        break;

      case TEXT:
        setStrokeWidth(0);
        setTextColor(AppColors.TEXT_ACCENT);
        setBackgroundTintList(android.content.res.ColorStateList.valueOf(0));
        removeShadow();
        break;
    }
  }

  /** The filled-button shadow shows as a grey box around a transparent button. */
  private void removeShadow() {
    setElevation(0);
    setStateListAnimator(null);
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density);
  }
}
