package com.openreadandroid.openread;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;

import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

// Screen Analyzer - Created 2018-02-05
// Identifies all screen elements for a given screen

public class ScreenAnalyzer {

    private Vision.Builder visionBuilder;
    private Vision vision;
    private Context appContext;
    // TAG used for debugging purposes
    private static final String TAG = "ScreenAnalyzer";

    // This should be read only. Maybe use a getter? For the future!
    public SparseArray<TextBlock> textBlocks;

    private int screenWidth;
    private int screenHeight;

    public ScreenAnalyzer(Context inAppContext) {

        visionBuilder = new Vision.Builder(
                new NetHttpTransport(),
                new AndroidJsonFactory(),
                null);

        visionBuilder.setVisionRequestInitializer(
                new VisionRequestInitializer("YOUR_API_KEY_HERE"));


        visionBuilder.build();

        vision = visionBuilder.build();

        appContext = inAppContext;

    }

    public void analyzePhoto(Mat inputMat) {
        Bitmap bm = Bitmap.createBitmap(inputMat.cols(), inputMat.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(inputMat, bm);

        analyzePhoto(bm);
    }

    public void analyzePhoto(Bitmap inputBitmap) {

        Log.i(TAG,"Starting Analyzing screen");

        if(inputBitmap != null) {

            TextRecognizer textRecognizer = new TextRecognizer.Builder(appContext).build();

            if(!textRecognizer.isOperational()) {
                // Note: The first time that an app using a Vision API is installed on a
                // device, GMS will download a native libraries to the device in order to do detection.
                // Usually this completes before the app is run for the first time.  But if that
                // download has not yet completed, then the above call will not detect any text,
                // barcodes, or faces.
                // isOperational() can be used to check if the required native libraries are currently
                // available.  The detectors will automatically become operational once the library
                // downloads complete on device.
                Log.w(TAG, "Detector dependencies are not yet available.");

                // Check for low storage.  If there is low storage, the native library will not be
                // downloaded, so detection will not become operational.
                IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                boolean hasLowStorage = appContext.registerReceiver(null, lowstorageFilter) != null;

                if (hasLowStorage) {
                    Toast.makeText(appContext,"Low Storage", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Low Storage");
                }
            }

            Frame imageFrame = new Frame.Builder()
                    .setBitmap(inputBitmap)
                    .build();

            textBlocks = textRecognizer.detect(imageFrame);

            Log.w(TAG,"Detected " + textBlocks.size() + " text objects");

            screenWidth = inputBitmap.getWidth();
            screenHeight = inputBitmap.getHeight();

            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));

                Log.i(TAG, textBlock.getValue());
            }
        }

    }

    public void highlightTextOnImage(Mat inputImage) {

        if (textBlocks != null) {
            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
                // Do something with value

                // Always gonna give 4 points according to the API so it's okay to hard code some stuff
                Point[] textCornerPoints = textBlock.getCornerPoints();

                org.opencv.core.Point[] openCVPointList = new org.opencv.core.Point[4];

                for (int j = 0; j < 4; j++) {
                    openCVPointList[j] = new org.opencv.core.Point(textCornerPoints[j].x, textCornerPoints[j].y);
                }

                for (int j = 0; j < 4; j++) {
                    Imgproc.line(inputImage,openCVPointList[j],openCVPointList[(j+1)%4], new Scalar(255,0,0), 3);
                }
            }
        }

    }

    public void highlightTextOnResultImage(ArrayList<ScreenElement> elements, Mat inputImage) {

        for (int i = 0; i < elements.size(); i++) {
            ScreenElement visitor = elements.get(i);

            org.opencv.core.Point[] openCVPointList = new org.opencv.core.Point[4];

            double leftX = visitor.getX_base() * screenWidth;
            double topY = visitor.getY_base() * screenHeight;
            double rightX = (visitor.getX_base() + visitor.getX_Width())* screenWidth;
            double bottomY = (visitor.getY_base() + visitor.getY_length()) * screenHeight;

            openCVPointList[0] = new org.opencv.core.Point(leftX,topY);
            openCVPointList[1] = new org.opencv.core.Point(rightX,topY);
            openCVPointList[2] = new org.opencv.core.Point(rightX,bottomY);
            openCVPointList[3] = new org.opencv.core.Point(leftX,bottomY);

            for (int j = 0; j < 4; j++) {
                Imgproc.line(inputImage,openCVPointList[j],openCVPointList[(j+1)%4], new Scalar(255,0,0), 3);
            }

        }
    }

}
