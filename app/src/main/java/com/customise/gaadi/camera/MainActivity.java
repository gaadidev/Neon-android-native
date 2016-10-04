package com.customise.gaadi.camera;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.gaadi.neon.PhotosLibrary;
import com.gaadi.neon.util.FileInfo;
import com.gaadi.neon.util.NeonConstants;
import com.gaadi.neon.util.PhotoParams;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    private static final String TAG = "MainActivity";
    public static final int REQUEST_CODE = 2004;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.bClickMe).setOnClickListener(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: " + requestCode + " responseCode : " + resultCode);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                ArrayList<FileInfo> images = (ArrayList<FileInfo>) data.getSerializableExtra(NeonConstants.COLLECTED_IMAGES);
                Log.i(TAG,"Selected images info "+ images.size());
            }
        }
    }

    @Override
    public void onClick(View v)
    {
        switch(v.getId()){
            case R.id.bClickMe:
                PhotoParams params = new PhotoParams();
                params.setMode(PhotoParams.MODE.NEUTRAL);
                params.setCameraFace(PhotoParams.CameraFacing.FRONT);
                //        params.setMode(PhotoParams.MODE.NEUTRAL);
                //        params.setOrientation(PhotoParams.CameraOrientation.PORTRAIT);
                //        params.setNoOfPhotos(2);
                //        params.setEnableCapturedReview(true);
                //        params.setEnableExtraBrightness(false);
                //        params.setRestrictedExtensionEnabled(true);
                //        params.setCameraFace(PhotoParams.CameraFacing.BACK);
                //        params.setGalleryFromCameraEnabled(false);
                PhotosLibrary.collectPhotos(this, params, REQUEST_CODE);
                break;
        }
    }
}
