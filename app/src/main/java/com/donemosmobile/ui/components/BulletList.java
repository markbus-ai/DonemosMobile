package com.donemosmobile.ui.components;

import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.donemosmobile.R;
import com.donemosmobile.ui.Ui;
import com.donemosmobile.ui.theme.AppColors;
import com.donemosmobile.ui.theme.AppSpacing;

import java.util.List;

/** Bulleted lines ("• message") in body size, one per item, stacked in a parent. */
public final class BulletList {

  private BulletList() {}

  public static void addTo(@NonNull LinearLayout parent, @NonNull List<String> items) {
    Context context = parent.getContext();
    for (String item : items) {
      TextView line = new TextView(context);
      line.setText(context.getString(R.string.bullet_item, item));
      line.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
      line.setTextColor(AppColors.TEXT_PRIMARY);
      parent.addView(line, Ui.matchWrap(context, AppSpacing.XS));
    }
  }
}
