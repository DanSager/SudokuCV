package com.example.sudokucv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.core.Point;

import java.util.List;

public class PreviewActivity extends AppCompatActivity {

    static final String TAG = "PreviewActivity";
    private Handler handler = new Handler();
    private String[] solved = null;
    private Bitmap template;
    List<Point> points;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

//        // Get array of input sudoku values
//        String[] values = null;
//        Bundle b = getIntent().getExtras();
//        if (b != null) {
//            values = b.getStringArray("values");
//        }
//
//        // Get sudoku template
//        template = BitmapFactory.decodeResource(getResources(), R.drawable.sudokutemplate);
//
//        // Fill template with default values
//        Vision v = new Vision();
//        points = v.findCenterPoints(template);
//        Bitmap defaults = v.writeInValues(template, values, points);
//        updateImageView(defaults);
//
//        // Retrieve the completed sudoku values
//        try {
//            solved = Sudoku.main(values);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public void correct(View view) throws InterruptedException {
        // Bad busy-wait
        while (solved == null) {
            Thread.sleep(10);
            Log.i(TAG, "waiting");
        }

        Vision v = new Vision();
        Bitmap completed = v.writeInValues(template, solved, points);
        updateImageView(completed);
    }

    public void incorrect(View view) throws InterruptedException {

    }

    private void updateImageView(Bitmap bm) {
        final Bitmap map = bm;
        handler.post(new Runnable() {
            @Override
            public void run() {
                ImageView view = findViewById(R.id.img);
                view.setImageBitmap(map);
            }
        });
    }
}
