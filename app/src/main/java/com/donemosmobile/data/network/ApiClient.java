package com.donemosmobile.data.network;

import android.content.Context;

import com.donemosmobile.BuildConfig;
import com.donemosmobile.data.local.TokenStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Cliente HTTP único de la app. Arma OkHttp (auth + logging) y expone la
 * interfaz Retrofit. No hay inyección de dependencias: singleton simple.
 */
public final class ApiClient {

  private static volatile ApiClient instance;

  private final AuthApi authApi;
  private final PacienteApi pacienteApi;
  private final TurnoApi turnoApi;
  private final CatalogoApi catalogoApi;
  private final TokenStore tokenStore;

  private ApiClient(Context context) {
    this.tokenStore = TokenStore.getInstance(context);

    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(BuildConfig.DEBUG
        ? HttpLoggingInterceptor.Level.BODY
        : HttpLoggingInterceptor.Level.NONE);

    OkHttpClient httpClient = new OkHttpClient.Builder()
        .addInterceptor(new AuthInterceptor(tokenStore))
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build();

    Gson gson = new GsonBuilder().create();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build();

    this.authApi = retrofit.create(AuthApi.class);
    this.pacienteApi = retrofit.create(PacienteApi.class);
    this.turnoApi = retrofit.create(TurnoApi.class);
    this.catalogoApi = retrofit.create(CatalogoApi.class);
  }

  public static ApiClient get(Context context) {
    ApiClient local = instance;
    if (local == null) {
      synchronized (ApiClient.class) {
        local = instance;
        if (local == null) {
          local = new ApiClient(context.getApplicationContext());
          instance = local;
        }
      }
    }
    return local;
  }

  public AuthApi auth() {
    return authApi;
  }

  public PacienteApi pacientes() {
    return pacienteApi;
  }

  public TurnoApi turnos() {
    return turnoApi;
  }

  public CatalogoApi catalogo() {
    return catalogoApi;
  }

  public TokenStore tokenStore() {
    return tokenStore;
  }
}
