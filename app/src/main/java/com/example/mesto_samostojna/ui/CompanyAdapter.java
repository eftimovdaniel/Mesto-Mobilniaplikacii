package com.example.mesto_samostojna.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mesto_samostojna.R;
import com.example.mesto_samostojna.data.Company;

import java.util.ArrayList;
import java.util.List;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.VH> {

    private final List<Company> items = new ArrayList<>();

    public void setItems(List<Company> companies) {
        items.clear();
        if (companies != null) {
            items.addAll(companies);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_company, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class VH extends RecyclerView.ViewHolder {

        private final TextView title;
        private final TextView subtitle;
        private final TextView category;

        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_company_title);
            subtitle = itemView.findViewById(R.id.item_company_address);
            category = itemView.findViewById(R.id.item_company_category);
        }

        void bind(Company c) {
            title.setText(c.getName());
            subtitle.setText(c.getAddress());
            category.setText(c.getCategory());
        }
    }
}
