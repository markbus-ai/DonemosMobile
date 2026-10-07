package com.donemosmobile.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.donemosmobile.data.network.dto.UserDto;

/**
 * Guarda una copia liviana del personal autenticado para poder mostrar
 * usuario, rol y sede sin volver a pegarle a la API (por ejemplo en "Más").
 *
 * <p>No guarda el token: eso vive cifrado en {@link TokenStore}. Acá van sólo
 * datos de presentación.
 */
public final class SessionStore {

  private static final String PREFS_NAME = "donemos_session";
  private static final String KEY_USERNAME = "user_username";
  private static final String KEY_NOMBRE = "user_nombre";
  private static final String KEY_ROL = "user_rol";
  private static final String KEY_SEDE = "user_sede";

  private static volatile SessionStore instance;

  private final SharedPreferences prefs;

  private SessionStore(Context context) {
    this.prefs = context.getApplicationContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
  }

  public static SessionStore getInstance(Context context) {
    SessionStore local = instance;
    if (local == null) {
      synchronized (SessionStore.class) {
        local = instance;
        if (local == null) {
          local = new SessionStore(context);
          instance = local;
        }
      }
    }
    return local;
  }

  /** Persiste los campos de presentación del usuario. Ignora {@code null}. */
  public void save(@Nullable UserDto user) {
    if (user == null) {
      return;
    }
    prefs.edit()
        .putString(KEY_USERNAME, user.username)
        .putString(KEY_NOMBRE, user.nombre)
        .putString(KEY_ROL, user.rol)
        .putString(KEY_SEDE, user.sede)
        .apply();
  }

  /** Usuario cacheado o {@code null} si todavía no se guardó ninguno. */
  @Nullable
  public UserDto getUser() {
    String username = prefs.getString(KEY_USERNAME, null);
    String nombre = prefs.getString(KEY_NOMBRE, null);
    String rol = prefs.getString(KEY_ROL, null);
    String sede = prefs.getString(KEY_SEDE, null);
    if (username == null && nombre == null && rol == null && sede == null) {
      return null;
    }
    UserDto user = new UserDto();
    user.username = username;
    user.nombre = nombre;
    user.rol = rol;
    user.sede = sede;
    return user;
  }

  public void clear() {
    prefs.edit().clear().apply();
  }
}
