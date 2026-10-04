package com.donemosmobile.ui.theme;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.widget.TextView;

public final class AppTypography {

  private AppTypography() {}

  public static void headline1(TextView tv) {
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
    tv.setTextColor(AppColors.TEXT_PRIMARY);
    tv.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
  }

  public static void headline2(TextView tv) {
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
    tv.setTextColor(AppColors.TEXT_PRIMARY);
    tv.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
  }

  public static void subtitle(TextView tv) {
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    tv.setTextColor(AppColors.TEXT_SECONDARY);
  }

  public static void body(TextView tv) {
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    tv.setTextColor(AppColors.TEXT_PRIMARY);
  }

  public static void bodyMedium(TextView tv) {
    body(tv);
    tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
  }

  public static void caption(TextView tv) {
    tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
    tv.setTextColor(AppColors.TEXT_SECONDARY);
  }
}
