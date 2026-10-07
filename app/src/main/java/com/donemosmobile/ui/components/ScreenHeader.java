package com.donemosmobile.ui.components;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Encabezado de pantalla: titulo y subtitulo, con "Volver" opcional a la
 * izquierda y una accion opcional a la derecha. Reemplaza el armado manual del
 * header en cada Activity.
 */
public class ScreenHeader extends LinearLayout {

  private final LinearLayout titles;

  public ScreenHeader(Context context) {
    super(context);
    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER_VERTICAL);
    setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        0);

    titles = new LinearLayout(context);
    titles.setOrientation(VERTICAL);
    addView(titles, new LinearLayout.LayoutParams(
        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
  }

  public ScreenHeader title(String text) {
    TextView title = new TextView(getContext());
    title.setText(text);
    AppTypography.headline1(title);
    titles.addView(title);
    return this;
  }

  public ScreenHeader subtitle(String text) {
    TextView subtitle = new TextView(getContext());
    subtitle.setText(text);
    AppTypography.caption(subtitle);
    titles.addView(subtitle);
    return this;
  }

  /** Agrega el "‹ Volver" a la izquierda del titulo. */
  public ScreenHeader back(OnClickListener listener) {
    TextView back = new TextView(getContext());
    back.setText("\u2039 Volver");
    back.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    back.setTextColor(AppColors.PRIMARY);
    back.setPadding(0, AppSpacing.dp(AppSpacing.XS, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()));
    back.setClickable(true);
    back.setFocusable(true);
    back.setOnClickListener(listener);
    addView(back, 0);
    return this;
  }

  /** Agrega una accion a la derecha del titulo. */
  public ScreenHeader action(View view) {
    addView(view);
    return this;
  }
}
