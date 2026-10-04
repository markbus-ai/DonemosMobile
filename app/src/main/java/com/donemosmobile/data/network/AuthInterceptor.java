package com.donemosmobile.data.network;

import androidx.annotation.NonNull;

import com.donemosmobile.data.local.TokenStore;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/** Inyecta el token Bearer guardado en cada request, si existe. */
class AuthInterceptor implements Interceptor {

  private final TokenStore tokenStore;

  AuthInterceptor(TokenStore tokenStore) {
    this.tokenStore = tokenStore;
  }

  @NonNull
  @Override
  public Response intercept(@NonNull Chain chain) throws IOException {
    Request request = chain.request();
    String token = tokenStore.getToken();
    if (token != null && !token.isEmpty()) {
      request = request.newBuilder()
          .header("Authorization", "Bearer " + token)
          .build();
    }
    return chain.proceed(request);
  }
}
