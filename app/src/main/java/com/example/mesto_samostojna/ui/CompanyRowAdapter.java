package com.example.mesto_samostojna.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mesto_samostojna.R;
import com.example.mesto_samostojna.data.Company;
import com.example.mesto_samostojna.util.GlideLogoLoader;

import java.util.ArrayList;
import java.util.List;

/** ListView adapter: ikona levo, naziv / adresa / tel / veb (baranje od zadacata). */
public class CompanyRowAdapter extends BaseAdapter {

    private final Context context;
    private final List<Company> items = new ArrayList<>();

    public CompanyRowAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<Company> companies) {
        items.clear();
        if (companies != null) {
            items.addAll(companies);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Company getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        Company c = items.get(position);
        return c.getId() != null ? c.getId() : position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row =
                    LayoutInflater.from(context)
                            .inflate(R.layout.item_company_row, parent, false);
        }
        Company c = items.get(position);
        ImageView icon = row.findViewById(R.id.row_icon);
        TextView title = row.findViewById(R.id.row_title);
        TextView address = row.findViewById(R.id.row_address);
        TextView phone = row.findViewById(R.id.row_phone);
        TextView website = row.findViewById(R.id.row_website);

        GlideLogoLoader.load(icon, c.getImageUrl(), c.getWebsite());
        title.setText(c.getName());
        address.setText(c.getAddress());
        phone.setText(c.getPhone());
        website.setText(c.getWebsite());
        return row;
    }
}
