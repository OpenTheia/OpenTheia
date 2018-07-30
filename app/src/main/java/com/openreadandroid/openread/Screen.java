package com.openreadandroid.openread;

import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.text.TextBlock;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

// Screen Class - Created 2018-01-13
// Stores a list of screen elements that composes the info of a screen

public class Screen {

    // Element list. Should be read only, nothing should be removed from the list
    private ArrayList<ScreenElement> elements;

    // TAG used for debugging purposes
    private static final String TAG = "Screen";

    private int screenWidth;
    private int screenHeight;

    public Screen() {
        elements = new ArrayList<ScreenElement>();
    }

    public void GenerateScreen(SparseArray<TextBlock> textBlocks, int screenWidth, int screenHeight) {

        elements.clear();

        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));

            elements.add(generateScreenElement(textBlock, screenWidth, screenHeight, elements.size()));
        }

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

    }

    private ScreenElement generateScreenElement(TextBlock textBlock, int screenWidth, int screenHeight, int inId) {

        Rect boundingRect = textBlock.getBoundingBox();

        double xpos = boundingRect.left * 1.0 / screenWidth;
        double ypos = boundingRect.top * 1.0 / screenHeight;
        double width = boundingRect.width() * 1.0 / screenWidth;
        double height = boundingRect.height() * 1.0 / screenHeight;

        // Removed padding for now
        /*
        double w_pad = calculate_padding(width);
        double h_pad = calculate_padding(height);

        xpos -= w_pad;
        ypos -= h_pad;
        width += 2*w_pad;
        height +=2*h_pad;
        if(width > 1) width = 1;
        if(height> 1) height = 1;
        if(xpos < 0) xpos = 0;
        if(ypos < 0) ypos = 0;
        */

        Log.d(TAG,"Increasing width and height");

        Log.w(TAG,"New Screen Element: " + xpos + " , " + ypos + " , " +
                width + " , " + height + " , " + textBlock.getValue() + " id: " + inId);

        ScreenElement returnElement = new ScreenElement(xpos, ypos, width, height,textBlock.getValue(), inId);

        return returnElement;
    }

    private double calculate_padding(double element_size)
    {
        Log.d(TAG,"Calculating padding");
        double m = -1;
        double b = 0.6; // 60%
        double percent_pad = element_size * m + b;
        if(percent_pad < 0) percent_pad = 0;
        double pad = percent_pad * element_size;
        return pad;
    }

    public ScreenElement GetElementAtPoint(double x, double y) {

        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).IsLocationWithinElement(x,y)) {
                return elements.get(i);
            }
        }

        // If nothing was found just return null

        return null;
    }

    public ScreenElement GetElementAtPoint(double x, double y,double radius) {

        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).IsLocationWithinElement(x,y,radius / screenWidth,radius / screenHeight)) {
                return elements.get(i);
            }
        }

        // If nothing was found just return null

        return null;
    }

    public ArrayList<ScreenElement> getAllElements() {
        return elements;
    }

    public void highlightAllScreenElements(Mat inputImage) {

        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                ScreenElement visitor = elements.get(i);

                highlightScreenElement(inputImage,visitor, new Scalar(255,visitor.getHoverValue() * 255,0,255));

            }
        }
    }

    public void highlightScreenElement(Mat inputImage, ScreenElement visitor, Scalar color) {

        if (visitor != null) {

            org.opencv.core.Point[] openCVPointList = new org.opencv.core.Point[4];

            double leftX = visitor.getX_base() * inputImage.width();
            double topY = visitor.getY_base() * inputImage.height();
            double rightX = (visitor.getX_base() + visitor.getX_Width())* inputImage.width();
            double bottomY = (visitor.getY_base() + visitor.getY_length()) * inputImage.height();

            openCVPointList[0] = new org.opencv.core.Point(leftX,topY);
            openCVPointList[1] = new org.opencv.core.Point(rightX,topY);
            openCVPointList[2] = new org.opencv.core.Point(rightX,bottomY);
            openCVPointList[3] = new org.opencv.core.Point(leftX,bottomY);

            for (int j = 0; j < 4; j++) {
                Imgproc.line(inputImage,openCVPointList[j],openCVPointList[(j+1)%4], color, 3);
            }
        }
    }

    public void decrementAllScreenElements() {

        if (elements != null) {
            for (int i = 0; i < elements.size(); i++) {
                ScreenElement visitor = elements.get(i);

                visitor.decrementHover();
            }
        }
    }

}
