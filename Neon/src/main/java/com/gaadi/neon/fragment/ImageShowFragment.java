package com.gaadi.neon.fragment;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gaadi.neon.adapter.ImageShowAdapter;
import com.gaadi.neon.util.Constants;
import com.gaadi.neon.util.FileInfo;
import com.gaadi.neon.util.SingletonClass;
import com.scanlibrary.R;
import com.scanlibrary.databinding.ImageShowLayoutBinding;

import java.util.ArrayList;

/**
 * @author princebatra
 * @version 1.0
 * @since 2/2/17
 */
public class ImageShowFragment extends Fragment {

    ImageShowAdapter adapter;
    ImageShowLayoutBinding binder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binder = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.image_show_layout, null, false);
        binder.btnDone.setOnClickListener(doneListener);
        return binder.getRoot();
    }

    View.OnClickListener doneListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (validate()) {
                if(SingletonClass.getSingleonInstance().getGenericParam().getTagEnabled()) {
                    SingletonClass.getSingleonInstance().getImageResultListener().imageCollection(SingletonClass.getSingleonInstance().getFileHashMap());
                }else{
                    SingletonClass.getSingleonInstance().getImageResultListener().imageCollection(SingletonClass.getSingleonInstance().getImagesCollection());
                }
                SingletonClass.getSingleonInstance().scheduleSinletonClearance();
                getActivity().finish();
            }
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        if(SingletonClass.getSingleonInstance().getImagesCollection() == null ||
                SingletonClass.getSingleonInstance().getImagesCollection().size()<=0){
            return;
        }
        if(adapter == null){
            adapter = new ImageShowAdapter(getActivity());
            binder.imageShowGrid.setAdapter(adapter);
        }else{
            adapter.notifyDataSetChanged();
        }
    }

    private boolean validate() {
        if (!SingletonClass.getSingleonInstance().getGenericParam().getTagEnabled()) {
            return true;
        }
        ArrayList<FileInfo> fileInfos = SingletonClass.getSingleonInstance().getImagesCollection();
        if (fileInfos != null && fileInfos.size() > 0) {
            for (int i = 0; i < fileInfos.size(); i++) {
                if(fileInfos.get(i).getFileTag() == null){
                    Toast.makeText(getActivity(),"Set tag for all images",Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }


}
