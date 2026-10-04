package com.donemosmobile.ui.theme;

import android.graphics.Color;

public final class AppColors {

  private AppColors() {}

  // Accent — rojo solo para acentos
  public static final int PRIMARY = Color.parseColor("#E5484D");
  public static final int PRIMARY_DARK = Color.parseColor("#C5373C");
  public static final int PRIMARY_TINT = Color.parseColor("#FDECEC");

  // Surfaces — blanco puro + gris suave para cards
  public static final int BACKGROUND = Color.WHITE;
  public static final int SURFACE = Color.parseColor("#F2F2F7");
  public static final int SURFACE_PRESSED = Color.parseColor("#E5E5EA");

  // Text
  public static final int TEXT_PRIMARY = Color.parseColor("#1C1C1E");
  public static final int TEXT_SECONDARY = Color.parseColor("#8E8E93");
  public static final int TEXT_ON_PRIMARY = Color.WHITE;

  // Divider
  public static final int DIVIDER = Color.parseColor("#E5E5EA");

  // State
  public static final int SUCCESS = Color.parseColor("#30A46C");
  public static final int WARNING = Color.parseColor("#F5A524");
  public static final int ERROR = Color.parseColor("#E5484D");
}
