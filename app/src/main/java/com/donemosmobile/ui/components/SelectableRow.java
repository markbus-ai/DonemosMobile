package com.donemosmobile.ui.components;

import android.content.Context;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatTextView;

import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;

/**
 * Fila seleccionable de un catalogo: el fondo y el color cambian segun este
 * seleccionada. Unifica las filas de tipo de donacion, motivo, bolsa y brazo.
 */
public class SelectableRow extends AppCompatTextView {

  public SelectableRow(Context context, String text, boolean selected, OnClickListener listener) {
    super(context);
    setText(text);
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()));
    setBackground(Ui.rounded(context, selected ? AppColors.PRIMARY_TINT : AppColors.SURFACE, 12));
    setTextColor(selected ? AppColors.TEXT_ACCENT : AppColors.TEXT_PRIMARY);
    setClickable(true);
    setFocusable(true);
    setOnClickListener(listener);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    params.topMargin = AppSpacing.dp(AppSpacing.XS, getResources());
    setLayoutParams(params);
  }
}
