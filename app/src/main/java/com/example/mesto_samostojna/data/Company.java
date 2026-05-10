package com.example.mesto_samostojna.data;

import androidx.annotation.Nullable;

/**
 * Одговара на JSON од API / компании во MySQL.
 */
public class Company {

    @Nullable
    private Integer id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String email;
    private String phone;
    private String website;
    private String category;

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

    public String getCategory() {
        return category;
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

    public void setCategory(String category) {
        this.category = category;
    }
}
