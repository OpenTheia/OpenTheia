package com.openreadandroid.openread;

import java.util.ArrayList;

import org.opencv.core.Core;
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
import org.opencv.video.Video;

public class PointerIdentifier {
	
	public class HSVRange {
		public int minHue;
		public int maxHue;
		public int minSat;
		public int maxSat;
		public int minVal;
		public int maxVal;
		
		public HSVRange(int minHue, int maxHue, int minSat, int maxSat, int minVal, int maxVal) {
			super();
			this.minHue = minHue;
			this.maxHue = maxHue;
			this.minSat = minSat;
			this.maxSat = maxSat;
			this.minVal = minVal;
			this.maxVal = maxVal;
		}
		
		public Scalar getMinScalar() {
			return new Scalar(minHue, minSat, minVal);
		}
		
		public Scalar getMaxScalar() {
			return new Scalar(maxHue, maxSat, maxVal);
		}
		
		public void print() {
			String printLine = "";
			printLine = "Hue: " + minHue + " : " + maxHue;
			System.out.println(printLine);
			printLine = "Sat: " + minSat + " : " + maxSat;
			System.out.println(printLine);
			printLine = "Val: " + minVal + " : " + maxVal;
			System.out.println(printLine);
		}
	}
	
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
	
	public PointerIdentifier() {
		mog2 = Video.createBackgroundSubtractorMOG2();
		mog2.setDetectShadows(false);
	}
	
