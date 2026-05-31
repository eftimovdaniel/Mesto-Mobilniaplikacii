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

/** REST повици кон сопствениот backend (Express + Postgres). */
public interface MestoApi {

    @GET("companies")
    Call<List<Company>> listCompanies();

    @POST("companies")
    Call<Company> createCompany(@Body Company company);

    @DELETE("companies/{id}")
    Call<ResponseBody> deleteCompany(@Path("id") int id);
}
