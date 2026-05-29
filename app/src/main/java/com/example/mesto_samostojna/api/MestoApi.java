package com.example.mesto_samostojna.api;

import com.example.mesto_samostojna.data.Company;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/** PostgREST повици кон Supabase табела {@code companies}. */
public interface MestoApi {

    @GET("rest/v1/companies?select=*&order=id.desc")
    Call<List<Company>> listCompanies();

    /** Supabase враќа низа со еден запис при {@code Prefer: return=representation}. */
    @POST("rest/v1/companies")
    Call<List<Company>> createCompany(@Body Company company);
}
