package com.example.mesto_samostojna;

import android.annotation.SuppressLint;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.mesto_samostojna.api.ApiClient;
import com.example.mesto_samostojna.api.MestoApi;
import com.example.mesto_samostojna.data.Company;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Форма „Нова компанија“. По успешен POST кон API (Retrofit), повикај
 * {@link #setResult(int)} и {@link #finish()} за да се освежи листата.
 */
public class AddCompanyActivity extends AppCompatActivity {

    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    private MaterialButton btnSave;
    private MaterialButton btnFindLocation;

    private TextInputEditText inputAddress;

    private TextInputLayout tilName;
    private TextInputLayout tilAddress;
    private TextInputLayout tilLatitude;
    private TextInputLayout tilLongitude;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPhone;
    private TextInputLayout tilWebsite;

    private TextInputEditText inputLatitude;
    private TextInputEditText inputLongitude;
    private TextView textLocationCoords;
    private ChipGroup chipGroupCategory;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_company);

        tilName = findViewById(R.id.til_name);
        tilAddress = findViewById(R.id.til_address);
        tilLatitude = findViewById(R.id.til_latitude);
        tilLongitude = findViewById(R.id.til_longitude);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilWebsite = findViewById(R.id.til_website);

        inputAddress = findViewById(R.id.input_address);
        inputLatitude = findViewById(R.id.input_latitude);
        inputLongitude = findViewById(R.id.input_longitude);
        textLocationCoords = findViewById(R.id.text_location_coords);
        chipGroupCategory = findViewById(R.id.chip_group_category);

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        TextWatcher locationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateLocationPreview();
            }
        };
        inputLatitude.addTextChangedListener(locationWatcher);
        inputLongitude.addTextChangedListener(locationWatcher);
        updateLocationPreview();

        btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> attemptSave());

        btnFindLocation = findViewById(R.id.btn_find_location);
        btnFindLocation.setOnClickListener(v -> geocodeFromAddress());
    }

    @Override
    protected void onDestroy() {
        geocodeExecutor.shutdownNow();
        super.onDestroy();
    }

    /** Геокодирање од адреса (Geocoder мора да се повика надвор од главната нишка). */
    private void geocodeFromAddress() {
        tilAddress.setError(null);
        String raw = textOf(inputAddress).trim();
        if (TextUtils.isEmpty(raw)) {
            tilAddress.setError(getString(R.string.error_required));
            return;
        }
        if (!Geocoder.isPresent()) {
            Toast.makeText(this, R.string.geocoder_missing, Toast.LENGTH_LONG).show();
            return;
        }

        final String query =
                raw.toLowerCase(Locale.ROOT).contains("македони")
                        || raw.toLowerCase(Locale.ROOT).contains("macedon")
                        ? raw
                        : raw + ", North Macedonia";

        btnFindLocation.setEnabled(false);
        geocodeExecutor.execute(
                () -> {
                    try {
                        List<Address> addresses = lookupAddresses(query);
                        runOnUiThread(
                                () -> {
                                    btnFindLocation.setEnabled(true);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        Address first = addresses.get(0);
                                        inputLatitude.setText(
                                                String.format(
                                                        Locale.US,
                                                        "%.7f",
                                                        first.getLatitude()));
                                        inputLongitude.setText(
                                                String.format(
                                                        Locale.US,
                                                        "%.7f",
                                                        first.getLongitude()));
                                        updateLocationPreview();
                                        Toast.makeText(
                                                        AddCompanyActivity.this,
                                                        R.string.geocoder_ok,
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    } else {
                                        Toast.makeText(
                                                        AddCompanyActivity.this,
                                                        R.string.geocoder_failed,
                                                        Toast.LENGTH_LONG)
                                                .show();
                                    }
                                });
                    } catch (IOException e) {
                        runOnUiThread(
                                () -> {
                                    btnFindLocation.setEnabled(true);
                                    Toast.makeText(
                                                    AddCompanyActivity.this,
                                                    R.string.geocoder_failed,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                });
                    }
                });
    }

    @SuppressLint("deprecation")
    private List<Address> lookupAddresses(String query) throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        return geocoder.getFromLocationName(query, 5);
    }

    private void updateLocationPreview() {
        String latStr = textOf(inputLatitude);
        String lngStr = textOf(inputLongitude);

        Double lat = parseCoord(latStr);
        Double lng = parseCoord(lngStr);

        if (lat != null && lng != null) {
            String ns = lat >= 0 ? "N" : "S";
            String ew = lng >= 0 ? "E" : "W";
            textLocationCoords.setText(
                    String.format(
                            Locale.US,
                            "%.4f° %s %.4f° %s",
                            Math.abs(lat),
                            ns,
                            Math.abs(lng),
                            ew));
        } else if (!TextUtils.isEmpty(latStr) || !TextUtils.isEmpty(lngStr)) {
            textLocationCoords.setText(R.string.location_placeholder);
        } else {
            textLocationCoords.setText(R.string.location_placeholder);
        }
    }

    @Nullable
    private static Double parseCoord(@NonNull String raw) {
        if (TextUtils.isEmpty(raw.trim())) {
            return null;
        }
        try {
            return Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String textOf(TextInputEditText editText) {
        Editable e = editText.getText();
        return e != null ? e.toString() : "";
    }

    private void attemptSave() {
        clearErrors();

        String name = textOf(findViewById(R.id.input_name));
        String address = textOf(findViewById(R.id.input_address));
        String email = textOf(findViewById(R.id.input_email));
        String phone = textOf(findViewById(R.id.input_phone));
        String website = textOf(findViewById(R.id.input_website));

        boolean ok = true;
        if (TextUtils.isEmpty(name.trim())) {
            tilName.setError(getString(R.string.error_required));
            ok = false;
        }
        if (TextUtils.isEmpty(address.trim())) {
            tilAddress.setError(getString(R.string.error_required));
            ok = false;
        }
        if (parseCoord(textOf(inputLatitude)) == null) {
            tilLatitude.setError(getString(R.string.error_required));
            ok = false;
        }
        if (parseCoord(textOf(inputLongitude)) == null) {
            tilLongitude.setError(getString(R.string.error_required));
            ok = false;
        }
        if (TextUtils.isEmpty(email.trim())) {
            tilEmail.setError(getString(R.string.error_required));
            ok = false;
        }
        if (TextUtils.isEmpty(phone.trim())) {
            tilPhone.setError(getString(R.string.error_required));
            ok = false;
        }
        if (TextUtils.isEmpty(website.trim())) {
            tilWebsite.setError(getString(R.string.error_required));
            ok = false;
        }

        int checked = chipGroupCategory.getCheckedChipId();
        if (checked == View.NO_ID) {
            ok = false;
            Toast.makeText(this, R.string.label_category, Toast.LENGTH_SHORT).show();
        }

        if (!ok) {
            return;
        }

        String categorySlug = categorySlugForChip(checked);

        Double lat = parseCoord(textOf(inputLatitude));
        Double lng = parseCoord(textOf(inputLongitude));
        if (lat == null || lng == null) {
            return;
        }

        Company company = new Company();
        company.setName(name.trim());
        company.setAddress(address.trim());
        company.setLatitude(lat);
        company.setLongitude(lng);
        company.setEmail(email.trim());
        company.setPhone(phone.trim());
        company.setWebsite(website.trim());
        company.setCategory(categorySlug);

        btnSave.setEnabled(false);
        MestoApi api = ApiClient.getApi();
        api.createCompany(company)
                .enqueue(
                        new Callback<Company>() {
                            @Override
                            public void onResponse(
                                    @NonNull Call<Company> call,
                                    @NonNull Response<Company> response) {
                                btnSave.setEnabled(true);
                                if (response.isSuccessful()) {
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(
                                                    AddCompanyActivity.this,
                                                    R.string.error_save_server,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Company> call, @NonNull Throwable t) {
                                btnSave.setEnabled(true);
                                Toast.makeText(
                                                AddCompanyActivity.this,
                                                R.string.error_network,
                                                Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
    }

    private void clearErrors() {
        tilName.setError(null);
        tilAddress.setError(null);
        tilLatitude.setError(null);
        tilLongitude.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilWebsite.setError(null);
    }

    @NonNull
    private String categorySlugForChip(int chipId) {
        if (chipId == R.id.chip_service) {
            return "service";
        }
        if (chipId == R.id.chip_entertainment) {
            return "entertainment";
        }
        if (chipId == R.id.chip_industry) {
            return "industry";
        }
        if (chipId == R.id.chip_education) {
            return "education";
        }
        return "unknown";
    }
}
