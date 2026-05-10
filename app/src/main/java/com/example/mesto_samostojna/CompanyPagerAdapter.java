package com.example.mesto_samostojna;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class CompanyPagerAdapter extends FragmentStateAdapter {

    public CompanyPagerAdapter(FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return CompanyListFragment.newInstance(MainActivity.CATEGORY_SLUGS[position]);
    }

    @Override
    public int getItemCount() {
        return MainActivity.CATEGORY_SLUGS.length;
    }
}
