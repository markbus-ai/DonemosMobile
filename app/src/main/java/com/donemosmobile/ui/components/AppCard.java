package com.donemosmobile.ui.components;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;
import com.google.android.material.card.MaterialCardView;

public class AppCard extends MaterialCardView {

  public AppCard(Context context, View content) {
    super(context);

    setRadius(dp(12));
    setCardElevation(dp(2));
    setCardBackgroundColor(AppColors.SURFACE);
    setStrokeWidth(0);

    LinearLayout container = new LinearLayout(context);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()),
        AppSpacing.dp(AppSpacing.MD, getResources()));

    container.addView(content, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));

    addView(container, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density);
  }
}
