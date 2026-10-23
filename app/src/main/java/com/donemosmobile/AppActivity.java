package com.donemosmobile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.donemosmobile.ui.LocaleHelper;
import com.donemosmobile.ui.theme.AppColors;
import com.google.android.material.R;

/**
 * Base para todas las activities de la app.
 * Aplica el tema Material Components antes de que las vistas Material se inflen
 * y fija el locale español para que los widgets nativos no mezclen idiomas.
 */
public abstract class AppActivity extends AppCompatActivity {

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(LocaleHelper.wrap(newBase));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.Theme_MaterialComponents_Light_NoActionBar);
    super.onCreate(savedInstanceState);
    // The Material default status bar is purple; match the white screens and
    // use dark status icons on it.
    getWindow().setStatusBarColor(AppColors.BACKGROUND);
    WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
        .setAppearanceLightStatusBars(true);
  }

  /**
   * Lifecycle guard for asynchronous callbacks: false once the screen is
   * finishing or destroyed, when results must no longer touch its views.
   */
  public boolean isAlive() {
    return !isFinishing() && !isDestroyed();
  }

  /** Same guard for views that only hold their host {@link Context}. */
  public static boolean isAlive(Context context) {
    if (context instanceof Activity) {
      Activity activity = (Activity) context;
      return !activity.isFinishing() && !activity.isDestroyed();
    }
    return true;
  }
}
