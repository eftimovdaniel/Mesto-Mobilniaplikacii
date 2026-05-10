package com.example.mesto_samostojna;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mesto_samostojna.api.ApiClient;
import com.example.mesto_samostojna.api.MestoApi;
import com.example.mesto_samostojna.data.Company;
import com.example.mesto_samostojna.util.GeoUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Biznis direktorium: TabLayout + ViewPager (swipe), lista so ListView vo fragment, prebaruvanje po
 * naziv vo ramki na tekoven tab, toolbar ikona za nova kompanija, oddalečen server + Toast pri blizina pod 50 m.
 */
public class MainActivity extends AppCompatActivity {

    public static final String[] CATEGORY_SLUGS = {
        "service", "entertainment", "industry", "education"
    };

    private static final int[] TAB_LABELS = {
        R.string.cat_service,
        R.string.cat_entertainment,
        R.string.cat_industry,
        R.string.cat_education
    };

    private static final double PROXIMITY_RADIUS_M = 50.0;
    private static final long PROXIMITY_TOAST_COOLDOWN_MS = 90_000L;

    private final List<Company> companies = new ArrayList<>();
    private String searchQuery = "";

    private final ConcurrentHashMap<Integer, Long> proximityLastToastAt = new ConcurrentHashMap<>();

    private TextInputEditText inputSearch;
    private MaterialToolbar toolbar;

    private FusedLocationProviderClient fusedClient;
    private LocationRequest locationRequest;
    private final LocationCallback locationCallback =
            new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location loc = locationResult.getLastLocation();
                    if (loc != null) {
                        checkProximity(loc);
                    }
                }
            };

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                        if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                            startLocationUpdates();
                        }
                    });

    private final ActivityResultLauncher<Intent> addCompanyLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadCompanies();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest =
                new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 12_000L)
                        .setMinUpdateIntervalMillis(8_000L)
                        .build();

        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(this::onToolbarMenuItemClick);

        TabLayout tabs = findViewById(R.id.tabs);
        ViewPager2 pager = findViewById(R.id.view_pager);
        pager.setAdapter(new CompanyPagerAdapter(this));
        new TabLayoutMediator(tabs, pager, (tab, position) -> tab.setText(TAB_LABELS[position]))
                .attach();

        inputSearch = findViewById(R.id.input_search);
        inputSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        searchQuery = s != null ? s.toString() : "";
                        notifyCompanyFragments();
                    }
                });

        loadCompanies();
        maybeRequestLocationAndStart();
    }

    private boolean onToolbarMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_add_company) {
            addCompanyLauncher.launch(new Intent(this, AddCompanyActivity.class));
            return true;
        }
        return false;
    }

    private void maybeRequestLocationAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
            return;
        }
        requestPermissionLauncher.launch(
                new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedClient.removeLocationUpdates(locationCallback);
        fusedClient.requestLocationUpdates(
                locationRequest, locationCallback, getMainLooper());
    }

    private void checkProximity(@NonNull Location userLoc) {
        if (companies.isEmpty()) {
            return;
        }
        double uLat = userLoc.getLatitude();
        double uLon = userLoc.getLongitude();
        long now = System.currentTimeMillis();
        for (Company c : companies) {
            if (c.getId() == null) {
                continue;
            }
            double d =
                    GeoUtils.distanceMeters(
                            uLat, uLon, c.getLatitude(), c.getLongitude());
            if (d >= PROXIMITY_RADIUS_M) {
                continue;
            }
            int id = c.getId();
            long last = proximityLastToastAt.getOrDefault(id, 0L);
            if (now - last < PROXIMITY_TOAST_COOLDOWN_MS) {
                continue;
            }
            proximityLastToastAt.put(id, now);
            Toast.makeText(
                            this,
                            getString(R.string.proximity_near, c.getName()),
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    public List<Company> getCompaniesForCategory(String slug) {
        List<Company> out = new ArrayList<>();
        String q = searchQuery.trim().toLowerCase(Locale.getDefault());
        for (Company c : companies) {
            if (!c.hasCategory(slug)) {
                continue;
            }
            if (!q.isEmpty()
                    && !c.getName().toLowerCase(Locale.getDefault()).contains(q)) {
                continue;
            }
            out.add(c);
        }
        return out;
    }

    private void notifyCompanyFragments() {
        for (Fragment f : getSupportFragmentManager().getFragments()) {
            if (f instanceof CompanyListFragment) {
                ((CompanyListFragment) f).onCompaniesUpdated();
            }
        }
    }

    private void loadCompanies() {
        MestoApi api = ApiClient.getApi();
        api.listCompanies()
                .enqueue(
                        new Callback<List<Company>>() {
                            @Override
                            public void onResponse(
                                    @NonNull Call<List<Company>> call,
                                    @NonNull Response<List<Company>> response) {
                                companies.clear();
                                if (response.isSuccessful() && response.body() != null) {
                                    companies.addAll(response.body());
                                } else {
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    R.string.error_network,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                                notifyCompanyFragments();
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<List<Company>> call, @NonNull Throwable t) {
                                companies.clear();
                                Toast.makeText(
                                                MainActivity.this,
                                                R.string.error_network,
                                                Toast.LENGTH_LONG)
                                        .show();
                                notifyCompanyFragments();
                            }
                        });
    }

    @Override
    protected void onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }
}
