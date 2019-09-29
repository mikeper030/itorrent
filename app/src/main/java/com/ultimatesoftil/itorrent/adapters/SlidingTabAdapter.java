package com.ultimatesoftil.itorrent.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

/**
 * Created by crothhass on 16.09.2015.
 */
public class SlidingTabAdapter extends FragmentPagerAdapter {

    private ArrayList<Fragment> fragments;
    private ArrayList<String> pageTitles;

    public SlidingTabAdapter(FragmentManager fm, ArrayList<Fragment> fragments, ArrayList<String> pageTitles) {
        super(fm);
        this.fragments = fragments;
        this.pageTitles = pageTitles;
    }

    /**
     * @return the number of pages to display
     */
    @Override
    public int getCount() {
        return fragments.size();
    }



    /**
     * Return the title of the item at {@code position}. This is important as what this method
     * returns is what is displayed in the {@link SlidingTabLayout}.
     * <p/>
     * Here we construct one using the position value, but for real application the title should
     * refer to the item's contents.
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return pageTitles.get(position);
    }

    @Override
    public Fragment getItem(int i) {
        return fragments.get(i);
    }



    /**
     * Destroy the item from the {@link ViewPager}. In our case this is simply removing the
     * {@link View}.
     */
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
        fragments.remove(position);
    }

    public int addView(Fragment v, String title) {
        return this.addView(v, title, -1);
    }

    public int addView(Fragment v, String title, int position) {
        if (position == -1) {
            position = fragments.size();
        }
        fragments.add(position, v);
        pageTitles.add(position, title);
        return position;
    }

    public int removeView() {
        return this.removeView(-1);
    }

    public int removeView(int position) {
        if (position == -1) {
            position = fragments.size() - 1;
        }
        if (position > 0) {
            fragments.remove(position);
            pageTitles.remove(position);
        }
        return position;
    }

}
