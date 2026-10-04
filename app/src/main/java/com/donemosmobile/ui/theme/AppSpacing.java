package com.donemosmobile.ui.theme;

import android.content.res.Resources;

public final class AppSpacing {

  private AppSpacing() {}

  public static int dp(int value, Resources res) {
    return (int) (value * res.getDisplayMetrics().density);
  }

  public static final int XS = 4;
  public static final int SM = 8;
  public static final int MD = 16;
  public static final int LG = 24;
  public static final int XL = 32;
  public static final int XXL = 48;
}
