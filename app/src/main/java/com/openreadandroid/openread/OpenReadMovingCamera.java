package com.openreadandroid.openread;

import android.content.Context;

import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;

import com.openreadandroid.mainapp.JavaCameraViewExd;
import com.openreadandroid.openread.PointerIdentifier.FingerResult;
import com.openreadandroid.openread.ScreenIdentifier.matchResult;

public class OpenReadMovingCamera extends OpenRead {

    public class movingScreenAnalysisResult extends screenAnalysisResult {
        public ScreenIdentifier.matchResult screenMatchResult;

        public movingScreenAnalysisResult(FingerResult fingerPointerResult, matchResult screenMatchResult) {
            super(fingerPointerResult);
            this.screenMatchResult = screenMatchResult;
        }
    }

    private MenuAndFingerTracking menuTracker;
    public Mat menuTrackedImage;


    public OpenReadMovingCamera(Context inContext, JavaCameraViewExd inCamera) {
        super(inContext, inCamera);
        menuTracker = new MenuAndFingerTracking();
    }

    public void analyzeImage(Mat inputMat) {

        MenuAndFingerTracking.menuAndFingerInfo screenMatchResult = menuTracker.grabMenuAndFingerInfo(inputMat);

        Mat inputHighlightImage;

        if (screenMatchResult.menuTracked) {
            inputHighlightImage = menuTracker.resultImage;
            menuTrackedImage = menuTracker.highlightedImage;
        } else {
            inputHighlightImage = inputMat;
            menuTrackedImage = inputMat.clone();
        }

        super.analyzeImage(inputHighlightImage);
    }


}
