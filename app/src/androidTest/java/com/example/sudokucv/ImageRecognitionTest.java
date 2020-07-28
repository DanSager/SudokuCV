package com.example.sudokucv;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ImageRecognitionTest {

    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    static final String TAG = "UnitTest";
    ArrayList<Triplet<Mat, List<String>, String>> list;
    Vision v;

    @Before
    public void loadImages() {
        v = new Vision();
        v.create(appContext);
        list = v.getImages(-1);
    }

    Pair<List<String>, List<String>> testImage(Triplet<Mat, List<String>, String> img) {
        Mat mImages = img.getFirst();
        List<String> mValues = img.getSecond();
        String name = img.getThird();

        ArrayList<Mat> results = v.isolateBoxes(mImages)[0];

        List<String> array = new ArrayList<>();

        for (Mat image : results) {
            if (image == null) {
                array.add("0");
                continue;
            }
            Bitmap b = v.getBitMap(image);

            if (array.size() == 12) {
                Log.i(TAG, "puase");
            }

            String output = v.readText(b);
            if (output.equals("") || output.equals("."))
                output = "0";

            array.add(output);
        }

        return new Pair<>(mValues, array);
    }

//    @Test
//    public void testZeroImage() {
//        Triplet<Mat, List<String>, String> img = list.get(0);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testZeroImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }
//
//    @Test
//    public void testFirstImage() {
//        Triplet<Mat, List<String>, String> img = list.get(1);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testFirstImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }
//
//    @Test
//    public void testSecondImage() {
//        Triplet<Mat, List<String>, String> img = list.get(2);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testSecondImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }
//
//    @Test
//    public void testThirdImage() {
//        Triplet<Mat, List<String>, String> img = list.get(3);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testThirdImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }
//
//    @Test
//    public void testFourthImage() {
//        Triplet<Mat, List<String>, String> img = list.get(4);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testFourthImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }
//
//    @Test
//    public void testFifthImage() {
//        Triplet<Mat, List<String>, String> img = list.get(5);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testFifthImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }
//
//    @Test
//    public void testSixthImage() {
//        Triplet<Mat, List<String>, String> img = list.get(6);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testSixthImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }
//
//    @Test
//    public void testSeventhImage() {
//        Triplet<Mat, List<String>, String> img = list.get(7);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testSeventhImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }
//
//    @Test
//    public void testEighthImage() {
//        Triplet<Mat, List<String>, String> img = list.get(8);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testEighthImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }

//    @Test
//    public void testNinthImage() {
//        Triplet<Mat, List<String>, String> img = list.get(9);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testNinthImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }

    @Test
    public void testTenthImage() {
        Triplet<Mat, List<String>, String> img = list.get(10);
        long startTime = System.currentTimeMillis();

        Pair<List<String>, List<String>> p = testImage(img);

        long duration = (System.currentTimeMillis() - startTime);

        Log.i(TAG, "testTenthImage: " + duration + "ms");

        assertThat(p.first, is(p.second));
    }

//    @Test
//    public void testEleventhImage() {
//        Triplet<Mat, List<String>, String> img = list.get(11);
//        long startTime = System.currentTimeMillis();
//
//        Pair<List<String>, List<String>> p = testImage(img);
//
//        long duration = (System.currentTimeMillis() - startTime);
//
//        Log.i(TAG, "testEleventhImage: " + duration + "ms");
//
//        assertThat(p.first, is(p.second));
//    }

//    @Test
//    public void useAppContext() {
//        // Context of the app under test.
//        assertEquals("com.example.sudokucv", appContext.getPackageName());
//    }

}