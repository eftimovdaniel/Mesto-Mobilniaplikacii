package com.example.mesto_samostojna;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.mesto_samostojna.api.ApiClient;
import com.example.mesto_samostojna.api.MestoApi;
import com.example.mesto_samostojna.data.Company;
import com.example.mesto_samostojna.ui.CompanyRowAdapter;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Eden tab: ListView so kompanii za izbranata kategorija + filter od MainActivity. */
public class CompanyListFragment extends Fragment {

    private static final String ARG_SLUG = "slug";

    private String categorySlug;
    private CompanyRowAdapter adapter;

    public static CompanyListFragment newInstance(String categorySlug) {
        CompanyListFragment f = new CompanyListFragment();
        Bundle b = new Bundle();
        b.putString(ARG_SLUG, categorySlug);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        categorySlug = args != null ? args.getString(ARG_SLUG) : "";
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_company_list, container, false);
        ListView list = v.findViewById(R.id.list_companies);
        TextView empty = v.findViewById(R.id.fragment_empty);
        list.setEmptyView(empty);
        adapter = new CompanyRowAdapter(requireContext());
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Company c = adapter.getItem(position);
            if (c == null) {
                return;
            }
            Intent intent = new Intent(requireContext(), CompanyDetailActivity.class);
            intent.putExtra(CompanyDetailActivity.EXTRA_COMPANY, c);
            startActivity(intent);
        });
        adapter.setOnDeleteClickListener(this::confirmAndDelete);
        return v;
    }

    /** Pokazuva AlertDialog za potvrda, pa po "Izbrisi" praka DELETE kon API. */
    private void confirmAndDelete(Company company) {
        if (company == null || company.getId() == null) {
            return;
        }
        String name = company.getName() != null ? company.getName() : "";
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_dialog_title)
                .setMessage(getString(R.string.delete_dialog_message, name))
                .setNegativeButton(R.string.delete_cancel, null)
                .setPositiveButton(R.string.delete_confirm, (d, w) -> performDelete(company))
                .show();
    }

    private void performDelete(Company company) {
        Integer id = company.getId();
        if (id == null) {
            return;
        }
        MestoApi api = ApiClient.getApi();
        api.deleteCompany(id)
                .enqueue(
                        new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(
                                    @NonNull Call<ResponseBody> call,
                                    @NonNull Response<ResponseBody> response) {
                                if (!isAdded()) {
                                    return;
                                }
                                if (response.isSuccessful()) {
                                    Toast.makeText(
                                                    requireContext(),
                                                    getString(
                                                            R.string.delete_success,
                                                            company.getName()),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                    if (requireActivity() instanceof MainActivity) {
                                        ((MainActivity) requireActivity()).reloadCompanies();
                                    }
                                } else {
                                    Toast.makeText(
                                                    requireContext(),
                                                    R.string.delete_failed,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                                if (!isAdded()) {
                                    return;
                                }
                                Toast.makeText(
                                                requireContext(),
                                                R.string.error_network,
                                                Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFromActivity();
    }

    /** Povik od MainActivity po novo vchituvanje ili promena na prebaruvanje. */
    public void onCompaniesUpdated() {
        refreshFromActivity();
    }

    private void refreshFromActivity() {
        if (!(requireActivity() instanceof MainActivity)) {
            return;
        }
        MainActivity host = (MainActivity) requireActivity();
        adapter.setItems(host.getCompaniesForCategory(categorySlug));
    }
}
