package com.donemosmobile.ui.home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.data.auth.AuthRepository;
import com.donemosmobile.ui.auth.LoginActivity;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

public class HomeHeader extends LinearLayout {

  public HomeHeader(Context context) {
    super(context);

    setOrientation(VERTICAL);
    setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()));
    setBackgroundColor(AppColors.BACKGROUND);

    // Fila: título + punto de marca
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);

    TextView title = new TextView(context);
    title.setText("Donemos");
    title.setTextSize(22);
    title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
    title.setTextColor(AppColors.TEXT_PRIMARY);
    row.addView(title, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    // Salir: cerrar sesión y volver al login.
    TextView logout = new TextView(context);
    logout.setText("Salir");
    logout.setTextSize(14);
    logout.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    logout.setTextColor(AppColors.PRIMARY);
    logout.setPadding(
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()));
    logout.setClickable(true);
    logout.setFocusable(true);
    logout.setOnClickListener(v -> logout(context));
    row.addView(logout);

    View dot = new View(context);
    GradientDrawable dotBg = new GradientDrawable();
    dotBg.setColor(AppColors.PRIMARY);
    dotBg.setShape(GradientDrawable.OVAL);
    dot.setBackground(dotBg);
    LayoutParams dotParams = new LayoutParams(
        AppSpacing.dp(10, getResources()),
        AppSpacing.dp(10, getResources()));
    dotParams.leftMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    row.addView(dot, dotParams);

    addView(row, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    // Contexto: panel interno
    TextView subtitle = new TextView(context);
    subtitle.setText("Panel del personal");
    AppTypography.caption(subtitle);
    LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    subParams.topMargin = AppSpacing.dp(2, getResources());
    addView(subtitle, subParams);
  }

  /**
   * Limpia la sesión local (aunque el server no responda) y navega al login
   * limpiando el back stack para que no se pueda volver a la Home.
   */
  private void logout(Context context) {
    new AuthRepository(context).logout(new AuthRepository.Callback<Void>() {
      @Override
      public void onSuccess(Void data) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
      }

      @Override
      public void onError(AuthRepository.ErrorType type, String message) {
        // logout() nunca responde con error: la limpieza local es incondicional.
      }
    });
  }
}
