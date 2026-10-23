package com.donemosmobile;

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
}
