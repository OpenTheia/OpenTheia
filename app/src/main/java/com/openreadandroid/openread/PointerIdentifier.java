package com.openreadandroid.openread;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractorMOG2;
import org.opencv.video.KalmanFilter;
import org.opencv.video.Video;

public class PointerIdentifier {

	public class FingerResult {
		public Boolean fingerFound;
		public Mat maskImage;
		public Mat highlightImage;
		public Point fingerPoint;

		public FingerResult(Mat maskImage, Mat highlightImage, Point fingerPoint) {
			super();
			this.maskImage = maskImage;
			this.highlightImage = highlightImage;
			this.fingerPoint = fingerPoint;
			fingerFound = true;
		}

		public FingerResult() {
			super();
			this.maskImage = null;
			this.highlightImage = null;;
			this.fingerPoint = null;;
			fingerFound = false;
		}

	}

	BackgroundSubtractorMOG2 mog2;
	KalmanFilter kalman;

	public PointerIdentifier() {
		mog2 = Video.createBackgroundSubtractorMOG2();
		mog2.setDetectShadows(false);

		// Make kalman filter
		int numDynamic = 4;
		int numMeasured = 2;
		kalman = new KalmanFilter(numDynamic,numMeasured,0,CvType.CV_32F);

		float friction = (float) 0.5;

		//set transition matrix
		float[] tM = { 1,0,1,0,
				0,1,0,1,
				0,0,friction,0,
				0,0,0,friction};
		Mat transitionMatrix=new Mat(numDynamic,numDynamic,CvType.CV_32F,new Scalar(0));
		transitionMatrix.put(0,0,tM);
		kalman.set_transitionMatrix(transitionMatrix);
		//set init measurement
		Mat measurementMatrix = Mat.eye(numMeasured,numDynamic, CvType.CV_32F);
		kalman.set_measurementMatrix(measurementMatrix);

		//Set state matrix
		Mat statePre = new Mat(numDynamic,1, CvType.CV_32F);
		statePre.put(0, 1, 0);
		statePre.put(1, 1, 0);
		statePre.put(2, 1, 0);
		statePre.put(3, 1, 0);
		kalman.set_statePre(statePre);

		//Process noise Covariance matrix
		Mat processNoiseCov=Mat.eye(numDynamic,numDynamic,CvType.CV_32F);
		processNoiseCov=processNoiseCov.mul(processNoiseCov,1e-2);
		kalman.set_processNoiseCov(processNoiseCov);

		//Measurement noise Covariance matrix: reliability on our first measurement
		Mat measurementNoiseCov=Mat.eye(numMeasured,numMeasured,CvType.CV_32F);
		measurementNoiseCov=measurementNoiseCov.mul(measurementNoiseCov,1e-1);
		kalman.set_measurementNoiseCov(measurementNoiseCov);

		Mat id2=Mat.eye(numDynamic,numDynamic,CvType.CV_32F);
		//id2=id2.mul(id2,0.1);
		kalman.set_errorCovPost(id2);
	}

	public FingerResult getFingerPointer(Mat inputImage, Mat highlightImage, Mat sourceImage) {

		if (sourceImage == null) {
			return new FingerResult();
		}

		Mat handMask = getHandMaskFromImage(inputImage,inputImage.width(),inputImage.height());

		Core.add(inputImage, new Scalar(255,0,0,255), highlightImage, handMask);


		Point fingerPointResult = getFingerPointFromMaskSmallestY(inputImage, highlightImage, handMask);

		Mat prediction= kalman.predict();
		Point predictPt = new Point();
		double predictWidth;
		predictPt.x = prediction.get(0,0)[0]; //predictPt.x = prediction.get(1,1)[0];
		predictPt.y = prediction.get(1,0)[0]; //predictPt.y = prediction.get(2,1)[0];

		if (fingerPointResult != null) {

			Mat measurement = new Mat(2,1,CvType.CV_32F, new Scalar(0));

			measurement.put(0, 0, fingerPointResult.x); // measurementMatrix.put(1, 1, center.x);
			measurement.put(1, 0, fingerPointResult.y); //measurementMatrix.put(2, 1, center.y);

			Mat estimated = kalman.correct(measurement);
			Point statePt = new Point();
			statePt.x=estimated.get(0,0)[0];
			statePt.y= estimated.get(1,0)[0];

			// Highlight image
            Imgproc.drawMarker(highlightImage,fingerPointResult,new Scalar(0,0,255,255),Imgproc.MARKER_CROSS,20,3,0);
            Imgproc.drawMarker(highlightImage,statePt,new Scalar(0,255,0,255),Imgproc.MARKER_CROSS,20,3,0);

            return new FingerResult(handMask,highlightImage,statePt);
		} else {

			Imgproc.drawMarker(highlightImage,predictPt,new Scalar(255,0,255,255),Imgproc.MARKER_CROSS,20,3,0);
			return new FingerResult(handMask,highlightImage,predictPt);
		}
	}

	public Mat getHandMaskFromImage(Mat inputImageHSV, double screenWidth, double screenHeight) {
		Mat handMask = new Mat();
		double elementWidthPerc = 0.01;
		double elementHeightPerc = 0.05;

		// Get initial mask

        //mog2.apply(inputImageHSV, handMask,0.5);
        mog2.apply(inputImageHSV, handMask,0.9);

		// Erode to remove small specks and dilate to restore image back to size

		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT,
                new Size(elementWidthPerc * screenWidth, elementHeightPerc * screenHeight));

		Imgproc.erode(handMask, handMask, dilateElement);
		Imgproc.dilate(handMask, handMask, dilateElement);


		return handMask;
	}

	public Point getFingerPointFromMaskSmallestY(Mat inputImage, Mat highlightImage, Mat handMask) {
		ArrayList<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
		Point returnPoint = null;
		Mat hierarchy = new Mat();

		Imgproc.findContours(handMask, contourList, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		double largestContourArea = -1;
		int largestContourIndex = -1;

		for (int i = 0; i < contourList.size(); i++) {
			if (Imgproc.contourArea(contourList.get(i)) > largestContourArea) {
				largestContourIndex = i;
				largestContourArea = Imgproc.contourArea(contourList.get(i));
			}
		}

		if (largestContourIndex != -1) {
			ArrayList<Point> contourPointList = new ArrayList<Point>(contourList.get(largestContourIndex).toList());

			double minYVal = 9999;
			int minYIndex = -1;

			for (int i = 0; i < contourPointList.size(); i++) {
				if (contourPointList.get(i).y < minYVal) {
					minYIndex = i;
					minYVal = contourPointList.get(i).y;
				}
			}

			if (minYIndex != -1) {
				returnPoint = contourPointList.get(minYIndex).clone();
			}
		}

		return returnPoint;
	}


}
