package com.donemosmobile;

import android.app.Application;

import com.donemosmobile.ui.auth.SessionExpired;

/** App entry point: wires the UI reactions the data layer raises. */
public class DonemosApp extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    // A 401 anywhere wipes the session and returns to login, once.
    SessionExpired.register(this);
  }
}
