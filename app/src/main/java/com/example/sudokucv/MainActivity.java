package com.example.sudokucv;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
    Handler mainHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up permissions and ocr
        checkPermission();
        Vision v = new Vision();
        v.create(getBaseContext());

        // Execute included tests
        //ExecuteTests tests = new ExecuteTests(v);
        //tests.start();

        // Get image values


        // Open preview screen
        String[] values = new String[] {"8","0","0","1","0","0","4","0","0","0","0","0","0","0","6","0","0","2","0","3","0","9","0","0","1","7","0","0","0","0","0","0","3","0","9","1","0","0","8","0","0","0","2","0","0","9","1","0","4","0","0","0","0","0","0","9","4","0","0","2","0","3","0","6","0","0","3","0","0","0","0","0","0","0","3","0","0","7","0","0","4"};
        Intent intent = new Intent(getApplicationContext(), PreviewActivity.class);
        intent.putExtra("values", values);
        startActivity(intent);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 120);
        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 121);
        }
    }

    private void updateImageView(Bitmap bm) {
        final Bitmap map = bm;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                ImageView view = findViewById(R.id.img);
                view.setImageBitmap(map);
            }
        });
    }

    public class ExecuteTests extends Thread {
        Vision v;

        ExecuteTests(Vision vRef) {
            this.v = vRef;
        }

        @Override
        public void run() {
            ArrayList<Triplet<Mat, List<String>, String>> images = v.getImages(1);

            for (Triplet<Mat, List<String>, String> t : images) {
                long startTime = System.currentTimeMillis();

                Mat mImages = t.getFirst();
                List<String> mValues = t.getSecond();
                String name = t.getThird();

                ArrayList<Mat> results = v.isolateBoxes(mImages);

                List<String> array = new ArrayList<>();

                int j = 0;
                for (Mat image : results) {
                    Bitmap b = v.getBitMap(image);
                    updateImageView(b);

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

                if (mValues.size() == 0 && array.size() == 81)
                    makeJson(array, name);
            }
        }

        private void makeJson(List<String> array, String name) {
            String path = getExternalFilesDir("/").getPath() + "/" + "testfiles" + "/" + name + ".json";
            try {
                Writer writer = new FileWriter(path);

                new Gson().toJson(array, writer);

                writer.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            Log.i(TAG, "wrote new file for: " + name + ".json");
        }

    }

}
