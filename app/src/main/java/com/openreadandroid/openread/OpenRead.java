package com.openreadandroid.openread;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import com.openreadandroid.openread.PointerIdentifier.FingerResult;

import com.openreadandroid.openread.ScreenAnalyzer;

import java.util.Locale;

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
	Screen currentScreen;
	int onElemCounter;

	String TAG = "OpenRead";

	// Number of frames where the pointer is constant before the element is read out
	int elemToReadCount = 5;

    private TextToSpeech textSpeaker;
    Boolean readingTextNoInterrupt;
    String lastReadText;
	
	public OpenRead(Context appContext) {
		screenGrabber = new ScreenIdentifier();
		fingerPointerGrabber = new PointerIdentifier();
		screenAnalyzer = new ScreenAnalyzer(appContext);
		currentScreen = new Screen();
		onElemCounter = 0;

		// Setting up narrator
        textSpeaker = new TextToSpeech(appContext, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textSpeaker.setLanguage(Locale.ENGLISH);
                }
            }
        });
        readingTextNoInterrupt = false;
        lastReadText = null;
	}
	
	public void setRefImage(Mat inputMat) {
		screenGrabber.setRefImage(inputMat);
		screenAnalyzer.analyzePhoto(inputMat);
		currentScreen.GenerateScreen(screenAnalyzer.textBlocks,inputMat.width(),inputMat.height());
	}
	
	public Mat getRefImage() {
		return screenGrabber.getRefImage();
	}
	
	public screenAnalysisResult analyzeImage(Mat inputMat) {
		
		Mat inputHighlightImage = inputMat.clone();
		PointerIdentifier.FingerResult fingerPointerResult = fingerPointerGrabber.getFingerPointer(inputMat,
					inputHighlightImage,screenGrabber.getRefImage());
		currentScreen.highlightAllScreenElements(inputHighlightImage);

		ScreenElement selectedElem;

		if (fingerPointerResult.fingerFound) {
            selectedElem = currentScreen.GetElementAtPoint(fingerPointerResult.fingerPoint.x / inputHighlightImage.width(),fingerPointerResult.fingerPoint.y / inputHighlightImage.height());
        } else {
		    selectedElem = null;
        }

		if (selectedElem != null) {
            onElemCounter++;

            if (onElemCounter > elemToReadCount) {
                // Read Element
                currentScreen.highlightScreenElement(inputHighlightImage,selectedElem, new Scalar(0,0,255,255));
                readText(selectedElem.GetElementDescription());
            }

        } else {
		    onElemCounter = 0;
		    lastReadText = null;
        }
		return new screenAnalysisResult(fingerPointerResult);
	}

    private void readText(String textToRead) {
        if (!readingTextNoInterrupt) {
            if (!textToRead.equals(lastReadText)) {
                textSpeaker.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null);
                lastReadText = textToRead;
            }
        } else {
            // Do nothing
        }
    }
	
}
