package com.donemosmobile.ui.components;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.donemosmobile.ui.theme.AppColors;

/**
 * Barra de navegación inferior persistente.
 *
 * <p>Dibuja una línea divisoria y una fila de ítems equiespaciados, con la
 * sección actual resaltada. La lógica de a qué pantalla ir queda en el
 * listener: la barra sólo comunica qué posición se tocó.
 */
public class AppNavBar extends LinearLayout {

  public interface OnItemSelectedListener {
    void onItemSelected(int position);
  }

  private final TextView[] items;
  private int selected;

  public AppNavBar(Context context, String[] labels, OnItemSelectedListener listener) {
    this(context, labels, 0, listener);
  }

  public AppNavBar(Context context, String[] labels, int selectedIndex,
      OnItemSelectedListener listener) {
    super(context);

    setOrientation(VERTICAL);
    setBackgroundColor(AppColors.BACKGROUND);
    setElevation(dp(8));

    this.selected = selectedIndex;

    // Línea divisoria superior: separa el contenido de la barra.
    View divider = new View(context);
    divider.setBackgroundColor(AppColors.DIVIDER);
    addView(divider, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));

    LinearLayout row = new LinearLayout(context);
    row.setOrientation(HORIZONTAL);

    items = new TextView[labels.length];

    for (int i = 0; i < labels.length; i++) {
      TextView item = new TextView(context);
      item.setText(labels[i]);
      item.setTextSize(11);
      item.setSingleLine(true);
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
      row.addView(item, params);
    }

    addView(row, new LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    refresh();
  }

  /** Resalta una sección sin disparar navegación. */
  public void setSelected(int position) {
    selected = position;
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
