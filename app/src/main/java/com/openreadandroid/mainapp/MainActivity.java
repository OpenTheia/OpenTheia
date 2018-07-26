package com.openreadandroid.mainapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import android.util.Log;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;

import com.openreadandroid.openread.OpenRead;

// Main Activity - Created 2018-01-13
// Initiates the Camera, and other UI elements. This class deals with everything related to the
// Android App. Think of it as a main class

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // TAG used for debugging purposes
    private static final String TAG = "MainActivity";

    JavaCameraViewExd javaCameraView;

    // Initialize OpenCV libraries
    static {
        System.loadLibrary("MyOpencvLibs");
    }

    BaseLoaderCallback mLoaderCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    javaCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }

        }
    };

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;

    Mat prevCameraImage;

    // Open Read Controller

    OpenRead readController;

    // Constructor
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        javaCameraView = findViewById(R.id.java_camera_view);
        javaCameraView.setVisibility(View.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);

        javaCameraView.setMaxFrameSize(OpenRead.lowScreenResolution,OpenRead.lowScreenResolution);

        javaCameraView.enableFpsMeter();

        final Button getRefImageButton = findViewById(R.id.get_ref_img_button);

        getRefImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                readController.setRefImage(prevCameraImage);
            }
        });

        readController = new OpenRead(this.getApplicationContext(), javaCameraView);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    public void onResume()
    {
        // To do - Add visionSystem resume
        super.onResume();
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG,"OpenCV Loaded successfully");
            mLoaderCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG,"OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallBack);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }


    // This function is called when a new camera frame is received
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        // Some magic to flip the frame to the correct orientation
        mRgba = inputFrame.rgba();
        Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());

        Mat resultMat;

        resultMat = mRgbaT;

        Log.d(TAG,"Screen width: " + resultMat.width() + " height: " + resultMat.height());

        Mat resizedImage = new Mat(resultMat.rows(),(int) (resultMat.cols()*0.561165),resultMat.type());
        Imgproc.resize(resultMat, resizedImage, resizedImage.size(), 0, 0, Imgproc.INTER_CUBIC);
        //Imgproc.cvtColor(resizedImage,resizedImage,Imgproc.COLOR_RGBA2RGB);

        prevCameraImage = resizedImage;

        readController.analyzeImage(resizedImage);

        Mat viewImageMat;

        if (readController.prevImage != null) {
            viewImageMat = readController.prevImage.clone();
        } else {
            viewImageMat = resizedImage;
        }

        Bitmap initial_bm;
        initial_bm = Bitmap.createBitmap(viewImageMat.cols(), viewImageMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(viewImageMat, initial_bm);

        final Bitmap highlight_bm = initial_bm.copy(initial_bm.getConfig(),true);

        // Update images on UI
        runOnUiThread(new Runnable() {

            public void run() {

                // find the imageview and draw it!
                ImageView iv = findViewById(R.id.highlighted_image_view);
                iv.setImageBitmap(highlight_bm);

            }
        });

        return resultMat;
    }
}
