package com.donemosmobile.ui.components;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.donemosmobile.ui.theme.AppSpacing;

/** Columna de acciones de formulario: primaria y secundarias. */
public class FormActions extends LinearLayout {

  public FormActions(Context context) {
    super(context);
    setOrientation(VERTICAL);
  }

  public AppButton add(String text, AppButton.Style style, OnClickListener listener,
      int heightDp, int topMarginDp) {
    AppButton button = new AppButton(getContext(), text, style, listener);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(heightDp, getResources()));
    params.topMargin = AppSpacing.dp(topMarginDp, getResources());
    addView(button, params);
    return button;
  }
}
