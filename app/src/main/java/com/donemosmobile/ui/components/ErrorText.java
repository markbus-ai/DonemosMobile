package com.donemosmobile.ui.components;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatTextView;

import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;

/**
 * Texto de error de campo, oculto hasta que haga falta. Reemplaza el
 * {@code makeErrorText}/{@code showError}/{@code hideError} repetido en cada
 * pantalla.
 */
public class ErrorText extends AppCompatTextView {

  public ErrorText(Context context) {
    super(context);
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    setTextColor(AppColors.ERROR);
    setVisibility(GONE);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    params.leftMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    setLayoutParams(params);
  }

  public void show(CharSequence message) {
    setText(message);
    setVisibility(VISIBLE);
  }

  public void hide() {
    setVisibility(GONE);
  }
}
