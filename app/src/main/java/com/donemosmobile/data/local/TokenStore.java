package com.donemosmobile.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/**
 * Guarda el token Bearer cifrado en disco.
 *
 * <p>Usa {@link EncryptedSharedPreferences} respaldado por el keystore del
 * dispositivo. Si el keystore falla (dispositivo sin soporte o clave corrupta)
 * cae a SharedPreferences planas para no dejar la app inutilizable; minSdk 24
 * garantiza que el camino cifrado esté disponible.
 */
public final class TokenStore {

  private static final String PREFS_NAME = "donemos_secure_prefs";
  private static final String PREFS_FALLBACK_NAME = "donemos_prefs_fallback";
  private static final String KEY_TOKEN = "auth_token";

  private static volatile TokenStore instance;

  private final SharedPreferences prefs;

  private TokenStore(Context context) {
    Context appContext = context.getApplicationContext();
    this.prefs = openPrefs(appContext);
  }

  private static SharedPreferences openPrefs(Context context) {
    try {
      MasterKey masterKey = new MasterKey.Builder(context)
          .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
          .build();
      return EncryptedSharedPreferences.create(
          context,
          PREFS_NAME,
          masterKey,
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    } catch (Exception e) {
      // Fallback plano: preferible a romper el arranque por un keystore roto.
      return context.getSharedPreferences(PREFS_FALLBACK_NAME, Context.MODE_PRIVATE);
    }
  }

  public static TokenStore getInstance(Context context) {
    TokenStore local = instance;
    if (local == null) {
      synchronized (TokenStore.class) {
        local = instance;
        if (local == null) {
          local = new TokenStore(context);
          instance = local;
        }
      }
    }
    return local;
  }

  public void saveToken(String token) {
    prefs.edit().putString(KEY_TOKEN, token).apply();
  }

  public String getToken() {
    return prefs.getString(KEY_TOKEN, null);
  }

  public boolean hasToken() {
    String token = getToken();
    return token != null && !token.isEmpty();
  }

  public void clear() {
    prefs.edit().remove(KEY_TOKEN).apply();
  }
}
