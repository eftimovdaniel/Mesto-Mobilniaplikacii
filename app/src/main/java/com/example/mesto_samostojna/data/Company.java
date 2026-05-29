package com.example.mesto_samostojna.data;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Model za edna kompanija — poleinjata se mapiraat vo JSON od API (POST/GET companies). */
public class Company implements Serializable {

    private static final long serialVersionUID = 1L;

    @Nullable
    private Integer id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String email;
    private String phone;
    private String website;

    @Nullable
    @com.google.gson.annotations.SerializedName("image_url")
    private String imageUrl;

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

    /** Dali kompanijata e vo dadena kategorija (slug: service, entertainment, industry, education). */
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
