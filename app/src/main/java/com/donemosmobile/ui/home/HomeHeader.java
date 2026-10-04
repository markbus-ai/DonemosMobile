package com.donemosmobile.ui.home;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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

    View dot = new View(context);
    GradientDrawable dotBg = new GradientDrawable();
    dotBg.setColor(AppColors.PRIMARY);
    dotBg.setShape(GradientDrawable.OVAL);
    dot.setBackground(dotBg);
    row.addView(dot, new LayoutParams(
        AppSpacing.dp(10, getResources()),
        AppSpacing.dp(10, getResources())));

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
}
