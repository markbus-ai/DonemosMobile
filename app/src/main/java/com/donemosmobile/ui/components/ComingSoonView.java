package com.donemosmobile.ui.components;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.donemosmobile.ui.theme.AppTypography;

/**
 * Estado honesto para funciones que todavía no existen: badge
 * "PRÓXIMAMENTE", título y explicación. Evita las pantallas que sólo mostraban
 * un título y dejaban al usuario sin contexto.
 */
public class ComingSoonView extends LinearLayout {

  public ComingSoonView(Context context, String title, String explanation) {
    super(context);
    setOrientation(VERTICAL);
    setGravity(Gravity.CENTER);
    setPadding(
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.XXL, getResources()),
        AppSpacing.dp(AppSpacing.LG, getResources()),
        AppSpacing.dp(AppSpacing.XXL, getResources()));

    TextView badge = new TextView(context);
    badge.setText("PRÓXIMAMENTE");
    badge.setTextSize(12);
    badge.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    badge.setTextColor(AppColors.PRIMARY);
    badge.setBackground(rounded(AppColors.PRIMARY_TINT, 20));
    badge.setPadding(
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()),
        AppSpacing.dp(AppSpacing.SM, getResources()),
        AppSpacing.dp(AppSpacing.XS, getResources()));
    addView(badge);

    TextView titleView = new TextView(context);
    titleView.setText(title);
    titleView.setGravity(Gravity.CENTER);
    AppTypography.headline1(titleView);
    LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    titleParams.topMargin = AppSpacing.dp(AppSpacing.MD, getResources());
    addView(titleView, titleParams);

    TextView explanationView = new TextView(context);
    explanationView.setText(explanation);
    explanationView.setGravity(Gravity.CENTER);
    AppTypography.body(explanationView);
    explanationView.setTextColor(AppColors.TEXT_SECONDARY);
    LinearLayout.LayoutParams explParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    explParams.topMargin = AppSpacing.dp(AppSpacing.SM, getResources());
    addView(explanationView, explParams);
  }

  private GradientDrawable rounded(int color, float radiusDp) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(color);
    drawable.setCornerRadius(AppSpacing.dp((int) radiusDp, getResources()));
    return drawable;
  }
}
