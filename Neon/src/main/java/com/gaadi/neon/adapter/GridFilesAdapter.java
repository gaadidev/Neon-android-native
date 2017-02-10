package com.gaadi.neon.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.gaadi.neon.util.FileInfo;
import com.gaadi.neon.util.NeonImagesHandler;
import com.scanlibrary.R;

import java.util.ArrayList;

/**
 * @author princebatra
 * @version 1.0
 * @since 2/2/17
 */
public class GridFilesAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<FileInfo>fileInfos;

    public GridFilesAdapter(Context _context, ArrayList<FileInfo> _fileInfos){
        context = _context;
        fileInfos = _fileInfos;
    }

    @Override
    public int getCount() {
        return fileInfos.size();
    }

    @Override
    public Object getItem(int position) {
        return fileInfos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        FilesHolder filesHolder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.select_files, null);

            filesHolder = new FilesHolder();
            filesHolder.selectedImage = (ImageView) convertView.findViewById(R.id.imageSelected);
            filesHolder.selection_view = (ImageView) convertView.findViewById(R.id.selection_view);
            filesHolder.transparentLayer = (ImageView) convertView.findViewById(R.id.vTransparentLayer);
            convertView.setTag(filesHolder);
        }else{
            Log.e("tag","came");
        }
        filesHolder = (FilesHolder) convertView.getTag();

        Glide.with(context).load(fileInfos.get(position).getFilePath())
                .placeholder(R.drawable.default_placeholder)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(filesHolder.selectedImage);

        if(NeonImagesHandler.getSingleonInstance().checkImageAvailableForPath(fileInfos.get(position))){
            filesHolder.selection_view.setVisibility(View.VISIBLE);
            filesHolder.transparentLayer.setVisibility(View.VISIBLE);
        }else{
            filesHolder.selection_view.setVisibility(View.GONE);
            filesHolder.transparentLayer.setVisibility(View.GONE);
        }
        final FilesHolder finalFilesHolder = filesHolder;
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(NeonImagesHandler.getSingleonInstance().checkImageAvailableForPath(fileInfos.get(position))){
                    if(NeonImagesHandler.getSingleonInstance().removeFromCollection(fileInfos.get(position))){
                        finalFilesHolder.selection_view.setVisibility(View.GONE);
                        finalFilesHolder.transparentLayer.setVisibility(View.GONE);
                    }
                }else{
                    if(NeonImagesHandler.getSingleonInstance().putInImageCollection(fileInfos.get(position),context)) {
                        finalFilesHolder.selection_view.setVisibility(View.VISIBLE);
                        finalFilesHolder.transparentLayer.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
        return convertView;
    }

    private class FilesHolder {
        ImageView selectedImage;
        ImageView transparentLayer;
        ImageView selection_view;
    }
}
