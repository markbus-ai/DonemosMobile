package com.donemosmobile;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.R;

/**
 * Base para todas las activities de la app.
 * Aplica el tema Material Components antes de que las vistas Material se inflen.
 */
public abstract class AppActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(R.style.Theme_MaterialComponents_DayNight_NoActionBar);
    super.onCreate(savedInstanceState);
  }
}
