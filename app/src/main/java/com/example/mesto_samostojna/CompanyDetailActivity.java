package com.example.mesto_samostojna;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mesto_samostojna.data.Company;
import com.example.mesto_samostojna.util.GlideLogoLoader;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Детален преглед на компанија со clickable полиња (тел/мејл/веб/мапа). */
public class CompanyDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COMPANY = "extra_company";

    private static final Map<String, Integer> CATEGORY_LABELS = new HashMap<>();

    static {
        CATEGORY_LABELS.put("service", R.string.cat_service);
        CATEGORY_LABELS.put("entertainment", R.string.cat_entertainment);
        CATEGORY_LABELS.put("industry", R.string.cat_industry);
        CATEGORY_LABELS.put("education", R.string.cat_education);
        CATEGORY_LABELS.put("other", R.string.cat_other);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_detail);

        Company company = null;
        Intent intent = getIntent();
        if (intent != null) {
            Object raw = intent.getSerializableExtra(EXTRA_COMPANY);
            if (raw instanceof Company) {
                company = (Company) raw;
            }
        }
        if (company == null) {
            finish();
            return;
        }
        final Company c = company;

        MaterialToolbar toolbar = findViewById(R.id.detail_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(R.string.detail_title);

        ImageView logo = findViewById(R.id.detail_logo);
        TextView name = findViewById(R.id.detail_name);
        TextView address = findViewById(R.id.detail_address);
        TextView coords = findViewById(R.id.detail_coords);
        TextView phone = findViewById(R.id.detail_phone);
        TextView email = findViewById(R.id.detail_email);
        TextView website = findViewById(R.id.detail_website);
        ChipGroup chips = findViewById(R.id.detail_categories);

        GlideLogoLoader.load(logo, c.getImageUrl(), c.getWebsite());
        name.setText(c.getName());
        address.setText(notEmpty(c.getAddress(), getString(R.string.detail_no_address)));
        coords.setText(String.format(
                Locale.getDefault(), "%.5f° N · %.5f° E", c.getLatitude(), c.getLongitude()));
        phone.setText(notEmpty(c.getPhone(), getString(R.string.detail_no_phone)));
        email.setText(notEmpty(c.getEmail(), getString(R.string.detail_no_email)));
        website.setText(notEmpty(c.getWebsite(), getString(R.string.detail_no_website)));

        chips.removeAllViews();
        if (c.getCategories() != null) {
            for (String slug : c.getCategories()) {
                Integer label = CATEGORY_LABELS.get(slug);
                Chip chip = new Chip(this);
                chip.setText(label != null ? getString(label) : slug);
                chip.setClickable(false);
                chip.setCheckable(false);
                chips.addView(chip);
            }
        }

        findViewById(R.id.detail_address_row).setOnClickListener(v -> openMap(c));
        findViewById(R.id.detail_phone_row).setOnClickListener(v -> dial(c.getPhone()));
        findViewById(R.id.detail_email_row).setOnClickListener(v -> email(c.getEmail()));
        findViewById(R.id.detail_website_row).setOnClickListener(v -> openWeb(c.getWebsite()));
    }

    private String notEmpty(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private void openMap(Company c) {
        String label = Uri.encode(c.getName() != null ? c.getName() : "");
        Uri geo = Uri.parse(String.format(
                Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                c.getLatitude(), c.getLongitude(),
                c.getLatitude(), c.getLongitude(), label));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, geo);
        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            Uri web = Uri.parse(String.format(
                    Locale.US,
                    "https://www.google.com/maps/search/?api=1&query=%f,%f",
                    c.getLatitude(), c.getLongitude()));
            safeView(web, R.string.detail_no_app);
        }
    }

    private void dial(String number) {
        if (TextUtils.isEmpty(number)) {
            Toast.makeText(this, R.string.detail_no_phone, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.trim()));
        try {
            startActivity(dialIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.detail_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void email(String addr) {
        if (TextUtils.isEmpty(addr)) {
            Toast.makeText(this, R.string.detail_no_email, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + addr.trim()));
        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.detail_pick_app)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.detail_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void openWeb(String url) {
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(this, R.string.detail_no_website, Toast.LENGTH_SHORT).show();
            return;
        }
        String normalized = url.trim();
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://" + normalized;
        }
        safeView(Uri.parse(normalized), R.string.detail_no_app);
    }

    private void safeView(Uri uri, int errorRes) {
        Intent view = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(view);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, errorRes, Toast.LENGTH_SHORT).show();
        }
    }
}
