package com.example.mesto_samostojna.api;

import com.example.mesto_samostojna.data.Company;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface MestoApi {

    @GET("companies")
    Call<List<Company>> listCompanies();

    @POST("companies")
    Call<Company> createCompany(@Body Company company);
}
