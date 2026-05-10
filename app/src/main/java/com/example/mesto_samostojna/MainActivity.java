package com.example.mesto_samostojna;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mesto_samostojna.api.ApiClient;
import com.example.mesto_samostojna.api.MestoApi;
import com.example.mesto_samostojna.data.Company;
import com.example.mesto_samostojna.ui.CompanyAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private CompanyAdapter adapter;
    private TextView emptyView;
    private RecyclerView recyclerView;

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

        recyclerView = findViewById(R.id.recycler_companies);
        emptyView = findViewById(R.id.main_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CompanyAdapter();
        recyclerView.setAdapter(adapter);

        MaterialButton openAdd = findViewById(R.id.btn_open_add_company);
        openAdd.setOnClickListener(
                v -> addCompanyLauncher.launch(new Intent(this, AddCompanyActivity.class)));

        loadCompanies();
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
                                if (response.isSuccessful() && response.body() != null) {
                                    List<Company> list = response.body();
                                    adapter.setItems(list);
                                    boolean empty = list.isEmpty();
                                    emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
                                    recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                                } else {
                                    Toast.makeText(
                                                    MainActivity.this,
                                                    R.string.error_network,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                    emptyView.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<List<Company>> call, @NonNull Throwable t) {
                                Toast.makeText(
                                                MainActivity.this,
                                                R.string.error_network,
                                                Toast.LENGTH_LONG)
                                        .show();
                                emptyView.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            }
                        });
    }
}
