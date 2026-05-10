package com.example.mesto_samostojna.api;

import com.example.mesto_samostojna.BuildConfig;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static volatile MestoApi api;

    private ApiClient() {}

    public static MestoApi getApi() {
        if (api == null) {
            synchronized (ApiClient.class) {
                if (api == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(
                            BuildConfig.DEBUG
                                    ? HttpLoggingInterceptor.Level.BASIC
                                    : HttpLoggingInterceptor.Level.NONE);

                    OkHttpClient ok =
                            new OkHttpClient.Builder().addInterceptor(logging).build();

                    Retrofit retrofit =
                            new Retrofit.Builder()
                                    .baseUrl(BuildConfig.API_BASE_URL)
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
