package com.arrata.user.opencvcamera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.Camera2Renderer;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorKNN;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener{

    private CameraBridgeViewBase mOpenCvCameraView;
    private Camera2Renderer camera2Renderer;
    final String TAG = "OPENCVTEST";

    private Mat initialBackground = null;
    VideoCapture capture;
    BackgroundSubtractorMOG2 mog2;
    BackgroundSubtractorKNN mog;


    CameraDevice cameraDevice;
    CameraCharacteristics mCameraCharacteristics;

    Camera.Parameters params;
    Camera mCamera = null;
    int minExposure;
    int maxExposure;

    long backgroundTime;

    ColorBlobDetector mDetector;
    Scalar mBlobColorRgba;

    private boolean isExposureLocked = false;

    private boolean processed = false;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private CameraDevice mCameraDevice;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;

    private android.util.Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.

            mCameraDevice = cameraDevice;


            try {
                captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);

            } catch(CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {

            cameraDevice.close();

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {

            cameraDevice.close();

        }

    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            String cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;

            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                return;
            }

            manager.openCamera(cameraId, mStateCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        if (!OpenCVLoader.initDebug()) {
            Log.e(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), not working.");
        } else {
            Log.d(this.getClass().getSimpleName(), "  OpenCVLoader.initDebug(), working.");
        }

        //ask fo write permission
        if(Build.VERSION.SDK_INT >= 23) {

            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
        }

        /*
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                openCamera();
            }
        }, 1000);
        */

/*
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        if(Build.VERSION.SDK_INT >= 21 && false) {
            try {
                String cameraId = manager.getCameraIdList()[1];

                mCameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                Range<Integer> range = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);

                minExposure = range.getLower();
                maxExposure = range.getUpper();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            mCamera = openFrontFacingCameraGingerbread();
            Camera.Parameters params = mCamera.getParameters();
            if(params.isAutoExposureLockSupported()) {
                isExposureLocked = !isExposureLocked;
                params.setAutoExposureLock(!isExposureLocked);
                mCamera.setParameters(params);
T

            }
            params.setExposureCompensation(150);
            mCamera.setParameters(params);
        }*/

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        if(Build.VERSION.SDK_INT >= 21) {
            try {
                String cameraId = manager.getCameraIdList()[1];

                mCameraCharacteristics = manager.getCameraCharacteristics(cameraId);
                Range<Integer> range = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);

                minExposure = range.getLower();
                maxExposure = range.getUpper();

                Log.d("minExposure", String.valueOf(minExposure));
                Log.d("maxExposure", String.valueOf(maxExposure));
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }

        //CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

        mog2 = Video.createBackgroundSubtractorMOG2();
        mog = Video.createBackgroundSubtractorKNN();

        //mog2.setDetectShadows(true);
        //mog2.setShadowThreshold(0.3);
        mog2.setShadowValue(0);
        Log.d("ThresholdGen", String.valueOf(mog2.getVarThresholdGen()));
        //mog2.setVarThresholdGen(5.0);//default 0.9 // was 19.0
        Log.d("MeanVariance", String.valueOf(mog2.getVarMax()));
        Log.d("MinVariance", String.valueOf(mog2.getVarMin()));
        //mog2.setVarMin(0.7);//Gaussian Variance


        //mog.setDetectShadows(true);
        //mog.setShadowValue(0);
        //mog.setShadowThreshold(0.5);
        Log.d("History",String.valueOf(mog.getHistory()));



        //mog.setDetectShadows(false);
        //mog.setShadowValue(0);

        //camera2Renderer; = ()

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setMaxFrameSize(352, 288);//setting max frame size



        //Should later be set accordingly to the screen ratio of each phone
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(1);



        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {

    }

    private Camera openFrontFacingCameraGingerbread() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Camera failed to open: " + e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    //Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
/*
        mRgba = inputFrame;

        Core.transpose(mRgba, mRgbaF);
        Imgproc.resize(mRgbaF, mRgbaT, mRgbaT.size(), 0, 0, 0);

        Core.flip(mRgbaT, mRgba, -1);

        Mat resizeimage = new Mat();
        Size sz = new Size(352,288);
        Imgproc.resize( mRgba, resizeimage, sz );

        return resizeimage;*/


        if(initialBackground == null) {
            initialBackground = inputFrame;
            backgroundTime = System.currentTimeMillis();

            return inputFrame;

        } else {
            Camera.Parameters params;
            //params = mCamera.getParameters();
            //Log.d("Exposure", String.valueOf(params.getExposureCompensation()));

            System.gc();
            Mat fgMask = new Mat();

            //Imgproc.GaussianBlur(inputFrame, inputFrame, new Size(11, 11), 2.0);
            //Imgproc.GaussianBlur(inputFrame, inputFrame, new Size(5, 5), 2.0);

            if(System.currentTimeMillis() - backgroundTime < 3000) {
                //mog2.apply(inputFrame, fgMask, 0.05);
                mog.apply(inputFrame, fgMask, 0.05);

            } else {
                //mog2.apply(inputFrame, fgMask, 0.00001);
                mog.apply(inputFrame, fgMask, 0.00001);
                //Imgproc.medianBlur(inputFrame, inputFrame, 3);
//                Imgproc.dilate(inputFrame, inputFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(10, 10)));
//                Imgproc.erode(inputFrame, inputFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(10,10)));


            }

            Mat output = new Mat();
            inputFrame.copyTo(output, fgMask);

            Mat flipped = new Mat();
            Core.flip(fgMask, flipped, 1);

            //Utility.whiteContours(flipped);

            Imgproc.dilate(flipped, flipped, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(8, 8)));
            //Imgproc.erode(flipped, flipped, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5)));
            //order has been changed this order seems to fit best
            //changed from 5, 5

            //Imgproc.medianBlur(flipped, flipped, 5);


            Utility.whiteContours(flipped);

            //Utility.removeNoise(flipped);


            //Log.d("Rows", String.valueOf(flipped.rows()));
            //Log.d("Cols", String.valueOf(flipped.cols()));

            //Utility.iterateMat(flipped);

            if(!processed && (System.currentTimeMillis() - backgroundTime > 4000)) {

                processed = true;
            }

            if(processed) {
                Utility.head(flipped);
            }


            return flipped;
            //return contourOutput;
        }

    }
}
