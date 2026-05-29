package com.example.mesto_samostojna.api;

import com.example.mesto_samostojna.BuildConfig;
import com.google.gson.Gson;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Retrofit кон Supabase PostgREST (Publishable key од local.properties). */
public final class ApiClient {

    private static volatile MestoApi api;

    private ApiClient() {}

  /** Lazy init; безбедно за повик од повеќе нишки. */
    public static MestoApi getApi() {
        if (api == null) {
            synchronized (ApiClient.class) {
                if (api == null) {
                    String baseUrl = BuildConfig.SUPABASE_URL;
                    String anonKey = BuildConfig.SUPABASE_ANON_KEY;
                    if (baseUrl == null || baseUrl.isEmpty() || anonKey == null || anonKey.isEmpty()) {
                        throw new IllegalStateException(
                                "Во local.properties стави supabase.url и supabase.anon.key "
                                        + "(види local.properties.example).");
                    }
                    if (!baseUrl.endsWith("/")) {
                        baseUrl = baseUrl + "/";
                    }

                    Interceptor supabaseAuth =
                            chain -> {
                                Request original = chain.request();
                                Request.Builder builder =
                                        original.newBuilder()
                                                .header("apikey", anonKey)
                                                .header("Authorization", "Bearer " + anonKey);
                                if ("POST".equalsIgnoreCase(original.method())) {
                                    builder.header("Prefer", "return=representation");
                                }
                                return chain.proceed(builder.build());
                            };

                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

                    OkHttpClient ok =
                            new OkHttpClient.Builder()
                                    .addInterceptor(supabaseAuth)
                                    .addInterceptor(logging)
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
