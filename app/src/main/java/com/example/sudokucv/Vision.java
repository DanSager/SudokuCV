package com.example.sudokucv;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.google.gson.Gson;
import com.googlecode.tesseract.android.TessBaseAPI;
//import com.zsmarter.*;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.opencv.utils.Converters;

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

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC3;

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

                if (!fileName.contains("."))
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
                String mockName = "puzzle" + Integer.toString(index) + ".";
                Boolean con = f.getName().contains(mockName);
                if ((f.getName().contains(".png") || f.getName().contains(".jpg")) && (!single || (single && con))) {
                    Mat image = null;
                    List<String> values = new ArrayList<>();

                    String imagePath = f.getPath();
                    String fileNamePath = imagePath.substring(0, imagePath.lastIndexOf('.'));
                    String fileName = fileNamePath.substring(imagePath.lastIndexOf('/') + 1);
                    String jsonPath = fileNamePath + ".json";

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

    public Mat getHoughPTransform(Mat image, double rho, double theta, int threshold) {
        Mat result = image.clone();
        Mat lines = new Mat();
        Imgproc.HoughLinesP(image, lines, rho, theta, threshold);

        for (int i = 0; i < lines.cols(); i++) {
            double[] val = lines.get(0, i);
            Imgproc.line(result, new Point(val[0], val[1]), new Point(val[2], val[3]), new Scalar(0, 0, 255), 2);
        }
        return result;
    }

    public ArrayList<Mat> isolateBoxes(Mat image) {
        // Lists
        ArrayList<Mat> boxes = new ArrayList<>();
        List<MatOfPoint> contents = new ArrayList<>();
        List<MatOfPoint> squares = new ArrayList<>();
        List<MatOfPoint> numbers = new ArrayList<>();
        List<MatOfPoint> noise = new ArrayList<>();
        List<List<MatOfPoint>> sortedSquares = new ArrayList<>();
        List<MatOfPoint> row = new ArrayList<>();

        // Contours
        MatOfPoint largestContour = null;

        // Colors
        Scalar red = new Scalar(255, 0, 0);
        Scalar green = new Scalar(0, 255, 0);
        Scalar black = new Scalar(0, 0, 0);
        Scalar white = new Scalar(255, 255, 255);

        // Make original colors, OpenCV uses BGR
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2RGB);

        // Resize image so contours are on average the same size regardless of pic
        Size s = new Size(2000, 2000);
        Imgproc.resize(image, image, s);

        // Clone
        Mat original = image.clone();
        Mat puzzleBorder = image.clone();

        // Make grayscale
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        //boxes.add(imgLabel(image, "gray"));

        // Erode
        Mat erode = image.clone();
        Imgproc.erode(erode, erode, Imgproc.getStructuringElement(Imgproc.CV_SHAPE_CROSS, new Size(3,3)));
        //boxes.add(imgLabel(erode, "erode"));
        image = erode.clone();

        // adaptive threshold, make black or white and invert
        Imgproc.adaptiveThreshold(image, image, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 57, 3);

        // Find all contours
        Imgproc.findContours(image, contents, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find largest contour
        largestContour = contents.get(0);
        for (MatOfPoint contour : contents) {
            if (Imgproc.contourArea(contour) > Imgproc.contourArea(largestContour))
                largestContour = contour;
        }

        // White out rest of image
        Mat maskInsideContour = Mat.zeros(image.size(), CV_8U);
        Imgproc.drawContours(maskInsideContour, Arrays.asList(largestContour), -1, white, -1);

        // Draw red line around border (debugging purposes only)
        Imgproc.drawContours(puzzleBorder, Arrays.asList(largestContour), -1, red, 3);
        //boxes.add(imgLabel(puzzleBorder, "puzzle border"));

        Mat maskedImage = new Mat(image.size(), CV_8UC3).setTo(white);
        Mat blankOriginalSize = new Mat(image.size(), image.type(), white);

        Core.subtract(blankOriginalSize, image, image);
        image.copyTo(maskedImage, maskInsideContour);
        image = maskedImage.clone();
        Core.subtract(blankOriginalSize, image, image);

        // Stretch puzzle to fill image
        RotatedRect box = Imgproc.minAreaRect(new MatOfPoint2f(largestContour.toArray())); // Rough box around puzzle
        Point[] pts = new Point[4];
        box.points(pts); // Copy the 4 corners of this box to array

        pts = sortCorners(pts); // Sort so that bottom-right = 0, bottom-left = 1, top-left = 2, top-right = 3

        List<Point> corners = getCorners(pts, largestContour); // Find corners by finding the nearest points of the contour to the box
        Mat warped = warp(image, corners.get(2), corners.get(3), corners.get(1), corners.get(0)); // Stretch puzzle to fill image
        Mat warpedOriginal = warp(original, corners.get(2), corners.get(3), corners.get(1), corners.get(0));
        image = warped.clone();

        // Resize image so contours are on average the same size regardless of the pic
        Imgproc.resize(image, image, s);
        Imgproc.resize(warpedOriginal, warpedOriginal, s);

        Mat noiseBorder = warpedOriginal.clone();
        //boxes.add(imgLabel(image, "After wrap & resize"));

        // Closing - Effect uncertain
        // Mat closing = new Mat(image.rows(), image.cols(), image.type());
        // Mat kernel = Mat.ones(5,5, CvType.CV_32F);
        // Imgproc.morphologyEx(image, closing, Imgproc.MORPH_CLOSE, kernel);
        // boxes.add(imgLabel(closing, "closing"));
        // image = closing.clone();

        // Filter out all noise
        contents = new ArrayList<>();
        Imgproc.findContours(image, contents, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint contour : contents) {
            Double area = Imgproc.contourArea(contour);
            if (area < 1000) {
                Imgproc.drawContours(image, Arrays.asList(contour), -1, black, -1);
                Imgproc.drawContours(noiseBorder, Arrays.asList(contour), -1, red, 1);
                noise.add(contour);
            }
        }

        //boxes.add(imgLabel(noiseBorder, "Noise selected"));
        //boxes.add(imgLabel(image, "post remove noise"));

        // Smooth
        Imgproc.medianBlur(image, image, 3);
        //Imgproc.medianBlur(image, image, 3);
        //Imgproc.medianBlur(image, image, 3);
        //boxes.add(imgLabel(image, "post smooth"));

        // Remove noise from the rest of contours
        contents.removeAll(noise);

        // Make clones
        Mat filledBoxes = image.clone(); // Holds squares with numbers and no noise
        Mat whiteOut = new Mat(image.size(), CV_8UC3).setTo(white);
        Mat numberBorder = warpedOriginal.clone();
        Mat boxBorder = warpedOriginal.clone();

        // Find all boxes & numbers (as contours)
        contents = new ArrayList<>(); // Clear old contours, ie. containing border
        Imgproc.findContours(image, contents, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        //boxes.add(imgLabel(whiteOut, "whiteout"));

        // Remove numbers
        for (MatOfPoint contour : contents) {
            Double area = Imgproc.contourArea(contour);
            if (area < 15000 && area > 1200) { // Number
                Imgproc.drawContours(image, Arrays.asList(contour), -1, black, -1);

                MatOfPoint2f m2f = new MatOfPoint2f(contour.toArray());
                RotatedRect rr = Imgproc.minAreaRect(m2f);
                Size sz = rr.size;
                Double dou = sz.width / 4;
                Double dou2 = sz.height / 4;
                rr.size = new Size(sz.width +dou, sz.height+dou2);
                //rr.center = new Point(rr.center.x + 20, rr.center.y + 20);

                Mat mask = new Mat(filledBoxes.rows(), filledBoxes.cols(), CvType.CV_8U, Scalar.all(0));
                Imgproc.ellipse(mask, rr, white, -1);
                filledBoxes.copyTo(whiteOut, mask);

                Imgproc.drawContours(numberBorder, Arrays.asList(contour), -1, red, 2);
                numbers.add(contour);
                //boxes.add(imgLabel(numberBorder, "Area: " + area));
            } else if (area > 1000 && area < 1200) {
                Imgproc.drawContours(numberBorder, Arrays.asList(contour), -1, green, 2);
                //boxes.add(imgLabel(numberBorder, "Area: " + area));
            }
        }

        //boxes.add(imgLabel(numberBorder, "Number border"));
        //boxes.add(imgLabel(whiteOut, "whiteout"));
        //boxes.add(imgLabel(image, "post remove nums"));

        // Fix horizontal and vertical lines
        Size sv = new Size(1, 7);
        Mat vertical_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, sv);
        Imgproc.morphologyEx(image, image, Imgproc.MORPH_CLOSE, vertical_kernel, new Point(vertical_kernel.size().width/2, vertical_kernel.size().height/2), 11);
        Size sh = new Size(7, 1);
        Mat horizontal_kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, sh);
        Imgproc.morphologyEx(image, image, Imgproc.MORPH_CLOSE, horizontal_kernel, new Point(horizontal_kernel.size().width/2, horizontal_kernel.size().height/2), 11);

        //boxes.add(imgLabel(image, "post fix Vert and hor"));

        // Invert image
        Mat blankWrappedSize = new Mat(image.rows(),image.cols(), image.type(), new Scalar(255,255,255));
        Core.subtract(blankWrappedSize, image, image);
        Core.subtract(blankWrappedSize, filledBoxes, filledBoxes);
        Core.subtract(blankWrappedSize, whiteOut, whiteOut);

        // Find the squares
        Imgproc.findContours(image, squares, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        if (squares.size() != 81) {
            Log.e(TAG, "Number of squares != 81");
            return boxes;
        }

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

        int i = 0;
        for (MatOfPoint contour : squares) {
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
                row = new ArrayList<>();
            }
        }

        Scalar alternate = red;
        for (List<MatOfPoint> row_of_contours : sortedSquares) {
            for (MatOfPoint contour : row_of_contours) {

                Imgproc.drawContours(boxBorder, Arrays.asList(contour), -1, alternate, 7);
                if (alternate == red)
                    alternate = green;
                else
                    alternate = red;

                Rect ROI = Imgproc.boundingRect(contour);
                //Rect ro = new Rect(ROI.x+11, ROI.y+11, ROI.width-22, ROI.height-22);
                Mat crop = whiteOut.submat(ROI);
                Mat cro = filledBoxes.submat(ROI);
                //Mat cro = whiteOut.submat(ro);

                boxes.add(crop);
                //boxes.add(cro);
            }
        }


        //boxes.add(imgLabel(boxBorder, "border of squares"));

        // TODO increase line detection
        // TODO try cropping the

        return boxes;
    }

    private Mat imgLabel(Mat img, String label) {
        Mat retval = img.clone();
        if (retval.channels() == 1)
            Imgproc.cvtColor(retval, retval, Imgproc.COLOR_GRAY2BGR);
        Scalar blue = new Scalar(0, 255, 255);
        Imgproc.putText(retval, label, new Point(25,250), 1, 4, blue, 3);
        return retval;
    }

    public Mat warp(Mat inputMat, Point topLeft, Point topRight, Point bottomLeft, Point bottomRight) {
        int resultWidth = (int)(topRight.x - topLeft.x);
        int bottomWidth = (int)(bottomRight.x - bottomLeft.x);
        if(bottomWidth > resultWidth)
            resultWidth = bottomWidth;
        int resultHeight = (int)(bottomLeft.y - topLeft.y);
        int bottomHeight = (int)(bottomRight.y - topRight.y);
        if (bottomHeight > resultHeight) {
            resultHeight = bottomHeight;
        }
        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC1);
        List<Point> source = new ArrayList<>();
        source.add(topLeft);
        source.add(topRight);
        source.add(bottomLeft);
        source.add(bottomRight);
        Mat startM = Converters.vector_Point2f_to_Mat(source);
        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(resultWidth, 0);
        Point ocvPOut3 = new Point(0, resultHeight);
        Point ocvPOut4 = new Point(resultWidth, resultHeight);
        List<Point> dest = new ArrayList<>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);
        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);
        Imgproc.warpPerspective(inputMat, outputMat, perspectiveTransform, new Size(resultWidth, resultHeight));
        return outputMat;
    }

    private List<Point> getCorners(Point[] pts, MatOfPoint contour) {
        List<Point> corners = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Point p = pts[i];
            Double maxDistance = 100000.0;
            Point closest = null;
            for (Point mP : contour.toArray()) {
                Double xDis = Math.abs(mP.x - p.x);
                Double yDis = Math.abs(mP.y - p.y);
                if (xDis + yDis < maxDistance) {
                    maxDistance = xDis + yDis;
                    closest = mP;
                }
            }
            corners.add(new Point(closest.x, closest.y));
        }
        return corners;
    }

    private Point[] sortCorners(Point[] pts) {
        Point[] sorted = new Point[4];
        Double xAvg = 0.0;
        Double yAvg = 0.0;
        for (Point p : pts) {
            xAvg = xAvg + p.x;
            yAvg = yAvg + p.y;
        }
        xAvg = xAvg/4;
        yAvg = yAvg/4;

        for (Point p : pts) {
            if (p.x > xAvg && p.y > yAvg)
                sorted[0] = p;
            else if (p.x > xAvg && p.y < yAvg)
                sorted[3] = p;
            else if (p.x < xAvg && p.y > yAvg)
                sorted[1] = p;
            else if (p.x < xAvg && p.y < yAvg)
                sorted[2] = p;
        }

        return sorted;
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
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        //baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!?@#$%&*()<>_-+=/:;'\"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "123456789");
        baseApi.setVariable("classify_bln_numeric_mode", "1");

        return 0;
    }

    public String readText(Bitmap bitmap) {
        baseApi.setImage(bitmap);
        String text = baseApi.getUTF8Text();
        //baseApi.end();
        return text;
    }

}
