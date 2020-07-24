package com.example.sudokucv;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    static final String TAG = "UnitTest";
    ArrayList<Triplet<Mat, List<String>, String>> list;
    Vision v;

    @Before
    public void loadImages() {
        v = new Vision();
        v.create(appContext);
        list = v.getImages(0);
    }

    @Test
    public void testVision() {

        for (Triplet<Mat, List<String>, String> t : list) {
            long startTime = System.currentTimeMillis();

            Mat mImages = t.getFirst();
            List<String> mValues = t.getSecond();
            String name = t.getThird();

            ArrayList<Mat> results = v.isolateBoxes(mImages)[0];

            List<String> array = new ArrayList<>();

            int j = 0;
            for (Mat image : results) {
                Bitmap b = v.getBitMap(image);

                String output = "";
                output = v.readText(b);
                if (output.equals("") || output.equals("."))
                    output = "0";

                array.add(output);

                if (mValues.size() >= j && mValues.size() > 0) {
                    if (!output.equals(mValues.get(j))) {
                        Log.i(TAG, "box " + j + " is incorrect. predicted: " + output + " expected: " + mValues.get(j));
                    }
                }
                j++;
            }

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime);

            Log.i(TAG, "Image " + name + " result: " + Boolean.toString(mValues.equals(array)) + " in " + duration + "ms");

        }
    }

    @Test
    public void useAppContext() {
        // Context of the app under test.
        assertEquals("com.example.sudokucv", appContext.getPackageName());
    }
}