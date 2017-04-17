package com.arrata.user.opencvcamera;

import android.Manifest;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.BackgroundSubtractorKNN;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener{

    private CameraBridgeViewBase mOpenCvCameraView;
    final String TAG = "OPENCVTEST";

    private Mat initialBackground = null;
    VideoCapture capture;
    BackgroundSubtractorMOG2 mog2;
    BackgroundSubtractorKNN mog;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
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

        mog2 = Video.createBackgroundSubtractorMOG2();
        mog = Video.createBackgroundSubtractorKNN();

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(1);

        //mOpenCvCameraView.setRotation(90.0f);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

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
        if(initialBackground == null) {
            initialBackground = inputFrame;

            return inputFrame;

        } else {
            Mat fgMask = new Mat();

            mog2.apply(inputFrame, fgMask, 0.01);

            Mat output = new Mat();
            inputFrame.copyTo(output, fgMask);

            return fgMask;
        }

    }
}
