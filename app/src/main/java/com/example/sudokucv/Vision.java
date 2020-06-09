package com.example.sudokucv;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.core.CvType.CV_8U;

public class Vision {

    private boolean testing = false;
    static final String TAG = "Vision";
    Context context;
    final String TEST_FILES = "testfiles";

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "success");
        } else {
            Log.d(TAG,"unsuccess");
        }
    }

    static {
        //System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
        //System.loadLibrary("jpgt");
        //System.loadLibrary("pngt");
        //System.loadLibrary("lept");
        //System.loadLibrary("tess");
    }

    public int create(Context con) {
        context = con;
        if (context.getPackageName().contains("test")) {
            testing = true;
        }
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
            Log.e(TAG, e.getMessage());
        }
    }

    public Mat loadImage(String filename) {
        Mat image = Imgcodecs.imread(filename);
        return image;
    }

    public ArrayList<Mat> aquireImages() {
        if (!testing)
            prepareFiles(TEST_FILES, TEST_FILES);

        final String TESS_DATA = "tessdata";
        prepareFiles("", TESS_DATA);

        String externalDir = "";
        //if (testing)
            externalDir = "/storage/emulated/0/Android/data/com.example.sudokucv/files";
        //else
        //    externalDir = context.getExternalFilesDir("/").getPath();

        String imagePath = externalDir + "/" + TEST_FILES + "/";
        ArrayList<Mat> images = new ArrayList<Mat>();
        try {
            File directory = new File(imagePath);
            File[] files = directory.listFiles();
            for (File fileName : files) {
                images.add(Imgcodecs.imread(fileName.getPath()));
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return images;
    }

    public ArrayList<Mat> isolateBoxes(Mat image) {
        ArrayList<Mat> boxes = new ArrayList<>();
        List<MatOfPoint> contours = new ArrayList<>();
        List<MatOfPoint> noise = new ArrayList<>();

        List<List<MatOfPoint>> sorted_contours = new ArrayList<>();
        List<MatOfPoint> row = new ArrayList<>();

        Mat original = image.clone();
        Mat hierarchy = new Mat();
        Mat hier = new Mat();

        // Make original colors, OpenCV uses BGR
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB);

        // Make black and white (grayscale)
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        // adaptive threshold
        Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 57, 5);
        //Imgproc.adaptiveThreshold(backup, backup, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 57, 5);

        Mat backup = image.clone();

        // Filter out all numbers and noise to isolate
        Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Scalar black = new Scalar(0, 0, 0);

        Scalar white = new Scalar(255, 255, 255);
        for (MatOfPoint contour : contours) {
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

        // Sort by top to bottom and each row left to right
        Imgproc.findContours(image, contours, hier, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //sort by y coordinates using the topleft point of every contour's bounding box
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint o1, MatOfPoint o2) {
                Rect rect1 = Imgproc.boundingRect(o1);
                Rect rect2 = Imgproc.boundingRect(o2);
                int result = Double.compare(rect1.tl().y, rect2.tl().y);
                return result;
            }
        } );

        int i = 0;
        for (MatOfPoint contour : contours) {
            Double area = Imgproc.contourArea(contour);
            if (area < 5000 && area > 1000) {
                row.add(contour);
                i++;
                if (i % 9 == 0) {
                    //sort by x coordinates
                    Collections.sort(row, new Comparator<MatOfPoint>() {
                        @Override
                        public int compare(MatOfPoint o1, MatOfPoint o2) {
                            Rect rect1 = Imgproc.boundingRect(o1);
                            Rect rect2 = Imgproc.boundingRect(o2);
                            int result = 0;
                            double total = rect1.tl().y/rect2.tl().y;
                            if (total>=0.9 && total<=1.4 ){
                                result = Double.compare(rect1.tl().x, rect2.tl().x);
                            }
                            return result;
                        }
                    });
                    sorted_contours.add(row);
                    row = new ArrayList<MatOfPoint>();
                }
            }
        }

        for (List<MatOfPoint> row_of_contours : sorted_contours) {
            for (MatOfPoint contour : row_of_contours) {
                Mat og = original.clone();
                //Mat zeroMat = Mat.zeros(image.rows(), image.cols(), CvType.CV_8UC1);
                Mat mask = Mat.zeros(image.size(), CV_8U);
                Mat number = Mat.zeros(image.size(), CV_8U);
                Scalar red = new Scalar(255, 0, 0);
                Imgproc.drawContours(mask, Arrays.asList(contour), -1, white, -1);
                Imgproc.drawContours(original, Arrays.asList(contour), -1, red,1);
                Mat result = new Mat();
                Core.bitwise_and(mask, image, result);
                //result[mask] = 0;

                Rect ROI = Imgproc.boundingRect(contour);
                Mat crop = backup.submat(ROI);

                //boxes.add(crop);
                boxes.add(original);
                original = og.clone();
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

    public String recognizeText(Bitmap bitmap) {
        TessBaseAPI baseApi = new TessBaseAPI();
        String dataPath = "/storage/emulated/0/Android/data/com.example.sudokucv/files/";
        //String dataPath = context.getExternalFilesDir("/").getPath() + "/";
        baseApi.init(dataPath, "digits");

        //baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        //baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!?@#$%&*()<>_-+=/:;'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        //baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
        //baseApi.setVariable("classify_bln_numeric_mode", "1");

        baseApi.setImage(bitmap);
        String recognizedText = baseApi.getUTF8Text();
        baseApi.end();
        //return "";
        return recognizedText;
    }

}
