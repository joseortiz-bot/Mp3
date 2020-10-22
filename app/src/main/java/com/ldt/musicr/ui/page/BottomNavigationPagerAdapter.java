package com.ldt.musicr.ui.page;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.ldt.musicr.R;
import com.ldt.musicr.ui.page.featurepage.FeatureTabFragment;
import com.ldt.musicr.ui.page.librarypage.LibraryTabFragment;
import com.ldt.musicr.ui.page.settingpage.SettingTabFragment;
import com.ldt.musicr.ui.widget.navigate.NavigateFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class BottomNavigationPagerAdapter extends FragmentPagerAdapter {
    private Context mContext;

    public BottomNavigationPagerAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mContext = context;
        initData();
    }

    public ArrayList<NavigateFragment> mData = new ArrayList<>();

    public boolean onBackPressed(int position) {
        if(position<mData.size())
        return mData.get(position).onBackPressed();
        return false;
    }


    private void initData() {
        mData.add(NavigateFragment.newInstance(new FeatureTabFragment()));
        mData.add(NavigateFragment.newInstance(new LibraryTabFragment()));
        mData.add(NavigateFragment.newInstance(new SettingTabFragment()));
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return mData.size();
    }

    // Returns the fragment to display for that page
    @NonNull
    @Override
    public Fragment getItem(int position) {
        if(position>=mData.size()) return mData.get(0);
        return mData.get(position);
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0: return mContext.getResources().getString(R.string.feature);
            case 1: return mContext.getResources().getString(R.string.library);
            case 2: return mContext.getResources().getString(R.string.settings);
            default:return "";
        }
    }
}
