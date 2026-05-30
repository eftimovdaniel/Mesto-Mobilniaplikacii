package com.example.mesto_samostojna.api;

import com.example.mesto_samostojna.BuildConfig;
import com.google.gson.Gson;

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

                    OkHttpClient ok =
                            new OkHttpClient.Builder().addInterceptor(logging).build();

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
