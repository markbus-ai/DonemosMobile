package com.donemosmobile;

import android.os.Bundle;

import com.donemosmobile.ui.home.Home;

public class DonemosMobileActivity extends AppActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Home home = new Home(this);
    setContentView(home);
  }
}
