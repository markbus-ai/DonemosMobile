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
  // 5.23:1 on white, 4.69:1 on SURFACE (WCAG AA body text).
  public static final int TEXT_SECONDARY = Color.parseColor("#6C6C70");
  public static final int TEXT_ON_PRIMARY = Color.WHITE;
  /** Accent used as text or behind white text: 5.27:1 on white (PRIMARY is 3.91:1). */
  public static final int TEXT_ACCENT = PRIMARY_DARK;

  // Divider
  public static final int DIVIDER = Color.parseColor("#E5E5EA");

  // State — fills/accents only. WARNING (2.04:1) and SUCCESS (3.16:1) fail as
  // text on white; use the *_TEXT tokens for anything that must be read.
  public static final int SUCCESS = Color.parseColor("#30A46C");
  public static final int WARNING = Color.parseColor("#F5A524");
  /** Success text: 5.41:1 on white, 4.85:1 on SURFACE. */
  public static final int SUCCESS_TEXT = Color.parseColor("#18794E");
  /** Warning text (dark amber): 5.93:1 on white, 5.31:1 on SURFACE. */
  public static final int WARNING_TEXT = Color.parseColor("#8A5A00");
  /** Error copy is always text: same as PRIMARY_DARK, 5.27:1 on white. */
  public static final int ERROR = PRIMARY_DARK;
}
