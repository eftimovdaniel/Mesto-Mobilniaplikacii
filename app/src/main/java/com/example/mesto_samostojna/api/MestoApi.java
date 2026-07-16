package com.example.mesto_samostojna.api;

import com.example.mesto_samostojna.data.Company;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit interface — REST povici kon backend mesto-api (Express + Postgres/Supabase).
 *
 * Retrofit: deklarativni HTTP metodi namesto racen OkHttp kod.
 * Base URL od {@link ApiClient} (local.properties → BuildConfig.BACKEND_URL).
 * Call&lt;&gt; e asinhron — enqueue() na background, callback na UI thread.
 */
public interface MestoApi {

    /** GET /companies — lista na site kompanii od bazata. */
    @GET("companies")
    Call<List<Company>> listCompanies();

    /** POST /companies — dodavanje nova kompanija (body = JSON od {@link Company}). */
    @POST("companies")
    Call<Company> createCompany(@Body Company company);

    /** DELETE /companies/{id} — brisenje na kompanija po ID. */
    @DELETE("companies/{id}")
    Call<ResponseBody> deleteCompany(@Path("id") int id);
}
