package com.openreadandroid.openread;

import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgcodecs.Imgcodecs;

public class ScreenIdentifier {

	public class matchResult {
		public Boolean matchFound;
		public Mat outImage;
		public Mat highlightImage;
		public Mat matchImage;

		public matchResult(Mat inOutImage, Mat inHLImage, Mat inMatchImage) {
			outImage = inOutImage;
			highlightImage = inHLImage;
			matchImage = inMatchImage;
			matchFound = true;
		}
		
		public matchResult() {
			outImage = null;
			highlightImage = null;
			matchImage = null;
			matchFound = false;
		}
		
	}

	private Mat refImage = null;

	private Boolean refSet = false;

	public ScreenIdentifier() {

	}

	public matchResult matchImage(Mat inputMat) {

		if (refSet) {

			Mat objectImage = refImage;
			Mat sceneImage = inputMat;

			MatOfKeyPoint objectKeyPoints = new MatOfKeyPoint();
			FeatureDetector featureDetector = FeatureDetector.create(FeatureDetector.BRISK);
			//System.out.println("Detecting key points...");
			featureDetector.detect(objectImage, objectKeyPoints);
			KeyPoint[] keypoints = objectKeyPoints.toArray();
			//System.out.println(keypoints);

			MatOfKeyPoint objectDescriptors = new MatOfKeyPoint();
			DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.BRISK);
			//System.out.println("Computing descriptors...");
			descriptorExtractor.compute(objectImage, objectKeyPoints, objectDescriptors);

			// Create the matrix for output image.
			Mat outputImage = new Mat(objectImage.rows(), objectImage.cols(), Imgcodecs.CV_LOAD_IMAGE_COLOR);
			Scalar newKeypointColor = new Scalar(255, 0, 0);

			//System.out.println("Drawing key points on object image...");
			Features2d.drawKeypoints(objectImage, objectKeyPoints, outputImage, newKeypointColor, 0);

			// Match object image with the scene image
			MatOfKeyPoint sceneKeyPoints = new MatOfKeyPoint();
			MatOfKeyPoint sceneDescriptors = new MatOfKeyPoint();
			//System.out.println("Detecting key points in background image...");
			featureDetector.detect(sceneImage, sceneKeyPoints);
			//System.out.println("Computing descriptors in background image...");
			descriptorExtractor.compute(sceneImage, sceneKeyPoints, sceneDescriptors);

			Mat matchoutput = new Mat(sceneImage.rows() * 2, sceneImage.cols() * 2, Imgcodecs.CV_LOAD_IMAGE_COLOR);
			Scalar matchestColor = new Scalar(0, 255, 0);

			List<MatOfDMatch> matches = new LinkedList<MatOfDMatch>();
			DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMINGLUT);
			//System.out.println("Matching object and scene images...");
			descriptorMatcher.knnMatch(objectDescriptors, sceneDescriptors, matches, 2);

			//System.out.println("Calculating good match list...");
			LinkedList<DMatch> goodMatchesList = new LinkedList<DMatch>();

			float nndrRatio = 0.7f;

			for (int i = 0; i < matches.size(); i++) {
				MatOfDMatch matofDMatch = matches.get(i);
				DMatch[] dmatcharray = matofDMatch.toArray();
				DMatch m1 = dmatcharray[0];
				DMatch m2 = dmatcharray[1];

				if (m1.distance <= m2.distance * nndrRatio) {
					goodMatchesList.addLast(m1);

				}
			}

			if (goodMatchesList.size() >= 7) {
				//System.out.println("Object Found!!!");

				List<KeyPoint> objKeypointlist = objectKeyPoints.toList();
				List<KeyPoint> scnKeypointlist = sceneKeyPoints.toList();

				LinkedList<Point> objectPoints = new LinkedList<>();
				LinkedList<Point> scenePoints = new LinkedList<>();

				for (int i = 0; i < goodMatchesList.size(); i++) {
					objectPoints.addLast(objKeypointlist.get(goodMatchesList.get(i).queryIdx).pt);
					scenePoints.addLast(scnKeypointlist.get(goodMatchesList.get(i).trainIdx).pt);
				}

				MatOfPoint2f objMatOfPoint2f = new MatOfPoint2f();
				objMatOfPoint2f.fromList(objectPoints);
				MatOfPoint2f scnMatOfPoint2f = new MatOfPoint2f();
				scnMatOfPoint2f.fromList(scenePoints);

				Mat homography = Calib3d.findHomography(objMatOfPoint2f, scnMatOfPoint2f, Calib3d.RANSAC, 3);

				Mat obj_corners = new Mat(4, 1, CvType.CV_32FC2);
				Mat scene_corners = new Mat(4, 1, CvType.CV_32FC2);

				obj_corners.put(0, 0, new double[] { 0, 0 });
				obj_corners.put(1, 0, new double[] { objectImage.cols(), 0 });
				obj_corners.put(2, 0, new double[] { objectImage.cols(), objectImage.rows() });
				obj_corners.put(3, 0, new double[] { 0, objectImage.rows() });
				
				//System.out.println("Transforming object corners to scene corners...");
				Core.perspectiveTransform(obj_corners, scene_corners, homography);
				
				Mat transformMat = Imgproc.getPerspectiveTransform(scene_corners, obj_corners);

				Mat outputMat = new Mat();

				Imgproc.warpPerspective(sceneImage, outputMat, transformMat, objectImage.size());

				Mat img = inputMat.clone();

				Imgproc.line(img, new Point(scene_corners.get(0, 0)), new Point(scene_corners.get(1, 0)),
						new Scalar(0, 255, 0), 4);
				Imgproc.line(img, new Point(scene_corners.get(1, 0)), new Point(scene_corners.get(2, 0)),
						new Scalar(0, 255, 0), 4);
				Imgproc.line(img, new Point(scene_corners.get(2, 0)), new Point(scene_corners.get(3, 0)),
						new Scalar(0, 255, 0), 4);
				Imgproc.line(img, new Point(scene_corners.get(3, 0)), new Point(scene_corners.get(0, 0)),
						new Scalar(0, 255, 0), 4);

				//System.out.println("Drawing matches image...");
				MatOfDMatch goodMatches = new MatOfDMatch();
				goodMatches.fromList(goodMatchesList);

				Features2d.drawMatches(objectImage, objectKeyPoints, sceneImage, sceneKeyPoints, goodMatches,
						matchoutput, matchestColor, newKeypointColor, new MatOfByte(), 2);

				// Imgcodecs.imwrite(".\\output\\outputImage.jpg", outputImage);
				// Imgcodecs.imwrite(".\\output\\matchoutput.jpg", matchoutput);
				// Imgcodecs.imwrite(".\\output\\img.jpg", img);
				return new matchResult(outputMat, img, matchoutput);
			} else {
				//System.out.println("Object Not Found");
			}
		}
		return new matchResult();
	}

	public void setRefImage(Mat inputMat) {
		refImage = inputMat;
		refSet = true;
	}

	public Mat getRefImage() {
		if (refImage == null) {
			return null;
		} else {
			return refImage.clone();
		}
	}

}
