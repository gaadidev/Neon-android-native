package com.gaadi.neon.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.gaadi.neon.fragment.ImageReviewViewPagerFragment;
import com.gaadi.neon.util.FileInfo;
import com.gaadi.neon.util.SingletonClass;

import java.util.ArrayList;

/**
 * @author dipanshugarg
 * @version 1.0
 * @since 25/1/17
 */
public class ImagesReviewViewPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<ImageReviewViewPagerFragment> fragmentList;
    private FragmentManager mFragmentManager;

    public ImagesReviewViewPagerAdapter(FragmentManager fm) {
        super(fm);
        mFragmentManager=fm;
        updatePagerItems();
    }

    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    public int getItemPosition(Object item) {
        ImageReviewViewPagerFragment fragment = (ImageReviewViewPagerFragment)item;

        int position = fragmentList.indexOf(fragment);

        if (position >= 0) {
            return position;
        } else {
            return POSITION_NONE;
        }
    }

    private void updatePagerItems(){

        if(fragmentList!=null)
            fragmentList.clear();
        else
            fragmentList=new ArrayList<>();
        for (int i = 0; i < SingletonClass.getSingleonInstance().getImagesCollection().size(); i++) {
            fragmentList.add(ImageReviewViewPagerFragment.create(i, SingletonClass.getSingleonInstance().getImagesCollection().get(i)));
        }
    }

    public void setPagerItems() {
        if (fragmentList != null)
            for (int i = 0; i < SingletonClass.getSingleonInstance().getImagesCollection().size(); i++) {
                mFragmentManager.beginTransaction().remove(fragmentList.get(i)).commit();
            }
        updatePagerItems();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }
}
