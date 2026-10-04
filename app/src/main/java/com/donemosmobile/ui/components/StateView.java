package com.donemosmobile.ui.components;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Vista reutilizable para los tres estados de una pantalla que carga datos:
 * cargando, vacío y error. Cada pantalla debe además ocultar el contenedor
 * cuando tiene contenido, para que el comportamiento sea consistente.
 *
 * <p>Uso típico:
 * <pre>
 *   StateView state = new StateView(this);
 *   root.addView(state);
 *   state.showLoading();
 *   // ...
 *   state.showEmpty("No hay turnos para hoy");
 *   // o
 *   state.showError("No se pudo conectar", retryRunnable);
 * </pre>
 */
public class StateView extends LinearLayout {

  private final TextView title;
  private final TextView message;
  private final AppButton retryButton;

  public StateView(Context context) {
    super(context);

    setOrientation(VERTICAL);
    setGravity(Gravity.CENTER);
    setPadding(
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.XXL, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.XXL, getResources()));

    title = new TextView(context);
    title.setGravity(Gravity.CENTER);
    AppTypography.bodyMedium(title);
    addView(title);

    message = new TextView(context);
    message.setGravity(Gravity.CENTER);
    AppTypography.caption(message);
    LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    messageParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    addView(message, messageParams);

    retryButton = new AppButton(context, "Reintentar", AppButton.Style.OUTLINED, v -> {});
    LinearLayout.LayoutParams retryParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        AppSpacing.dp(44, getResources()));
    retryParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    addView(retryButton, retryParams);

    setVisibility(View.GONE);
  }

  /** Muestra el indicador de carga. */
  public void showLoading() {
    title.setText("Cargando…");
    title.setTextColor(AppColors.TEXT_SECONDARY);
    message.setVisibility(View.GONE);
    retryButton.setVisibility(View.GONE);
    setVisibility(View.VISIBLE);
  }

  /** Muestra un estado vacío sin acción. */
  public void showEmpty(String text) {
    title.setText(text);
    title.setTextColor(AppColors.TEXT_SECONDARY);
    message.setVisibility(View.GONE);
    retryButton.setVisibility(View.GONE);
    setVisibility(View.VISIBLE);
  }

  /** Muestra un error con botón de reintento opcional. */
  public void showError(String text, final Runnable onRetry) {
    title.setText(text);
    title.setTextColor(AppColors.ERROR);
    message.setVisibility(View.GONE);
    if (onRetry != null) {
      retryButton.setOnClickListener(v -> onRetry.run());
      retryButton.setVisibility(View.VISIBLE);
    } else {
      retryButton.setVisibility(View.GONE);
    }
    setVisibility(View.VISIBLE);
  }

  /** Oculta la vista cuando la pantalla ya tiene contenido. */
  public void hide() {
    setVisibility(View.GONE);
  }
}
