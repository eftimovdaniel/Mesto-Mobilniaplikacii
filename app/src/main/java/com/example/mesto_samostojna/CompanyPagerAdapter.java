package com.example.mesto_samostojna;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * Adapter za ViewPager2.
 *
 * ZOSO FragmentStateAdapter: ViewPager2 go cuva/unistuva fragmentot pri swipe.
 * Sekoja pozicija = eden category slug od MainActivity.CATEGORY_SLUGS.
 */
public class CompanyPagerAdapter extends FragmentStateAdapter {

    public CompanyPagerAdapter(FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Sekoja pozicija = edna kategorija od MainActivity.CATEGORY_SLUGS
        return CompanyListFragment.newInstance(MainActivity.CATEGORY_SLUGS[position]);
    }

    @Override
    public int getItemCount() {
        return MainActivity.CATEGORY_SLUGS.length; // 5 tabovi
    }
}
