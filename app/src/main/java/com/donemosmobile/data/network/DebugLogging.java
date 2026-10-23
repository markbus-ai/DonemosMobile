package com.donemosmobile.data.network;

import androidx.annotation.NonNull;

import okhttp3.Interceptor;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Debug-only HTTP logging that never prints credentials: the Authorization
 * header is redacted everywhere and credential requests (login) log headers
 * only, so neither the password nor the issued token reaches logcat.
 */
final class DebugLogging {

  private static final String AUTHORIZATION = "Authorization";

  private DebugLogging() {}

  static Interceptor create() {
    HttpLoggingInterceptor bodies = new HttpLoggingInterceptor();
    bodies.setLevel(HttpLoggingInterceptor.Level.BODY);
    bodies.redactHeader(AUTHORIZATION);

    HttpLoggingInterceptor headers = new HttpLoggingInterceptor();
    headers.setLevel(HttpLoggingInterceptor.Level.HEADERS);
    headers.redactHeader(AUTHORIZATION);

    return chain -> (isCredentialRequest(chain.request().url().encodedPath()) ? headers : bodies)
        .intercept(chain);
  }

  /** True for requests whose bodies carry credentials or tokens. */
  static boolean isCredentialRequest(@NonNull String encodedPath) {
    return encodedPath.endsWith("/auth/login");
  }
}
