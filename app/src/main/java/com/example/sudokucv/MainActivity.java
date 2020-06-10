package com.example.sudokucv;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

//import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONArray;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs; // imread, imwrite, etc
import org.opencv.videoio.Videoio; // VideoCapture

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";
    public static final String TESS_DATA = "tessdata";

    //    static {
//        if (OpenCVLoader.initDebug()) {
//            Log.d(TAG, "success");
//        } else {
//            Log.d(TAG,"unsuccess");
//        }
//    }
//
//    // Used to load the 'native-lib' library on application startup.
    static {
        //System.loadLibrary("native-lib");
        //System.loadLibrary("opencv_java3");
        //System.loadLibrary("jpgt");
        //System.loadLibrary("pngt");
        //System.loadLibrary("lept");
        //System.loadLibrary("tess");
    }
//
//    private static void loadLibraries() {
//        if (OpenCVLoader.initDebug()) {
//            Log.d(TAG, "success");
//        } else {
//            Log.d(TAG,"unsuccess");
//        }
//
//        System.loadLibrary("native-lib");
//        System.loadLibrary("opencv_java3");
//        System.loadLibrary("jpgt");
//        System.loadLibrary("pngt");
//        System.loadLibrary("lept");
//        System.loadLibrary("tess");
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        //TextView tv = findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        new loadTask().execute();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
    }

    private class loadTask extends AsyncTask<Void, Bitmap, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            checkPermission();
            //prepareFiles("",TESS_DATA);
            Vision v = new Vision();
            v.create(getBaseContext());
            ArrayList<Triplet<Mat, List<String>, String>> images = v.getImages(-1);
            int i = 0;

            for (Triplet<Mat, List<String>, String> t : images) {
                Mat mImages = t.getFirst();
                List<String> mValues = t.getSecond();
                String name = t.getThird();

                ArrayList<Mat> results = v.isolateBoxes(mImages);

                List<String> array = new ArrayList<>();

                for (Mat image : results) {
                    Bitmap b = v.getBitMap(image);
                    publishProgress(b);
                    String output = v.readText(b);
                    if (output.equals(""))
                        output = "0";

                    array.add(output);

                    //SystemClock.sleep(200);
                }

                Log.i(TAG, "Image " + name + " result: " + Boolean.toString(mValues.equals(array)));

                //build(array, i);
                i++;
            }

            return null;
        }

        private void build(List<String> array, int i) {
            String path = getExternalFilesDir("/").getPath() + "/" + "testfiles" + "/" + "puzzle" + Integer.toString(i) + ".json";
            try {
                // create a writer
                Writer writer = new FileWriter(path);

                // convert map to JSON File
                new Gson().toJson(array, writer);

                // close the writer
                writer.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        protected void onProgressUpdate(Bitmap... values) {
            Bitmap b = values[0];

            ImageView view = findViewById(R.id.img);
            view.setImageBitmap(b);

            super.onProgressUpdate();
        }
    }
}
