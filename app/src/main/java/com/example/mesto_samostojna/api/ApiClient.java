package com.example.mesto_samostojna.api;

import com.example.mesto_samostojna.BuildConfig;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Retrofit кон mesto backend (Render). Base URL од local.properties → BuildConfig. */
public final class ApiClient {

    private static volatile MestoApi api;

    private ApiClient() {}

    @SuppressWarnings({"ConstantConditions", "ConstantValue"})
    public static MestoApi getApi() {
        if (api == null) {
            synchronized (ApiClient.class) {
                if (api == null) {
                    // BACKEND_URL е compile-time constant (buildConfigField од local.properties).
                    // Проверките подолу служат како runtime safety net ако некој
                    // build-а без поставен backend.url; lint warnings се очекувани.
                    String baseUrl = BuildConfig.BACKEND_URL;
                    if (baseUrl == null || baseUrl.isEmpty()) {
                        throw new IllegalStateException(
                                "Во local.properties стави backend.url=https://<твој-render-url>");
                    }
                    if (!baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/";
                    }

                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    // Render free план „заспива" по 15 мин — првиот повик потоа
                    // трае ~15–30 s. Стандардниот OkHttp timeout е 10 s, па
                    // зголемуваме за да не паѓа на cold start.
                    OkHttpClient ok =
                            new OkHttpClient.Builder()
                                    .addInterceptor(logging)
                                    .connectTimeout(45, TimeUnit.SECONDS)
                                    .readTimeout(45, TimeUnit.SECONDS)
                                    .writeTimeout(45, TimeUnit.SECONDS)
                                    .callTimeout(60, TimeUnit.SECONDS)
                                    .build();

                    Retrofit retrofit =
                            new Retrofit.Builder()
                                    .baseUrl(baseUrl)
                                    .client(ok)
                                    .addConverterFactory(GsonConverterFactory.create(new Gson()))
                                    .build();
                    api = retrofit.create(MestoApi.class);
                }
            }
        }
        return api;
    }
}
