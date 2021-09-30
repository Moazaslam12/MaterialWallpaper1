package com.app.materialwallpaper.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.duolingo.open.rtlviewpager.RtlViewPager;
import com.app.materialwallpaper.Config;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.utils.CustomTabLayout;
import com.app.materialwallpaper.utils.SharedPref;

import static com.app.materialwallpaper.utils.Constant.FILTER_LIVE;
import static com.app.materialwallpaper.utils.Constant.FILTER_WALLPAPER;
import static com.app.materialwallpaper.utils.Constant.ORDER_FEATURED;
import static com.app.materialwallpaper.utils.Constant.ORDER_LIVE;
import static com.app.materialwallpaper.utils.Constant.ORDER_POPULAR;
import static com.app.materialwallpaper.utils.Constant.ORDER_RANDOM;
import static com.app.materialwallpaper.utils.Constant.ORDER_RECENT;

public class FragmentTabLayout extends Fragment {

    public RelativeLayout tab_background;
    public CustomTabLayout smartTabLayout;
    public ViewPager viewPager;
    public RtlViewPager viewPagerRTL;
    public int tab_count = 5;
    SharedPref sharedPref;
    View view;

    public FragmentTabLayout() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (Config.ENABLE_RTL_MODE) {
            view = inflater.inflate(R.layout.fragment_tab_layout_rtl, container, false);
        } else {
            view = inflater.inflate(R.layout.fragment_tab_layout, container, false);
        }
        sharedPref = new SharedPref(getActivity());
        tab_background = view.findViewById(R.id.tab_background);
        smartTabLayout = view.findViewById(R.id.tabs);
        if (sharedPref.getIsDarkTheme()) {
            tab_background.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            smartTabLayout.setSelectedIndicatorColors(getResources().getColor(R.color.colorAccentDark));
        } else {
            tab_background.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
        initViewPager();
        return view;

    }

    public void initViewPager() {
        if (Config.ENABLE_RTL_MODE) {
            viewPagerRTL = view.findViewById(R.id.view_pager_rtl);
            viewPagerRTL.setOffscreenPageLimit(tab_count);
            viewPagerRTL.setAdapter(new ViewPagerAdapter(getChildFragmentManager(), tab_count));
            smartTabLayout.post(() -> smartTabLayout.setViewPager(viewPagerRTL));

        } else {
            viewPager = view.findViewById(R.id.view_pager);
            viewPager.setOffscreenPageLimit(tab_count);
            viewPager.setAdapter(new ViewPagerAdapter(getChildFragmentManager(), tab_count));
            smartTabLayout.post(() -> smartTabLayout.setViewPager(viewPager));
        }
    }

    public class ViewPagerAdapter extends FragmentStatePagerAdapter {

        int noOfItems;

        public ViewPagerAdapter(FragmentManager fm, int noOfItems) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.noOfItems = noOfItems;
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return FragmentWallpaper.newInstance(ORDER_RECENT, FILTER_WALLPAPER);
            } else if (position == 1) {
                return FragmentWallpaper.newInstance(ORDER_FEATURED, FILTER_WALLPAPER);
            } else if (position == 2) {
                return FragmentWallpaper.newInstance(ORDER_POPULAR, FILTER_WALLPAPER);
            } else if (position == 3) {
                return FragmentWallpaper.newInstance(ORDER_RANDOM, FILTER_WALLPAPER);
            } else {
                return FragmentWallpaper.newInstance(ORDER_LIVE, FILTER_LIVE);
            }
        }

        @Override
        public int getCount() {
            return noOfItems;
        }

        @Override
        public String getPageTitle(int position) {
            if (position == 0) {
                return getResources().getString(R.string.menu_recent);
            } else if (position == 1) {
                return getResources().getString(R.string.menu_featured);
            } else if (position == 2) {
                return getResources().getString(R.string.menu_popular);
            } else if (position == 3) {
                return getResources().getString(R.string.menu_random);
            } else {
                return getResources().getString(R.string.menu_live);
            }
        }
    }

}

