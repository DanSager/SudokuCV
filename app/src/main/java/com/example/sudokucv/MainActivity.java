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
import android.util.Log;
import android.widget.ImageView;

import com.google.gson.Gson;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs; // imread, imwrite, etc
import org.opencv.videoio.Videoio; // VideoCapture

import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
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

                int j = 0;
                for (Mat image : results) {
                    Bitmap b = v.getBitMap(image);
                    publishProgress(b);
//                    String output = v.readText(b);
//                    String realpred = output + " real";
//                    if (output.equals(""))
//                        output = "0";
//
//                    array.add(output);
//
//                    if (array.size() == 46) {
//                        //Log.i(TAG,"a");
//                    }
//
//                    if (mValues.size() >= j && mValues.size() > 0) {
//                        if (!output.equals(mValues.get(j))) {
//                            Log.i(TAG, "box " + j + " is incorrect. predicted: " + output + " expected: " + mValues.get(j));
//                        }
//                    }
                    j++;

                    //SystemClock.sleep(2);
                }

                Log.i(TAG, "Image " + name + " result: " + Boolean.toString(mValues.equals(array)));

                if (mValues.size() == 0 && array.size() == 81)
                    build(array, name);
                i++;
            }

            return null;
        }

        private void build(List<String> array, String name) {
            String path = getExternalFilesDir("/").getPath() + "/" + "testfiles" + "/" + name + ".json";
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
            Log.i(TAG, "wrote new file for: " + name + ".json");
        }

        protected void onProgressUpdate(Bitmap... values) {
            Bitmap b = values[0];

            ImageView view = findViewById(R.id.img);
            view.setImageBitmap(b);

            super.onProgressUpdate();
        }
    }
}
