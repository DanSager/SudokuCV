package com.example.sudokucv;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs; // imread, imwrite, etc
import org.opencv.videoio.Videoio; // VideoCapture

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "MainActivity";
    Handler mainHandler = new Handler();
    private String[] values = null;
    ArrayList<Mat> steps = new ArrayList<>();
    private int stepsIterator = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;
    Context c;

    ImageView imageView;
    Uri imageUri;
    ContentValues cntVals;
    String mCameraFileName;

    /*
    *   TODO:
    *       X Update camera usage so a new picture isn't stored
    *       Crop camera to be 1:1
    *       Optimize code in main activity
    *       Print when image can not be read
    *       Improve visuals
    *       Fix isolateBoxes algorithm so there aren't any necessary exception points
    *       Add incorrect functionality
    *
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up permissions and ocr
        checkPermission();
        c = getApplicationContext();

//        Vision v = new Vision();
//        v.create(getBaseContext());

//        // Temp load image, will be replaced later with capture image
//        Triplet<Mat, List<String>, String> image = loadImage(v);
//
//        // Display image
//        Mat mat = image.getFirst();
//        Bitmap bmp = v.getBitMap(mat);
//        updateImageView(bmp);
//
//        // Process image
//        ProcessImage pi = new ProcessImage(v, mat);
//        pi.start();

        // ############################
        // Execute included tests
        //ExecuteTests tests = new ExecuteTests(v);
        //tests.start();
    }

    public void takePicture(View view) { // onClick from begin
        cntVals = new ContentValues();
        cntVals.put(MediaStore.Images.Media.TITLE, "New Picture");
        cntVals.put(MediaStore.Images.Media.DESCRIPTION, "From your camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cntVals);

        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        camera.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(camera, CAMERA_REQUEST_CODE);
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private Bitmap rotateBitmap(String imgUrl, Bitmap bmp) {
        try {
            ExifInterface ei = new ExifInterface(imgUrl);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
            }
            else if (orientation == 3) {
                matrix.postRotate(180);
            }
            else if (orientation == 8) {
                matrix.postRotate(270);
            }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true); // rotating bitmap
        } catch (IOException e) {
            Log.e(TAG, "Error in rotateBitmap: " + e.getMessage());
            Toast.makeText(getApplicationContext(), "Error at reading image rotation", Toast.LENGTH_SHORT).show();
        }
        return bmp;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Read values
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                Bitmap bmp = null;
                try {
                    // Attempt to load image we just took
                    bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                } catch (IOException e) {
                    Log.e(TAG, "Error in onActivityResult1: " + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Error at reading image", Toast.LENGTH_SHORT).show();
                }

                try {

                    if (bmp != null) {
                        // Rotate image
                        String imageUrl = getRealPathFromURI(imageUri);
                        bmp = rotateBitmap(imageUrl, bmp);

                        // Delete the new image from storage
                        File fdelete = new File(imageUrl);
                        if (fdelete.exists())
                            if (fdelete.delete())
                                Log.i(TAG, "Deleted image at " + imageUrl);

                        // Update the image view to contain the image
                        imageView = findViewById(R.id.img);
                        imageView.setImageBitmap(bmp);
                        imageView.setVisibility(View.VISIBLE);

                        // Convert bitmap to Mat
                        Mat m = Vision.getMat(bmp);

                        // Process image / Continue execution
                        Toast.makeText(getApplicationContext(), "Begin processing image", Toast.LENGTH_SHORT).show();
                        ProcessImage pi = new ProcessImage(m);
                        pi.start();
                    } else {
                        // Image is null. Return to previous state
                    }
                } catch (Exception e) {
                    Log.i(TAG,  "Error in onActivityResult2: " + e.getMessage());
                }
            }
        }
    }

    public void start(View view) { // onClick from start
        // Open preview screen
        if (values == null) {
            Toast.makeText(getApplicationContext(), "Try again, null", Toast.LENGTH_SHORT).show();
        } else if (values.length != 81) {
            Toast.makeText(getApplicationContext(), "Try again, != 81", Toast.LENGTH_SHORT).show();
        } else {
            //String[] values = new String[] {"8","0","0","1","0","0","4","0","0","0","0","0","0","0","6","0","0","2","0","3","0","9","0","0","1","7","0","0","0","0","0","0","3","0","9","1","0","0","8","0","0","0","2","0","0","9","1","0","4","0","0","0","0","0","0","9","4","0","0","2","0","3","0","6","0","0","3","0","0","0","0","0","0","0","3","0","0","7","0","0","4"};
            Intent intent = new Intent(getApplicationContext(), PreviewActivity.class);
            intent.putExtra("values", values);
            startActivity(intent);
        }
    }

    public void debug(View view) {
        if (stepsIterator >= steps.size()) {
            stepsIterator = 0;
        }

        Mat img = steps.get(stepsIterator);
        stepsIterator++;

        Bitmap img_bitmap = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, img_bitmap);
        updateImageView(img_bitmap);
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

    private void toast(String incoming) {
        final String msg = incoming;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void makeVisible(){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Button start = (Button) findViewById(R.id.start);
                start.setVisibility(View.VISIBLE);
            }
        });
    }

    private Triplet<Mat, List<String>, String> loadImage(Vision v) {
        ArrayList<Triplet<Mat, List<String>, String>> images = v.getImages(0);
        return images.get(0);
    }

    public class ProcessImage extends Thread {
        Mat mat;

        ProcessImage(Mat mRef) {
            this.mat = mRef;
        }

        @Override
        public void run() {
            try {
                // Process
                String[] sudokuDefaults = processImage(mat);
                if (sudokuDefaults == null) {
                    toast("Could not properly read the sudoku, try again");
                    return;
                } else {
//                    Button start = (Button) findViewById(R.id.start);
//                    start.setVisibility(View.VISIBLE);
                    makeVisible();
                }

                values = sudokuDefaults;
            } catch (Exception e) {
                Log.e(TAG, "Error in run: " + e.getMessage());
            }
        }

        private String[] processImage (Mat img) {
            long startTime = System.currentTimeMillis();
            Vision v = new Vision();
            v.create(getApplicationContext());

            ArrayList<Mat>[] boxes = v.isolateBoxes(img);

            if (boxes == null) return null;

            if (boxes[0].size() == 0) {
                // Couldn't read the sudoku board correctly
                return null;
            }

            ArrayList<Mat> numImgs = boxes[0];
            steps = boxes[1];

            String[] array = new String[numImgs.size()];

            int i = 0;
            for (Mat numImg : numImgs) {
                if (numImg == null) {
                    array[i++] = "0";
                    continue;
                }
                Bitmap b = v.getBitMap(numImg);

                String output = v.readText(b);
                if (output.equals("") || output.equals("."))
                    output = "0";

                array[i++] = output;
            }

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime);
            if (array.length == 81) {
                toast("Ready");
                Log.i(TAG, "Processed image. Duration: " + duration + "ms");
                return array;
            } else {
                toast("unable to successfully process image, array length equals " + array.length);
            }
            return null;
        }
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

                ArrayList<Mat> results = v.isolateBoxes(mImages)[0];

                List<String> array = new ArrayList<>();

                int j = 0;
                for (Mat image : results) {
                    if (image == null) {
                        array.add("0");
                        continue;
                    }
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
