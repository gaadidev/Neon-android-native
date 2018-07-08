package com.gaadi.neon.fragment;

/**
 * @author lakshaygirdhar
 * @version 1.0
 * @since 19/10/16
 */

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.gaadi.neon.activity.ImageReviewActivity;
import com.gaadi.neon.adapter.FlashModeRecyclerHorizontalAdapter;
import com.gaadi.neon.enumerations.CameraFacing;
import com.gaadi.neon.enumerations.CameraOrientation;
import com.gaadi.neon.interfaces.ICameraParam;
import com.gaadi.neon.model.ImageTagModel;
import com.gaadi.neon.util.CameraPreview;
import com.gaadi.neon.util.Constants;
import com.gaadi.neon.util.DrawingView;
import com.gaadi.neon.util.FileInfo;
import com.gaadi.neon.util.FindLocations;
import com.gaadi.neon.util.NeonImagesHandler;
import com.gaadi.neon.util.NeonUtils;
import com.gaadi.neon.util.PrefsUtils;
import com.scanlibrary.R;
import com.scanlibrary.databinding.NeonCamera2FragmentLayoutBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation,unchecked")
public class Camera2Fragment extends Fragment implements View.OnTouchListener, Camera.PictureCallback {

    private static final String TAG = "Camera2Fragment";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_REVIEW = 100;
    private static final int SHAKE_THRESHOLD = 20;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public NeonCamera2FragmentLayoutBinding binder;
    public Camera mCamera;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private String cameraId;
    private Size imageDimension;
    private TextureView textureView;
    private HandlerThread mBackgroundThread;
    private DrawingView drawingView;
    private ImageView currentFlashMode;
    private ArrayList<String> supportedFlashModes;
    private RecyclerView rcvFlash;
    private CameraPreview mCameraPreview;
    private boolean readyToTakePicture;
    private FrameLayout mCameraLayout;
    // private View fragmentView;
    private ICameraParam cameraParam;
    private SetOnPictureTaken mPictureTakenListener;
    private Handler mBackgroundHandler;
    private boolean permissionAlreadyRequested;
    private Activity mActivity;
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
//            createSupportedFlashList();
            createCameraPreview();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };
    private boolean useFrontFacingCamera;
    //private boolean enableCapturedReview;
    private float mDist;
    private ImageView mSwitchCamera;
    private CameraFacing cameraFacing;
    private CameraFacing localCameraFacing;
    private SensorManager sensorManager;
    private boolean locationRestrictive = true;
    private float[] mGravity;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = event.values.clone();
                // Shake detection
                float x = mGravity[0];
                float y = mGravity[1];
                float z = mGravity[2];

                long curTime = System.currentTimeMillis();

                if ((curTime - lastUpdate) > 100) {
                    long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;

                    float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                    if (speed > SHAKE_THRESHOLD && mCamera != null && mCamera.getParameters() != null) {
                        handleFocus(null, mCamera.getParameters());
                    }
                    Log.e("tag", String.valueOf(speed));
                    last_x = x;
                    last_y = y;
                    last_z = z;
                }

            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    private boolean fromCreate;

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Camera2Fragment getInstance(boolean locationRestrictive) {

        Camera2Fragment fragment = new Camera2Fragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("locationRestrictive", locationRestrictive);
        fragment.setArguments(bundle);
        return fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera() {
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            if (cameraId == null) {
                cameraId = manager.getCameraIdList()[0];
            }
            if (cameraCaptureSessions != null && cameraDevice != null) {
                cameraDevice.close();
                cameraCaptureSessions.close();
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            createSupportedFlashList(characteristics);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera " + cameraId);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture != null) {
                texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
//            Log.d(TAG, "width: " + imageDimension.getWidth() + ", height: " + imageDimension.getHeight());
                Surface surface = new Surface(texture);
                captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                captureRequestBuilder.addTarget(surface);
                cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        //The camera is already closed
                        if (null == cameraDevice) {
                            return;
                        }
                        // When the session is ready, we start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession;
                        try{
                            updatePreview();
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        //Toast.makeText(mActivity, "Configuration change", Toast.LENGTH_SHORT).show();
                    }
                }, null);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {

            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onResume() {
        super.onResume();
        /*if (!fromCreate) {
            try {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Camera2Fragment fragment = new Camera2Fragment();
                            FragmentManager manager = getActivity().getSupportFragmentManager();
                            manager.beginTransaction().replace(R.id.content_frame, fragment).commit();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
        mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            Log.e(TAG, "opening camera from onResume()");
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        if (cameraDevice != null) {
            cameraDevice.close();
        }
        stopBackgroundThread();
        super.onPause();
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    protected void takePicture() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    new Camera2Fragment.ImagePostProcessing(mActivity, bytes).execute();
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /*public void clickPicture() {
        if (readyToTakePicture) {
            if (mCamera != null) {
                mCamera.takePicture(null, null, this);
            }
            readyToTakePicture = false;
        }
    }*/
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void clickPicture() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory() + "/pic.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    new Camera2Fragment.ImagePostProcessing(mActivity, bytes).execute();
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mPictureTakenListener = (SetOnPictureTaken) activity;
    }

    public void startPreview() {
        mCamera.startPreview();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binder = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.neon_camera2_fragment_layout, container, false);
        localCameraFacing = NeonImagesHandler.getSingleonInstance().getCameraParam().getCameraFacing();
        mActivity = getActivity();
        cameraParam = NeonImagesHandler.getSingleonInstance().getCameraParam();
        if (cameraParam != null) {
            initialize();
            customize();
        } else {
            Toast.makeText(getContext(), getString(R.string.pass_params), Toast.LENGTH_SHORT).show();
        }
        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        fromCreate = true;
        return binder.getRoot();
    }

    private void initialize() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.HORIZONTAL, false);

        textureView = binder.texture;
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        currentFlashMode = binder.currentFlashMode;
        rcvFlash = binder.flashListview;
        mSwitchCamera = binder.switchCamera;

        rcvFlash.setLayoutManager(layoutManager);

        //View to add rectangle on tap to focus
        drawingView = new DrawingView(mActivity);

        binder.setHandlers(this);

        binder.getRoot().setOnTouchListener(this);

        if (getArguments() != null)
            locationRestrictive = getArguments().getBoolean("locationRestrictive", true);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onClickFragmentsView(View v) {
        Log.d("", "onClickFragmentsView");
        if (v.getId() == R.id.buttonCaptureVertical || v.getId() == R.id.buttonCaptureHorizontal) {
            if (!locationRestrictive || FindLocations.getInstance().checkPermissions(mActivity) &&
                    FindLocations.getInstance().getLocation() != null) {
                clickPicture();
//                takePicture();
            } else {
                Toast.makeText(getActivity(), "Failed to get location.Please try again later.", Toast.LENGTH_SHORT).show();
            }

        } else if (v.getId() == R.id.switchCamera) {
            CameraManager cameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
            try {
                String[] cameraIdList = cameraManager.getCameraIdList();
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                int desiredCameraFacing = -1;
                if (CameraCharacteristics.LENS_FACING_BACK == characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    desiredCameraFacing = CameraCharacteristics.LENS_FACING_FRONT;
                } else if (CameraCharacteristics.LENS_FACING_FRONT == characteristics.get(CameraCharacteristics.LENS_FACING)) {
                    desiredCameraFacing = CameraCharacteristics.LENS_FACING_BACK;
                }
                for (String id : cameraIdList) {
                    characteristics = cameraManager.getCameraCharacteristics(id);
                    if (desiredCameraFacing == characteristics.get(CameraCharacteristics.LENS_FACING)) {
                        cameraId = id;
                    }
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            openCamera();
        } else if (v.getId() == R.id.currentFlashMode) {
            if (rcvFlash.getVisibility() == View.GONE)
                createFlashModesDropDown();
            else
                rcvFlash.setVisibility(View.GONE);
        }
    }

    private void customize() {
        CameraOrientation orientation = cameraParam.getCameraOrientation();
        cameraFacing = cameraParam.getCameraFacing();
        setOrientation(mActivity, orientation);

        if (!cameraParam.getFlashEnabled()) {
            binder.llFlash.setVisibility(View.INVISIBLE);
        }
        // enableCapturedReview = mPhotoParams.isEnableCapturedReview();

        if (cameraParam.getCameraSwitchingEnabled()) {
            if (NeonUtils.isFrontCameraAvailable() != Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mSwitchCamera.setVisibility(View.GONE);
                useFrontFacingCamera = false;
            }
        } else {
            mSwitchCamera.setVisibility(View.GONE);
            useFrontFacingCamera = false;
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ViewGroup.LayoutParams layoutParamsDrawing
                = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT);

        getActivity().addContentView(drawingView, layoutParamsDrawing);
    }

    private void setFlashLayoutAndMode() {
        String flashMode = PrefsUtils.getStringSharedPreference(getActivity(), Constants.FLASH_MODE, "");
        if (flashMode.equals("")) {
            currentFlashMode.setImageResource(R.drawable.flash_off);
        } else {
            if (supportedFlashModes != null && supportedFlashModes.size() > 0) {
                if (supportedFlashModes.contains(flashMode)) {
                    setFlash(flashMode);
                } else {
                    setFlash(supportedFlashModes.get(0));
                }
            }
        }
    }

    public void setFlash(String mode) {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(mode);

        if ("off".equals(mode)) {
            currentFlashMode.setImageResource(R.drawable.flash_off);
        } else if ("on".equals(mode)) {
            currentFlashMode.setImageResource(R.drawable.flash_on);
        } else if ("auto".equals(mode)) {
            currentFlashMode.setImageResource(R.drawable.flash_auto);
        } else if ("red-eye".equals(mode)) {
            currentFlashMode.setImageResource(R.drawable.flash_red_eye);
        } else if ("torch".equals(mode)) {
            currentFlashMode.setImageResource(R.drawable.flash_torch);
        } else {
            currentFlashMode.setImageResource(R.drawable.flash_off);
        }
        PrefsUtils.setStringSharedPreference(getActivity(), Constants.FLASH_MODE, mode);
        mCamera.setParameters(parameters);
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        if (!fromCreate) {
//            try {
//                new Handler().post(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Camera2Fragment fragment = new Camera2Fragment();
//                            FragmentManager manager = getActivity().getSupportFragmentManager();
//                            manager.beginTransaction().replace(R.id.content_frame, fragment).commit();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });
//                return;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//
//        fromCreate = false;
//        if (mCamera == null) {
//            try {
//                if (cameraFacing == CameraFacing.front && NeonUtils.isFrontCameraAvailable() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                    Log.d(TAG, "onResume: open front");
//                    mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
//                } else {
//                    mCamera = Camera.open();
//                }
//
//                //To set hardware camera rotation
//                setCameraRotation();
//
//
//                mCameraPreview = new CameraPreview(mActivity, mCamera);
//                mCameraPreview.setReadyListener(new CameraPreview.ReadyToTakePicture() {
//                    @Override
//                    public void readyToTakePicture(boolean ready) {
//                        readyToTakePicture = ready;
//                        handleFocus(null, mCamera.getParameters());
//                    }
//                });
//
//                mCameraPreview.setOnTouchListener(this);
//
//                mCameraLayout = binder.cameraPreview;
//                mCameraLayout.addView(mCameraPreview);
//
//                //set the screen layout to fullscreen
//                mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
//            } catch (Exception e) {
//                Log.e("Camera Open Exception", "" + e.getMessage());
//            }
//
//        } else {
//            Log.e(TAG, "camera not null");
//        }
//    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_REVIEW) {
                String capturedFilePath = "";
                mPictureTakenListener.onPictureTaken(capturedFilePath);
            }
        } else {
            if (requestCode != 101) {
                mActivity.setResult(resultCode);
                mActivity.finish();
            }
        }
    }

    private void createSupportedFlashList(Camera.Parameters parameters) {
        supportedFlashModes = (ArrayList<String>) parameters.getSupportedFlashModes();
        if (supportedFlashModes == null) {
            currentFlashMode.setVisibility(View.GONE);
            rcvFlash.setVisibility(View.GONE);
        } else {
            currentFlashMode.setVisibility(View.VISIBLE);
        }
    }

    private void createSupportedFlashList(CameraCharacteristics cc) {
//        supportedFlashModes = Arrays.asList(cc.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES));
        if (supportedFlashModes == null) {
            currentFlashMode.setVisibility(View.GONE);
            rcvFlash.setVisibility(View.GONE);
        } else {
            currentFlashMode.setVisibility(View.VISIBLE);
        }
    }

    private void createFlashModesDropDown() {
        FlashModeRecyclerHorizontalAdapter flashModeAdapter = new FlashModeRecyclerHorizontalAdapter(getActivity(), supportedFlashModes);
        rcvFlash.setAdapter(flashModeAdapter);
        rcvFlash.setVisibility(View.VISIBLE);
        flashModeAdapter.setOnItemClickListener(new FlashModeRecyclerHorizontalAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                setFlash(supportedFlashModes.get(position));
                rcvFlash.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mCamera != null) {

            Camera.Parameters params = mCamera.getParameters();
            int action = event.getAction();


            if (event.getPointerCount() > 1) {
                // handle multi-touch events
                if (action == MotionEvent.ACTION_POINTER_DOWN) {
                    mDist = getFingerSpacing(event);
                } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                    mCamera.cancelAutoFocus();
                    handleZoom(event, params);
                }
            } else {
                // handle single touch events
                if (action == MotionEvent.ACTION_UP) {
                    handleFocus(event, params);
                }
            }
            if (event.getPointerCount() > 1) {
                return true;
            }

            final Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);

            try {
                mCamera.autoFocus(null);

                drawingView.setHaveTouch(true, focusRect);
                drawingView.invalidate();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        drawingView.setHaveTouch(false, focusRect);
                        drawingView.invalidate();
                    }
                }, 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        new ImagePostProcessing(mActivity, data).execute();
    }

    private void setCameraRotation() {
        //STEP #1: Get rotation degrees

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; //Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; //Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;//Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;//Landscape right
        }
        int rotate = (info.orientation - degrees + 360) % 360;

        //STEP #2: Set the 'rotation' parameter
        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(rotate);
        mCamera.setParameters(params);

        Camera.Parameters parameters = mCamera.getParameters();
        createSupportedFlashList(parameters);

        setFlashLayoutAndMode();
    }

    private void setOrientation(Activity activity, CameraOrientation orientation) {
        if (orientation != null) {
            if (orientation == CameraOrientation.landscape) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                binder.buttonCaptureHorizontal.setVisibility(View.VISIBLE);
                binder.buttonCaptureVertical.setVisibility(View.INVISIBLE);
            } else if (orientation == CameraOrientation.portrait) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                binder.buttonCaptureHorizontal.setVisibility(View.INVISIBLE);
                binder.buttonCaptureVertical.setVisibility(View.VISIBLE);
            }
        } else {
            Log.e(Constants.TAG, "No orientation set");
        }
    }

    private Rect calculateTapArea(float x, float y, float coefficient) {
        int FOCUS_AREA_SIZE = 200;
        int areaSize = Float.valueOf(FOCUS_AREA_SIZE * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, mCameraPreview.getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, mCameraPreview.getHeight() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        //        matrix.mapRect(rectF);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }

    public void handleFocus(MotionEvent event, final Camera.Parameters params) {
        Log.d(TAG, "handleFocus: " + event);
        //        int pointerId = event.getPointerId(0);
        //        int pointerIndex = event.findPointerIndex(pointerId);
        // Get the pointer's current position
        //        float x = event.getX(pointerIndex);
        //        float y = event.getY(pointerIndex);

        if (!readyToTakePicture) {
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    sensorManager.unregisterListener(sensorEventListener);
                    List<String> supportedFocusModes = params.getSupportedFocusModes();
                    if (supportedFocusModes != null && supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        if (mCamera != null) {
                            mCamera.setParameters(params);
                            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean b, Camera camera) {
                                    try {
                                        sensorManager.registerListener(sensorEventListener,
                                                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                                                SensorManager.SENSOR_DELAY_NORMAL);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }, 500);

    }

    /**
     * Determine the space between the first two fingers
     */
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    public void stopCamera() {
        try {
            readyToTakePicture = false;
            if (null == mCamera) {
                return;
            }
            mCamera.setPreviewCallback(null);
            mCameraPreview.getHolder().removeCallback(mCameraPreview);
            mCamera.stopPreview();
            mCameraLayout.removeAllViews();
            mCamera.release();
            mCamera = null;
            mCameraPreview = null;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCamera();
    }

    private void startCamera(int cameraFacing) {
        if (mCamera == null) {
            try {
                mCamera = Camera.open(cameraFacing);


                //To set hardware camera rotation
                setCameraRotation();

                mCameraPreview = new CameraPreview(mActivity, mCamera);

                mCameraPreview.setReadyListener(new CameraPreview.ReadyToTakePicture() {
                    @Override
                    public void readyToTakePicture(boolean ready) {
                        readyToTakePicture = ready;
                        handleFocus(null, mCamera.getParameters());


                    }
                });

                mCameraPreview.setOnTouchListener(this);

                mCameraLayout = binder.cameraPreview;
                mCameraLayout.addView(mCameraPreview);

                //set the screen layout to fullscreen
                mActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);


            } catch (Exception e) {
                Log.e("Camera Open Exception", "" + e.getMessage());
            }
        } else {
            Log.e(TAG, "camera not null");
        }
    }

    private int initCameraId() {
        int count = Camera.getNumberOfCameras();
        int result = -1;

        if (count > 0) {
            result = 0; // if we have a camera, default to this one

            Camera.CameraInfo info = new Camera.CameraInfo();

            for (int i = 0; i < count; i++) {
                Camera.getCameraInfo(i, info);

                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK
                        && !useFrontFacingCamera) {
                    result = i;
                    break;
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
                        && useFrontFacingCamera) {
                    result = i;
                    break;
                }
            }
        }

        return result;
    }

    public int setPhotoOrientation(Activity activity, int cameraId) {
        if (NeonImagesHandler.getSingleonInstance().getCameraParam().getCameraOrientation() == CameraOrientation.portrait) {
            if (localCameraFacing == CameraFacing.front) {
                return 180;
            } else {
                return 0;
            }
        }
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        // do something for phones running an SDK before lollipop
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    private int getBackFacingCameraId() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public interface PictureTakenListener {
        void onPictureTaken(String filePath);

        void onPicturesFinalized(ArrayList<FileInfo> infos);

        void onPicturesFinalized(Map<ImageTagModel, List<FileInfo>> filesMap);
    }

    public interface SetOnPictureTaken {
        void onPictureTaken(String filePath);
    }

    private class ImagePostProcessing extends AsyncTask<Void, Void, File> {

        private Context context;
        private byte[] data;
        private ProgressDialog progressDialog;

        ImagePostProcessing(Context context, byte[] data) {
            this.context = context;
            this.data = data;
        }


        public File savePictureToStorage() {
            File pictureFile = Constants.getMediaOutputFile(getActivity(), Constants.TYPE_IMAGE);
            Log.d("HIMANSHU FILE=", pictureFile.getAbsolutePath());
            if (pictureFile == null)
                return null;

            try {
                OutputStream fos = new FileOutputStream(pictureFile);
                Bitmap bm;

                // COnverting ByteArray to Bitmap - >Rotate and Convert back to Data
                if (data != null) {
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    int screenHeight = getResources().getDisplayMetrics().heightPixels;
                    bm = BitmapFactory.decodeByteArray(data, 0, (data != null) ? data.length : 0);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        // Notice that width and height are reversed

                        if (bm.getHeight() < bm.getWidth()) {
                            bm = rotateBitmap(bm, 90);
                        }

                        Bitmap scaled = Bitmap.createScaledBitmap(bm, screenWidth, screenHeight, true);
                        int w = scaled.getWidth();
                        int h = scaled.getHeight();
                        // Setting post rotate to 90
                        Matrix mtx = new Matrix();
                        int cameraId;
                        if (cameraFacing == CameraFacing.front) {
                            cameraId = getBackFacingCameraId();
                        } else {
                            cameraId = initCameraId();
                        }
                        int CameraEyeValue = setPhotoOrientation(getActivity(), cameraId); // CameraID = 1 : front 0:back
                        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) { // As Front camera is Mirrored so Fliping the Orientation
                            if (CameraEyeValue == 270) {
                                mtx.postRotate(90);
                            } else if (CameraEyeValue == 90) {
                                mtx.postRotate(270);
                            }
                        } else {
                            mtx.postRotate(CameraEyeValue); // CameraEyeValue is default to Display Rotation
                        }

                        bm = Bitmap.createBitmap(scaled, 0, 0, w, h, mtx, true);
                    } else {// LANDSCAPE MODE
                        //No need to reverse width and height
                        bm = Bitmap.createScaledBitmap(bm, screenWidth, screenHeight, true);
                    }
                } else {
                    return null;
                }
                // COnverting the Die photo to Bitmap


                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                fos.write(byteArray);
                //fos.write(data);
                fos.close();
                Uri pictureFileUri = Uri.parse("file://" + pictureFile.getAbsolutePath());
                mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        pictureFileUri));

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (pictureFile.exists()) {
                return pictureFile;
            } else {
                pictureFile = null;
                savePictureToStorage();
            }
            return pictureFile;
        }

        @Override
        protected File doInBackground(Void... params) {
            File pictureFile = savePictureToStorage();
            return pictureFile;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(context, null, "Saving Picture", true);
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (progressDialog != null)
                progressDialog.dismiss();
            if (file != null) {
                /*if(getActivity() instanceof NeonBaseNeutralActivity) {
                    mPictureTakenListener.onPictureTaken(file.getAbsolutePath());
                    readyToTakePicture = true;
                    return;
                }
                mCamera.startPreview();*/

                // Modify for live Photos

                if (NeonImagesHandler.getSingletonInstance().getLivePhotosListener() != null) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent viewPagerIntent = new Intent(context, ImageReviewActivity.class);
                            viewPagerIntent.putExtra(Constants.IMAGE_REVIEW_POSITION, NeonImagesHandler.getSingletonInstance().getImagesCollection().size() - 1);
                            startActivity(viewPagerIntent);
                        }
                    }, 200);
                }

                mPictureTakenListener.onPictureTaken(file.getAbsolutePath());

                // readyToTakePicture = true;
            } else {
                Toast.makeText(context, getString(R.string.camera_error), Toast.LENGTH_SHORT).show();
                //readyToTakePicture = true;

            }

            readyToTakePicture = true;
            if (mCamera != null) {
                mCamera.startPreview();
            }
        }
    }
}