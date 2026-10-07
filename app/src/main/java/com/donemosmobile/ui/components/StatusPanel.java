package com.donemosmobile.ui.components;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Panel de resultado con titulo, cuerpo libre y acciones. Se usa para el aviso
 * 409 y el exito 201 de Donaciones.
 */
public class StatusPanel extends LinearLayout {

  private final Context context;
  private final LinearLayout body;

  public StatusPanel(Context context, String title, int backgroundColor) {
    super(context);
    this.context = context;
    setOrientation(VERTICAL);
    setBackground(Ui.rounded(context, backgroundColor, 12));
    setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()));
    setVisibility(GONE);

    TextView titleView = new TextView(context);
    titleView.setText(title);
    AppTypography.bodyMedium(titleView);
    addView(titleView);

    body = new LinearLayout(context);
    body.setOrientation(VERTICAL);
    addView(body);
  }

  public LinearLayout body() {
    return body;
  }

  public AppButton addAction(String text, AppButton.Style style, OnClickListener listener,
      int topMarginDp) {
    AppButton button = new AppButton(context, text, style, listener);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, AppSpacing.dp(48, getResources()));
    params.topMargin = AppSpacing.dp(topMarginDp, getResources());
    body.addView(button, params);
    return button;
  }

  public void show() {
    setVisibility(VISIBLE);
  }

  public void hide() {
    setVisibility(GONE);
  }
}