	public Mat getHandMaskFromImage(Mat inputImageHSV) {
		Mat handMask = new Mat();
		int blurSize = 11;
		int dilateElementSize = 2 * ( 2 ) + 1;
		
		// Get initial mask
		
		mog2.apply(inputImageHSV, handMask,0.01);
		
		// Erode to remove small specks and dilate to restore image back to size
		
		Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 20));
		
		Imgproc.erode(handMask, handMask, dilateElement);
		Imgproc.dilate(handMask, handMask, dilateElement);
		

		return handMask;
	}
	
	// Grab the finger point by using deep analysis of the contours. This function is broken,
	// and has been left here for further development. Do not use!
	public Point getFingerPointFromMaskContourAnalysis(Mat inputImage, Mat highlightImage, Mat handMask) {
		ArrayList<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
		
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
	        
	    Imgproc.drawContours(highlightImage, contourList, largestContourIndex, new Scalar(0,0,255),1);
	    
	    MatOfPoint largestContour = contourList.get(largestContourIndex);
		
	    if (!contourList.isEmpty()) {
	    	ArrayList<MatOfPoint> hullList = new ArrayList<MatOfPoint>();
	    	MatOfInt hull = new MatOfInt();
	    	Imgproc.convexHull(largestContour, hull, false);
	    	hullList.add(PointerIdentifier.convertIndexesToPoints(largestContour, hull));
	    	Imgproc.drawContours(highlightImage, hullList, 0, new Scalar(0,255,0),3);
	    	
	    	
	    	
	    	if (hullList.get(0).size().height > 2) {
	    		MatOfInt hullIndexes = new MatOfInt();
	    		Imgproc.convexHull(largestContour, hullIndexes, true);
	    		
	    		MatOfInt4 convexityDefects = new MatOfInt4();
	    		
	    		Imgproc.convexityDefects(largestContour, hullIndexes, convexityDefects);
	    		
	    		ArrayList<Integer> cdList = new ArrayList<Integer>(convexityDefects.toList());
	    		Point data[] = largestContour.toArray();
	    		
	    		int lineThickness = 2;
	    		Scalar lineColor = new Scalar(255,255,0);

	    		// Draw bounding box
		    	Rect boundingBox = Imgproc.boundingRect(hullList.get(0));
		    	Imgproc.rectangle(highlightImage, new Point(boundingBox.x,boundingBox.y),
		    			new Point(boundingBox.x + boundingBox.width,boundingBox.y + boundingBox.height),
		    			new Scalar(255,0,255),3);
		    	
		    	Point boundingCenterPoint = new Point(boundingBox.x + boundingBox.width / 2, boundingBox.y + boundingBox.height / 2);
	    		
	    		for (int i = 0; i < cdList.size(); i += 4) {
	    			Point start = data[cdList.get(i)];
	    			Point end = data[cdList.get(i+1)];
	    			Point defect = data[cdList.get(i+2)];
	    			
	    			
	    			
	    			double centerAngle = Math.atan2(boundingCenterPoint.y - start.y, boundingCenterPoint.x - start.x) * 180 / Math.PI;
	    			double innerAngle = PointerIdentifier.angleBetweenVectors(start, end, defect);
	    			double length = Math.sqrt((start.x - end.x) * (start.x - end.x) + (start.y - end.y)*(start.y - end.y));
	    			
	    			// This part of the code seems broken. Arbitrarily picking the first point is incorrect
	    			// Should instead look at how to mathematically define which contour you're looking to analyze better
	    			if (centerAngle > 0 && centerAngle < 179 && length > 0.1 * boundingBox.height) {
	    				Imgproc.line(highlightImage, start, defect, lineColor,lineThickness);
		    			Imgproc.line(highlightImage, defect, end, lineColor,lineThickness);
		    			Imgproc.circle(highlightImage, start, 10, lineColor);
		    			return start;
	    			}
	    			
	    		}
	    	}
	    	
	    }
	    
	    return null;
	}

	public static MatOfPoint convertIndexesToPoints(MatOfPoint contour, MatOfInt indexes) {
	    int[] arrIndex = indexes.toArray();
	    Point[] arrContour = contour.toArray();
	    Point[] arrPoints = new Point[arrIndex.length];

	    for (int i=0;i<arrIndex.length;i++) {
	        arrPoints[i] = arrContour[arrIndex[i]];
	    }

	    MatOfPoint hull = new MatOfPoint(); 
	    hull.fromArray(arrPoints);
	    return hull; 
	}
	
	public static double angleBetweenVectors(Point start, Point end, Point defect) {
		
		double dist1 = Math.sqrt( (start.x - defect.x) * (start.x - defect.x) + (start.y - defect.y) * (start.y - defect.y) );
		double dist2 = Math.sqrt( (end.x - defect.x) * (end.x - defect.x) + (end.y - defect.y) * (end.y - defect.y) );
		
		Point A = new Point();
		Point B = new Point();
		Point C = new Point();
		
		C.x = defect.x;
		C.y = defect.y;
		
		if (dist1 < dist2) {
			
			B.x = start.x;
			B.y = start.y;
			A.x = end.x;
			A.y = end.y;
			
		} else {
			
			A.x = start.x;
			A.y = start.y;
			B.x = end.x;
			B.y = end.y;
			
		}
		
		double Q1 = C.x - A.x;
		double Q2 = C.y - A.y;
		double P1 = B.x - A.x;
		double P2 = B.y - A.y;
		
		double returnValue = Math.acos( (P1*Q1 + P2*Q2) / ( Math.sqrt(P1*P1 + P2*P2) + Math.sqrt(Q1*Q1 + Q2*Q2) ) ); 
		
		returnValue = returnValue * 180 / Math.PI;
		
		return returnValue;
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
	    		Imgproc.circle(highlightImage, returnPoint, 10, new Scalar(0,0,255,255),5);
	    	}
	    } 
	    
	    return returnPoint;
	}
	
	public FingerResult getFingerPointer(Mat inputImage, Mat highlightImage, Mat sourceImage) {
		
		if (sourceImage == null) {
			return new FingerResult();
		}
		
		Mat handMask = getHandMaskFromImage(inputImage);
		
		Core.add(inputImage, new Scalar(255,0,0,255), highlightImage, handMask);
		
		// highlightImage is modified inside of fingerPointResult
		Point fingerPointResult = getFingerPointFromMaskSmallestY(inputImage, highlightImage, handMask);

		if (fingerPointResult != null) {
			return new FingerResult(handMask,highlightImage,fingerPointResult);
		} else {
			return new FingerResult();
		}
	}
	
	public static int[][][] getHSVHistogram(Mat HSVInputImage) {
		int numBins = 10;
		
		int[][][] HSVHist = new int[numBins][numBins][numBins];
		
		for (int i = 0; i < numBins; i++) {
			for (int j = 0; j < numBins; j++) {
				for (int k = 0; k < numBins; k++) {
					HSVHist[i][j][k] = 0;
				}
			}
		}
		
		for (int i = 0; i < HSVInputImage.height(); i++) {
			for (int j = 0; j < HSVInputImage.width(); j++) {
				double[] HSVValues = HSVInputImage.get(i, j);
				
				int hueVal = (int) (HSVValues[0] / 180 * numBins);
				int satVal = (int) (HSVValues[1] / 256 * numBins);
				int valVal = (int) (HSVValues[2] / 256 * numBins);
				
				HSVHist[hueVal][satVal][valVal] += 1;
			}
		}
		
		return HSVHist;
	}
	
	public static void print3DArray(int[][][] inputArray) {
		System.out.println("h,s,v,size,neg");
		
		for (int i = 0; i < inputArray.length; i++) {
			for (int j = 0; j < inputArray[i].length; j++) {
				for (int k = 0; k < inputArray[j].length; k++) {
					String printLine = "";
					printLine += i + ",";
					printLine += j + ",";
					printLine += k + ",";
					printLine += Math.abs(inputArray[i][j][k]) + ",";
					if (inputArray[i][j][k] > 0) {
						printLine += "0";
					} else {
						printLine += "1";
					}
					if (inputArray[i][j][k] != 0) {
						System.out.println(printLine);
					}
				}
			}
		}
		
	}
	
	public static int[][][] getDifference3DArray(int[][][] arrayA, int[][][] arrayB) {
		int[][][] returnArray = new int[arrayA.length][arrayA[0].length][arrayA[0][0].length];
		
		for (int i = 0; i < arrayA.length; i++) {
			for (int j = 0; j < arrayA[i].length; j++) {
				for (int k = 0; k < arrayA[j].length; k++) {
					int difference = arrayA[i][j][k] - arrayB[i][j][k];
					
					returnArray[i][j][k] = difference;
				}
			}
		}
		
		return returnArray;
	}
	
	public HSVRange getRangeFromDifferenceArray(int[][][] diffArray) {
		int maxDiff = -1;
		int[] maxDiffIndex = {-1,-1,-1};
		int numBins = diffArray.length;
		double diffTolerance = 0.6;
		
		for (int i = 0; i < diffArray.length; i++) {
			for (int j = 0; j < diffArray[i].length; j++) {
				for (int k = 0; k < diffArray[j].length; k++) {
					if (diffArray[i][j][k] > maxDiff) {
						maxDiff = diffArray[i][j][k];
						maxDiffIndex[0] = i;
						maxDiffIndex[1] = j;
						maxDiffIndex[2] = k;
					}
				}
			}
		}
		
		int minHue;
		int maxHue;
		int minSat;
		int maxSat;
		int minVal;
		int maxVal;
		
		int differenceValue = 0;
		
		minHue = maxDiffIndex[0] - 1;
		maxHue = maxDiffIndex[0] + 1;
		
		minSat = maxDiffIndex[1] - 1;
		maxSat = maxDiffIndex[1] + 1;
		
		minVal = maxDiffIndex[2] - 1;
		maxVal = maxDiffIndex[2] + 1;
		
		if (minHue < 0) {
			minHue = 0;
		}
		
		if (maxHue > numBins - 1) {
			maxHue = 0;
		}
		
		if (minSat < 0) {
			minSat = 0;
		}
		
		if (maxSat > numBins - 1) {
			maxSat = numBins - 1;
		}
		
		if (minVal < 0) {
			minVal = 0;
		}
		
		if (maxVal > numBins- 1) {
			maxVal = numBins - 1;
		}
		
		// Expand Hue
		
		differenceValue = diffArray[minHue][maxDiffIndex[1]][maxDiffIndex[2]];
		
		while (minHue > 0 && differenceValue > diffTolerance * maxDiff) {
			minHue -= 1;
			differenceValue = diffArray[minHue][maxDiffIndex[1]][maxDiffIndex[2]];
		}
		
		differenceValue = diffArray[maxHue][maxDiffIndex[1]][maxDiffIndex[2]];
		
		while (maxHue < numBins - 1 && differenceValue > diffTolerance * maxDiff) {
			maxHue += 1;
			differenceValue = diffArray[maxHue][maxDiffIndex[1]][maxDiffIndex[2]];
		}
		
		// Expand Sat
		
		differenceValue = diffArray[maxDiffIndex[0]][minSat][maxDiffIndex[2]];
		
		while (minSat > 0 && differenceValue > diffTolerance * maxDiff) {
			minSat -= 1;
			differenceValue = diffArray[maxDiffIndex[0]][minSat][maxDiffIndex[2]];
		}
		
		differenceValue = diffArray[maxDiffIndex[0]][maxSat][maxDiffIndex[2]];
		
		while (maxSat < numBins - 1 && differenceValue > diffTolerance * maxDiff) {
			maxSat += 1;
			differenceValue = diffArray[maxDiffIndex[0]][maxSat][maxDiffIndex[2]];
		}
		
		// Expand Sat
		
		differenceValue = diffArray[maxDiffIndex[0]][maxDiffIndex[1]][minVal];
		
		while (minVal > 0 && differenceValue > diffTolerance * maxDiff) {
			minVal -= 1;
			differenceValue = diffArray[maxDiffIndex[0]][maxDiffIndex[1]][minVal];
		}
		
		differenceValue = diffArray[maxDiffIndex[0]][maxDiffIndex[1]][maxVal];
		
		while (maxVal < numBins - 1 && differenceValue > diffTolerance * maxDiff) {
			maxVal += 1;
			differenceValue = diffArray[maxDiffIndex[0]][maxDiffIndex[1]][maxVal];
		}
		
		double hueBinWidth = 180.0 / (numBins - 1);
		double satValBinWidth = 256.0 / (numBins - 1);
		
		int minHueValue = (int) (minHue * hueBinWidth);
		int maxHueValue = (int) (maxHue * hueBinWidth);
		
		int minSatValue = (int) (minSat * satValBinWidth);
		int maxSatValue = (int) (maxSat * satValBinWidth);
		
		int minValValue = (int) (minVal * satValBinWidth);
		int maxValValue = (int) (maxVal * satValBinWidth);
		
		return new HSVRange(minHueValue, maxHueValue, minSatValue, maxSatValue, minValValue, maxValValue);
	}
	
	private HSVRange getHandHSVRange(Mat inputImageHSV, Mat sourceImageHSV) {
		
		int[][][] HSVHistA = PointerIdentifier.getHSVHistogram(sourceImageHSV);
        int[][][] HSVHistB = PointerIdentifier.getHSVHistogram(inputImageHSV);
        int[][][] DiffHist = PointerIdentifier.getDifference3DArray(HSVHistB, HSVHistA);
        
        HSVRange inputRange = getRangeFromDifferenceArray(DiffHist);
		
		return inputRange;
	}
	
}
