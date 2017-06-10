package com.arrata.user.opencvcamera;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by USER on 2017-05-23.
 */

public class Utility {
    static void iterateMat(Mat mat) {
        int whiteCount = 0;
        int blackCount = 0;
        double blue = 0.0, green = 0.0, red = 0.0;

        for(int i = 0; i < mat.rows(); ++i) {
            for(int j = 0; j < mat.cols(); ++j) {
                double values[] = mat.get(i, j);
                blue = values[0];
                //green = values[1];
                //red = values[2];

            }
        }

        Log.d("iterateBlue", String.valueOf(blue));
        Log.d("iterateGreen", String.valueOf(green));
        Log.d("iterateRed", String.valueOf(red));

    }

    static void getPatch(Mat mat) {


    }

    static void head(Mat mat) {
        //columns are the horizontal lines for a portrait camera view
        int startcol = 0;
        int endcol = 0;

        int whitecount[] = new int[mat.rows()];//Count the number of whites per column
        whitecount[0] = 0;
        int blackcount = 0;

        int whitestart = -1;
        int whiteend = -1;
        double values[];

        int columncount[] = new int[mat.cols()];//Count the number of whites per column

        for(int i = 0; i < mat.cols(); ++i) {
            whitestart = -1;
            whiteend = -1;
            for(int j = 1; j < mat.rows(); ++j) {
                startcol += 1;
                values = mat.get(j, i);
                if(values[0] == 0) {
                    whitecount[j] = whitecount[j - 1] + 1;
                    if(whitestart == -1) {
                        whitestart = j;
                    }
                    blackcount = 0;
                } else {
                    if(blackcount == 0) {
                        whiteend = j;
                    }
                    blackcount += 1;
                    if(blackcount > mat.rows() / 10) {
                        whitestart = -1;
                    }
                }
            }
            if(whitestart != -1 && whiteend != -1 && whitestart < whiteend) {
                columncount[i] = whitecount[whiteend] - whitecount[whitestart];
            }
        }

        boolean seen = false;
        boolean bottom = false;
        for(int i = 0; i < mat.cols(); ++i) {
            if(!seen && columncount[i] > 0) {
                seen = true;
                for(int j = 0; j < mat.rows(); ++j) {
                    double val[] = {127.0};
                    mat.put(j, i, val);
                }
            } /*else if(seen && !bottom && columncount[i] == 0) {
                for(int j = 0; j < mat.rows(); ++j) {
                    double val[] = {127.0};
                    mat.put(j, i, val);
                    bottom = true;
                }
            }*/
        }

        for(int i = mat.cols() - 1; i >= 0; --i) {
            if(Utility.regions(mat, i) > 1) {
                for(int j = 0; j < mat.rows(); ++j) {
                    double val[] = {127.0};
                    mat.put(j, i, val);
                }
                break;
            }
        }


        for(int i = 0; i < mat.rows(); ++i) {
           // Log.d("Counter", String.valueOf(i) + " rows " + String.valueOf(columncount[i]));
            double val[] = {127.0};
            mat.put(i, 0, val);

        }
    }

    static int regions(Mat mat, int col) {
        boolean waswhite = false;
        int regions = 0;
        for(int i = 0; i < mat.rows(); ++i) {
            if(mat.get(i, col)[0]  > 0) {
                waswhite = true;
            }else if(waswhite && mat.get(i, col)[0] == 0) {
                regions++;
                waswhite = false;
            }
        }
        return regions;
    }

    static void whiteContours(Mat flipped) {
        Mat mHierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(flipped, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        //blurring is necessary

        //Photo.fastNlMeansDenoising(flipped, flipped);

        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal > contourArea)
            {
                Imgproc.fillPoly(flipped, Arrays.asList(contours.get(contourIdx)), new Scalar(0));
            } else {
                //Imgproc.fillPoly(flipped, Arrays.asList(contours.get(contourIdx)), new Scalar(255));
            }
        }

        Imgproc.drawContours(flipped, contours, maxValIdx, new Scalar(255), 1);

    }

    static void removeNoise(Mat flipped) {
        Mat mHierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(flipped, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        Imgproc.drawContours(flipped, contours, maxValIdx, new Scalar(0), 3);

        //find contours again
        Imgproc.findContours(flipped, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }

        Imgproc.drawContours(flipped, contours, maxValIdx, new Scalar(255), 3);
    }
}
