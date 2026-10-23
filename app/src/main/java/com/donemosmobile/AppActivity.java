package com.donemosmobile;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.donemosmobile.ui.LocaleHelper;
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
  }
}
