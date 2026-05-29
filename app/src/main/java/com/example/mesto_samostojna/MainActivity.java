package com.example.mesto_samostojna;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
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
import com.example.mesto_samostojna.geofence.GeofenceManager;
import com.example.mesto_samostojna.geofence.ProximityNotifier;
import com.example.mesto_samostojna.ui.CompanyRowAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Бизнис директориум: табови, листа, пребарување, geofence нотификации при влез &lt; 50 m.
 */
public class MainActivity extends AppCompatActivity {

    public static final String[] CATEGORY_SLUGS = {
        "service", "entertainment", "industry", "education", "other"
    };

    private static final int[] TAB_LABELS = {
        R.string.cat_service,
        R.string.cat_entertainment,
        R.string.cat_industry,
        R.string.cat_education,
        R.string.cat_other
    };

    private final List<Company> companies = new ArrayList<>();
    private String searchQuery = "";

    private TextInputEditText inputSearch;
    private MaterialToolbar toolbar;
    private TabLayout tabs;
    private ViewPager2 pager;
    private ListView searchResultsList;
    private TextView searchEmptyView;
    private CompanyRowAdapter searchAdapter;

    private final ActivityResultLauncher<String[]> requestLocationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        Boolean fine = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                        Boolean coarse = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                        if (Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse)) {
                            requestBackgroundLocationIfNeeded();
                        }
                    });

    private final ActivityResultLauncher<String> requestBackgroundLocationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            registerGeofencesForLoadedCompanies();
                        }
                        requestNotificationPermissionIfNeeded();
                    });

    private final ActivityResultLauncher<String> requestNotificationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> registerGeofencesForLoadedCompanies());

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

        ProximityNotifier.ensureChannel(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_main);
        toolbar.setOnMenuItemClickListener(this::onToolbarMenuItemClick);

        tabs = findViewById(R.id.tabs);
        pager = findViewById(R.id.view_pager);
        pager.setAdapter(new CompanyPagerAdapter(this));
        new TabLayoutMediator(tabs, pager, (tab, position) -> tab.setText(TAB_LABELS[position]))
                .attach();

        searchResultsList = findViewById(R.id.list_search_results);
        searchEmptyView = findViewById(R.id.empty_search_results);
        searchAdapter = new CompanyRowAdapter(this);
        searchResultsList.setAdapter(searchAdapter);
        searchResultsList.setOnItemClickListener(
                (parent, view, position, id) -> {
                    Company c = searchAdapter.getItem(position);
                    Intent intent = new Intent(this, CompanyDetailActivity.class);
                    intent.putExtra(CompanyDetailActivity.EXTRA_COMPANY, c);
                    startActivity(intent);
                });

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
                        refreshSearchMode();
                    }
                });

        loadCompanies();
        maybeRequestPermissions();
    }

    private boolean onToolbarMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_add_company) {
            addCompanyLauncher.launch(new Intent(this, AddCompanyActivity.class));
            return true;
        }
        return false;
    }

    private void maybeRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            requestBackgroundLocationIfNeeded();
            requestNotificationPermissionIfNeeded();
            return;
        }
        requestLocationLauncher.launch(
                new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                });
    }

    private void requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            registerGeofencesForLoadedCompanies();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            registerGeofencesForLoadedCompanies();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            registerGeofencesForLoadedCompanies();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            registerGeofencesForLoadedCompanies();
            return;
        }
        requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void registerGeofencesForLoadedCompanies() {
        if (!companies.isEmpty()) {
            GeofenceManager.syncGeofences(this, new ArrayList<>(companies));
        }
    }

    public List<Company> getCompaniesForCategory(String slug) {
        List<Company> out = new ArrayList<>();
        for (Company c : companies) {
            if (c.hasCategory(slug)) {
                out.add(c);
            }
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

    private void refreshSearchMode() {
        String q = searchQuery.trim().toLowerCase(Locale.getDefault());
        boolean searching = !q.isEmpty();

        tabs.setVisibility(searching ? View.GONE : View.VISIBLE);
        pager.setVisibility(searching ? View.GONE : View.VISIBLE);

        if (!searching) {
            searchResultsList.setVisibility(View.GONE);
            searchEmptyView.setVisibility(View.GONE);
            notifyCompanyFragments();
            return;
        }

        List<Company> matches = new ArrayList<>();
        for (Company c : companies) {
            if (matchesQuery(c, q)) {
                matches.add(c);
            }
        }
        searchAdapter.setItems(matches);
        boolean any = !matches.isEmpty();
        searchResultsList.setVisibility(any ? View.VISIBLE : View.GONE);
        searchEmptyView.setVisibility(any ? View.GONE : View.VISIBLE);
    }

    private static boolean matchesQuery(Company c, String q) {
        return containsCi(c.getName(), q)
                || containsCi(c.getAddress(), q)
                || containsCi(c.getPhone(), q)
                || containsCi(c.getEmail(), q)
                || containsCi(c.getWebsite(), q);
    }

    private static boolean containsCi(String value, String q) {
        return value != null && value.toLowerCase(Locale.getDefault()).contains(q);
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
                                    GeofenceManager.syncGeofences(
                                            MainActivity.this, new ArrayList<>(companies));
                                } else {
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    R.string.error_network,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                                notifyCompanyFragments();
                                refreshSearchMode();
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
                                refreshSearchMode();
                            }
                        });
    }
}
