package com.arrata.user.opencvcamera;

import android.util.Log;

import org.opencv.core.Mat;

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



}
