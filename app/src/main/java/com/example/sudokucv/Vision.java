package com.example.sudokucv;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.gson.Gson;
import com.googlecode.tesseract.android.TessBaseAPI;
//import com.zsmarter.*;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.core.CvType.CV_8U;

public class Vision {

    static final String TAG = "Vision";
    private boolean testing = false;
    Context context = null;
    final String TEST_FILES = "testfiles";
    final String TESS_DATA = "tessdata";
    TessBaseAPI baseApi = null;

    static {
        OpenCVLoader.initDebug();
    }

    public int create(Context con) {
        context = con;
        if (context.getPackageName().contains("test")) {
            testing = true;
        }

        prepareFiles("", TESS_DATA);
        prepareFiles(TEST_FILES, TEST_FILES);
        loadTesseract();
        return 1;
    }

    private void prepareFiles(String path, String files_dir){
        try{
            File dir = context.getExternalFilesDir(files_dir);
            if(!dir.exists()){
                if (!dir.mkdir()) {
                    Toast.makeText(context.getApplicationContext(), "The folder " + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
                }
            }
            String fileList[] = context.getAssets().list(path);
            if (!path.equals(""))
                path = path + "/";
            for(String fileName : fileList){
                String pathToDataFile = dir + "/" + fileName;
                File f = new File(pathToDataFile);
                f.mkdir();
                if (f.isDirectory())
                    continue;

                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = context.getAssets().open(path + fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: prepareFiles - " + e.getMessage());
        }
    }

    public ArrayList<Triplet<Mat, List<String>, String>> getImages(int index) {
        boolean single = (index > -1);
        //externalDir = "/storage/emulated/0/Android/data/com.example.sudokucv/files";
        String externalDir = context.getExternalFilesDir("/").getPath();

        //ArrayList<Pair<Mat, List<String>>> images = new ArrayList<>();
        ArrayList<Triplet<Mat, List<String>, String>> images = new ArrayList<>();

        try {
            File directory = new File(externalDir + "/" + TEST_FILES + "/");
            File[] files = directory.listFiles();
            for (File f : files) {
                if (f.getName().contains(".png") || f.getName().contains(".jpg")) {
                    Mat image = null;
                    List<String> values = new ArrayList<>();

                    String imagePath = f.getPath();
                    String fileNamePath = imagePath.substring(0, imagePath.lastIndexOf('.'));
                    String fileName = fileNamePath.substring(imagePath.lastIndexOf('/') + 1);
                    String jsonPath = fileNamePath + ".json";

                    if (!single || (single && f.getName().contains(Integer.toString(index))))
                        image = Imgcodecs.imread(imagePath);

                    File temp = new File(jsonPath);
                    if(temp.exists()) {
                        values = parse(jsonPath);
                    }

                    Triplet<Mat, List<String>, String> t = new Triplet<>(image, values, fileName);
                    images.add(t);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }

        return images;
    }

    public List<String> parse(String path) {
        String[] data = null;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            Gson gson = new Gson();
            data = gson.fromJson(reader, String[].class);
        } catch (FileNotFoundException ex) {
            Log.e(TAG, ex.getMessage());
        }
        List l = Arrays.asList(data);
        return l;
    }

    public ArrayList<Mat> isolateBoxes(Mat image) {
        ArrayList<Mat> boxes = new ArrayList<>();
        List<MatOfPoint> contents = new ArrayList<>();
        List<MatOfPoint> squares = new ArrayList<>();
        List<MatOfPoint> noise = new ArrayList<>();

        List<List<MatOfPoint>> sortedSquares = new ArrayList<>();
        List<MatOfPoint> row = new ArrayList<>();

        Mat original = image.clone();

        // Make original colors, OpenCV uses BGR
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB);

        // Make black and white (grayscale)
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        // adaptive threshold
        Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 57, 5);
        //Imgproc.adaptiveThreshold(backup, backup, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 57, 5);

        Mat backup = image.clone();

        // Filter out all numbers and noise to isolate
        Imgproc.findContours(image, contents, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Scalar black = new Scalar(0, 0, 0);

        Scalar white = new Scalar(255, 255, 255);
        for (MatOfPoint contour : contents) {
            Double area = Imgproc.contourArea(contour);
            if (area < 1000) {
                Imgproc.drawContours(image, Arrays.asList(contour), -1, black, -1);
            }
            if (area < 75) {
                noise.add(contour);
            }
        }

        // Add back in numbers
        //Mat backup = image.clone();
        Mat invert = new Mat(image.rows(),image.cols(), image.type(), new Scalar(255,255,255));
        Core.subtract(invert, backup, backup);
        for (MatOfPoint contour : noise) {
            Imgproc.drawContours(backup, Arrays.asList(contour), -1, white, -1);
        }

        // Fix horizontal and vertical lines
        Size sv = new Size(1, 5);
        Mat vertical_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, sv);
        Imgproc.morphologyEx(image, image, Imgproc.MORPH_CLOSE, vertical_kernel, new Point(vertical_kernel.size().width/2, vertical_kernel.size().height/2), 9);
        Size sh = new Size(5, 1);
        Mat horizontal_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, sh);
        Imgproc.morphologyEx(image, image, Imgproc.MORPH_CLOSE, horizontal_kernel, new Point(horizontal_kernel.size().width/2, horizontal_kernel.size().height/2), 4);

        // Invert image
        Core.subtract(invert, image, image);

        // Find the squares
        Imgproc.findContours(image, squares, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //sort by y coordinates using the topleft point of every contour's bounding box
        Collections.sort(squares, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                Rect rect1 = Imgproc.boundingRect(o1);
                Rect rect2 = Imgproc.boundingRect(o2);
                int result = Double.compare(rect1.tl().y, rect2.tl().y);
                return result;
            }
        } );

        Double average = 0.0;
        for (MatOfPoint contour : squares) {
            average = average + Imgproc.contourArea(contour);
        }
        average = average / squares.size();
        Double offset = average / 8;
        average = average + offset;

        int i = 0;
        for (MatOfPoint contour : squares) {
            Double area = Imgproc.contourArea(contour);
            if (area < average && area > 1000) {
                row.add(contour);
                i++;
                if (i % 9 == 0) {
                    //sort by x coordinates
                    Collections.sort(row, new Comparator<MatOfPoint>() {
                        @Override
                        public int compare(MatOfPoint o1, MatOfPoint o2) {
                            Rect rect1 = Imgproc.boundingRect(o1);
                            Rect rect2 = Imgproc.boundingRect(o2);
                            return Double.compare(rect1.tl().x, rect2.tl().x);
                        }
                    });
                    sortedSquares.add(row);
                    row = new ArrayList<MatOfPoint>();
                }
            } else {
//                Mat mask = Mat.zeros(image.size(), CV_8U);
//                Scalar red = new Scalar(255, 0, 0);
//                Imgproc.drawContours(original, Arrays.asList(contour), -1, red,1);
//                boxes.add(0,original);
//                return boxes;
            }
        }

        for (List<MatOfPoint> row_of_contours : sortedSquares) {
            for (MatOfPoint contour : row_of_contours) {
                //Mat og = original.clone();
                //Mat mask = Mat.zeros(image.size(), CV_8U);
                //Imgproc.drawContours(mask, Arrays.asList(contour), -1, white, -1);

                //Scalar red = new Scalar(255, 0, 0);

                //Imgproc.drawContours(og, Arrays.asList(contour), -1, red,1);
                //Mat result = new Mat();
                //Core.bitwise_and(mask, image, result);

                Rect ROI = Imgproc.boundingRect(contour);
                Mat crop = backup.submat(ROI);

                boxes.add(crop);
                //boxes.add(og);
            }
        }

        //boxes.add(0,image);
        return boxes;
    }

    public Bitmap getBitMap(Mat image) {
        Bitmap img_bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, img_bitmap);
        return img_bitmap;
    }

    public int loadTesseract(){
        baseApi = new TessBaseAPI();
        String dataPath = context.getExternalFilesDir("/").getPath() + "/";
        baseApi.init(dataPath, "digits");

        // Optional, test later
        //baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        //baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!?@#$%&*()<>_-+=/:;'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        //baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
        //baseApi.setVariable("classify_bln_numeric_mode", "1");

        return 0;
    }

    public String readText(Bitmap bitmap) {
        baseApi.setImage(bitmap);
        String text = baseApi.getUTF8Text();
        //baseApi.end();
        return text;
    }

}
