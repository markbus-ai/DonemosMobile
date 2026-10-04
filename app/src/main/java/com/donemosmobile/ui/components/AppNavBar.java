package com.donemosmobile.ui.components;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;

public class AppNavBar extends LinearLayout {

  public interface OnItemSelectedListener {
    void onItemSelected(int position);
  }

  private final TextView[] items;
  private int selected = 0;

  public AppNavBar(Context context, String[] labels, OnItemSelectedListener listener) {
    super(context);

    setOrientation(HORIZONTAL);
    setBackgroundColor(AppColors.BACKGROUND);
    setElevation(dp(8));

    items = new TextView[labels.length];

    for (int i = 0; i < labels.length; i++) {
      TextView item = new TextView(context);
      item.setText(labels[i]);
      item.setTextSize(13);
      item.setGravity(Gravity.CENTER);

      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
          0, dp(52), 1);
      final int position = i;
      item.setOnClickListener(v -> {
        selected = position;
        refresh();
        if (listener != null) {
          listener.onItemSelected(position);
        }
      });

      items[i] = item;
      addView(item, params);
    }

    refresh();
  }

  private void refresh() {
    for (int i = 0; i < items.length; i++) {
      if (i == selected) {
        items[i].setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        items[i].setTextColor(AppColors.PRIMARY);
      } else {
        items[i].setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        items[i].setTextColor(AppColors.TEXT_SECONDARY);
      }
    }
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density);
  }
}
