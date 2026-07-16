package com.example.mesto_samostojna.data;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model za edna kompanija vo aplikacijata "Mesto".
 *
 * ZOSO: Retrofit/Gson treba Java klasa za da go mapira JSON-ot od API.
 * Poleinjata se 1:1 so backend (GET/POST /companies).
 * image_url (snake_case) → imageUrl (camelCase) preku @SerializedName.
 * Serializable: da se prati preku Intent do CompanyDetailActivity.
 */
public class Company implements Serializable {

    // serialVersionUID: fiksna verzija za Serializable — sprecuva greska pri
    // deserijalizacija ako klasata se prati preku Intent megju verzii.
    private static final long serialVersionUID = 1L;

    // Poleinjata gi popolnuva Gson od JSON-ot na API. Imenata mora da se
    // sovpaganat so backend-ot (osven kade sto ima @SerializedName).

    @Nullable
    private Integer id;          // null pri kreiranje (nova), popolnet po POST od serverot
    private String name;
    private String address;
    private double latitude;     // primitiven double — sekogas ima vrednost (0.0 default)
    private double longitude;
    private String email;
    private String phone;
    private String website;

    // Backend go prakja kako "image_url" (snake_case); @SerializedName go mapira
    // vo Java camelCase imeto imageUrl.
    @Nullable
    @com.google.gson.annotations.SerializedName("image_url")
    private String imageUrl;

    // Inicijalizirana na prazna lista za da ne e null pred setCategories.
    private List<String> categories = new ArrayList<>();

    @Nullable
    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getWebsite() {
        return website;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getCategories() {
        return categories;
    }

    /** Dali kompanijata pripagja na dadena kategorija (slug: service, entertainment, industry, education, other). */
    public boolean hasCategory(String slug) {
        return categories != null && slug != null && categories.contains(slug);
    }

    public void setId(@Nullable Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
    }
}
