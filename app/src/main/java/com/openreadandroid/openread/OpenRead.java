package com.openreadandroid.openread;

import android.content.Context;

import org.opencv.core.Mat;

import com.openreadandroid.openread.PointerIdentifier.FingerResult;

import com.openreadandroid.openread.ScreenAnalyzer;

public class OpenRead {


	public class screenAnalysisResult {
		public PointerIdentifier.FingerResult fingerPointerResult;
		
		public screenAnalysisResult(FingerResult fingerPointerResult) {
			this.fingerPointerResult = fingerPointerResult;
		}
	}
	
	ScreenIdentifier screenGrabber;
	PointerIdentifier fingerPointerGrabber;
	ScreenAnalyzer screenAnalyzer;
	
	public OpenRead(Context appContext) {
		screenGrabber = new ScreenIdentifier();
		fingerPointerGrabber = new PointerIdentifier();
		screenAnalyzer = new ScreenAnalyzer(appContext);
	}
	
	public void setRefImage(Mat inputMat) {
		screenGrabber.setRefImage(inputMat);
		//screenAnalyzer.analyzePhoto(inputMat);
	}
	
	public Mat getRefImage() {
		return screenGrabber.getRefImage();
	}
	
	public screenAnalysisResult analyzeImage(Mat inputMat) {
		
		Mat inputHighlightImage = inputMat.clone();
		PointerIdentifier.FingerResult fingerPointerResult = fingerPointerGrabber.getFingerPointer(inputMat,
					inputHighlightImage,screenGrabber.getRefImage());
		//screenAnalyzer.highlightTextOnImage(inputHighlightImage);
		
		return new screenAnalysisResult(fingerPointerResult);
	}
	
}
