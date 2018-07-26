package com.openreadandroid.openread;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import com.openreadandroid.mainapp.JavaCameraViewExd;
import com.openreadandroid.openread.PointerIdentifier.FingerResult;

import com.openreadandroid.openread.ScreenAnalyzer;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;

public class OpenRead {


	public class screenAnalysisResult {
		public PointerIdentifier.FingerResult fingerPointerResult;
		
		public screenAnalysisResult(FingerResult fingerPointerResult) {
			this.fingerPointerResult = fingerPointerResult;
		}
	}

    // The state enum
    private enum OpenReadState {
        // Lower resolution state, when we are just findiing where the finger is
        TRACKING_FINGER,
        // Higher resolution state, when we are grabbing the screen for OCR
        OBTAINING_OCR_SCREEN,
        // Waiting state where no processing is done
        WAITING
    };

	public static int lowScreenResolution = 500;
	public static int highScreenResolution = 1400;
	public static int cameraSwitchDelay = 4000;
	
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

    OpenReadState currentState;

    public Mat prevImage;

    Context applicationContext;

    JavaCameraViewExd camera;
	
	public OpenRead(Context appContext,JavaCameraViewExd inCamera) {
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

        currentState = OpenReadState.TRACKING_FINGER;

        prevImage = null;

        applicationContext = appContext;

        camera = inCamera;
	}
	
	public void setRefImage(Mat inputMat) {
		screenGrabber.setRefImage(inputMat);
		switchStates(OpenReadState.OBTAINING_OCR_SCREEN);
	}
	
	public Mat getRefImage() {
		return screenGrabber.getRefImage();
	}
	
	public void analyzeImage(Mat inputMat) {
		
		Mat inputHighlightImage = inputMat.clone();
        prevImage = inputHighlightImage;

        if (currentState == OpenReadState.TRACKING_FINGER) {
            PointerIdentifier.FingerResult fingerPointerResult = fingerPointerGrabber.getFingerPointer(inputMat,
                    inputHighlightImage,screenGrabber.getRefImage());
            currentScreen.highlightAllScreenElements(inputHighlightImage);

            ScreenElement selectedElem;

            // Logic to detect if on an element
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

        } else if (currentState == OpenReadState.OBTAINING_OCR_SCREEN) {
            screenAnalyzer.analyzePhoto(inputMat);
            currentScreen.GenerateScreen(screenAnalyzer.textBlocks,inputMat.width(),inputMat.height());
            switchStates(OpenReadState.TRACKING_FINGER);
        }
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

    private void switchStates(OpenReadState inputState) {

        Handler mainHandler;
        Runnable myRunnable;

        Log.d(TAG, "Entering state: " + inputState.toString());

        switch (inputState) {
            case TRACKING_FINGER:

                // Get a handler that can be used to post to the main thread
                mainHandler = new Handler(applicationContext.getMainLooper());


                myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        camera.disableView();
                        camera.setMaxFrameSize(OpenRead.lowScreenResolution,OpenRead.lowScreenResolution);
                        camera.enableView();
                    }
                };
                mainHandler.post(myRunnable);


                currentState = OpenReadState.TRACKING_FINGER;

                break;

            case OBTAINING_OCR_SCREEN:

                // Get a handler that can be used to post to the main thread
                mainHandler = new Handler(applicationContext.getMainLooper());

                myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        camera.disableView();
                        camera.setMaxFrameSize(OpenRead.highScreenResolution,OpenRead.highScreenResolution);
                        camera.enableView();
                    } // This is your code
                };
                mainHandler.post(myRunnable);

                currentState = OpenReadState.WAITING;

                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        currentState = OpenReadState.OBTAINING_OCR_SCREEN;
                    }
                }, OpenRead.cameraSwitchDelay);

                break;
        }
    }

	
}
