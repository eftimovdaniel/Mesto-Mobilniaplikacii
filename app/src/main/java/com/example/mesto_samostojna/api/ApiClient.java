package com.example.mesto_samostojna.api;

import com.example.mesto_samostojna.BuildConfig;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton Retrofit klient kon mesto backend (Render).
 *
 * Zosto:
 * - Base URL od local.properties → BuildConfig (ne e hardkodiran; ne odi na Git).
 * - Dolgi timeouti: Render free "zaspiva" po ~15 min; prv request moze da trae 15–30 s.
 *   Default OkHttp (10 s) bi padnal na cold start — zatoa 45–60 s.
 * - volatile + synchronized: bezbedno kreiranje od poveke threads.
 */
public final class ApiClient {

    private static volatile MestoApi api;

    private ApiClient() {}

    @SuppressWarnings({"ConstantConditions", "ConstantValue"})
    public static MestoApi getApi() {
        if (api == null) {
            synchronized (ApiClient.class) {
                if (api == null) {
                    // BACKEND_URL = compile-time constant od local.properties (buildConfigField).
                    String baseUrl = BuildConfig.BACKEND_URL;
                    if (baseUrl == null || baseUrl.isEmpty()) {
                        throw new IllegalStateException(
                                "Vo local.properties stavi backend.url=https://<tvoj-render-url>");
                    }
                    if (!baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/"; // Retrofit bara trailing slash
                    }

                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    // Pogolemi timeouti poradi Render cold start (free plan).
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
