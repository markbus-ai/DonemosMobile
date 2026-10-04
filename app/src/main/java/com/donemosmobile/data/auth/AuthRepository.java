package com.donemosmobile.data.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.donemosmobile.data.local.TokenStore;
import com.donemosmobile.data.network.ApiClient;
import com.donemosmobile.data.network.AuthApi;
import com.donemosmobile.data.network.dto.ErrorResponse;
import com.donemosmobile.data.network.dto.LoginRequest;
import com.donemosmobile.data.network.dto.LoginResponse;
import com.donemosmobile.data.network.dto.UserDto;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Punto de entrada a auth. Corre la red con {@code enqueue} de Retrofit (fuera
 * del hilo principal) y entrega el resultado por callback en el hilo principal.
 */
public class AuthRepository {

  public enum ErrorType {
    INVALID_CREDENTIALS,
    TOO_MANY_ATTEMPTS,
    UNAUTHORIZED,
    NETWORK,
    SERVER,
    UNKNOWN
  }

  public interface Callback<T> {
    void onSuccess(@Nullable T data);
    void onError(@NonNull ErrorType type, @Nullable String message);
  }

  private final AuthApi api;
  private final TokenStore tokenStore;
  private final Gson gson = new Gson();

  public AuthRepository(Context context) {
    ApiClient client = ApiClient.get(context);
    this.api = client.auth();
    this.tokenStore = client.tokenStore();
  }

  /** Autentica y persiste el token. Devuelve el usuario en {@code onSuccess}. */
  public void login(String username, String password, final Callback<UserDto> callback) {
    api.login(new LoginRequest(username, password))
        .enqueue(new retrofit2.Callback<LoginResponse>() {
          @Override
          public void onResponse(@NonNull Call<LoginResponse> call,
              @NonNull Response<LoginResponse> response) {
            LoginResponse body = response.body();
            if (response.isSuccessful() && body != null
                && body.token != null && !body.token.isEmpty()) {
              tokenStore.saveToken(body.token);
              callback.onSuccess(body.user);
              return;
            }
            if (response.code() == 401) {
              callback.onError(ErrorType.INVALID_CREDENTIALS, extractMessage(response));
              return;
            }
            if (response.code() == 429) {
              callback.onError(ErrorType.TOO_MANY_ATTEMPTS, extractMessage(response));
              return;
            }
            callback.onError(mapStatus(response.code()), extractMessage(response));
          }

          @Override
          public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
            callback.onError(ErrorType.NETWORK, t.getMessage());
          }
        });
  }

  /**
   * Valida la sesión actual contra el backend. Un 401 limpia el token guardado
   * y responde {@link ErrorType#UNAUTHORIZED}.
   */
  public void me(final Callback<UserDto> callback) {
    api.me().enqueue(new retrofit2.Callback<UserDto>() {
      @Override
      public void onResponse(@NonNull Call<UserDto> call, @NonNull Response<UserDto> response) {
        if (response.isSuccessful()) {
          callback.onSuccess(response.body());
          return;
        }
        if (response.code() == 401) {
          clearSession();
          callback.onError(ErrorType.UNAUTHORIZED, extractMessage(response));
          return;
        }
        callback.onError(mapStatus(response.code()), extractMessage(response));
      }

      @Override
      public void onFailure(@NonNull Call<UserDto> call, @NonNull Throwable t) {
        callback.onError(ErrorType.NETWORK, t.getMessage());
      }
    });
  }

  /**
   * Cierra la sesión. La limpieza local ocurre siempre, aunque el server no
   * responda; el callback se dispara igual para no trabar la UI.
   */
  public void logout(final Callback<Void> callback) {
    api.logout().enqueue(new retrofit2.Callback<Void>() {
      @Override
      public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
        clearSession();
        callback.onSuccess(null);
      }

      @Override
      public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
        clearSession();
        callback.onSuccess(null);
      }
    });
  }

  public void clearSession() {
    tokenStore.clear();
  }

  public boolean hasSession() {
    return tokenStore.hasToken();
  }

  private ErrorType mapStatus(int code) {
    return code >= 500 ? ErrorType.SERVER : ErrorType.UNKNOWN;
  }

  @Nullable
  private String extractMessage(Response<?> response) {
    if (response.errorBody() == null) {
      return null;
    }
    try {
      ErrorResponse error = gson.fromJson(response.errorBody().string(), ErrorResponse.class);
      return error != null ? error.message : null;
    } catch (IOException | JsonSyntaxException e) {
      return null;
    }
  }
}
