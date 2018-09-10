/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.example.android.bluetoothchat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.android.common.activities.SampleActivityBase;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

import static org.opencv.core.CvType.CV_8UC4;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase implements SensorEventListener, StepListener, CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    public static final String TAG = "MainActivity";

    private Mat mRgba;
    private Mat mRgbaT;
    private Mat mIntermediateMat;
    private Mat mGray;
    private Mat mThres;
    private Mat mCombo;
    private Mat mPrevGray;
    private Mat mGreen;
    private Mat mImg1;
    private Mat mImg2;
    private Scalar mean;
    private Scalar green = new Scalar(0,255,0);
    private Scalar red = new Scalar(255,0,0);
    private Point lightPos;
    private Point middle;

    private List<Point> prevList;
    private List<Point> nextList;

    MatOfPoint2f prevFeatures, nextFeatures;
    MatOfPoint features;

    MatOfByte status;
    MatOfFloat err;

    private Size radius = new Size(15,15);

    private CameraBridgeViewBase mOpenCvCameraView;

    private TextView info;

    private TextView angleStep;
    private TextView coordinates;
    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;

    // Sensors & SensorManager
    private Sensor accelerometer;
    private Sensor rotation;
    BluetoothChatFragment fragment = new BluetoothChatFragment();

    public static String luminance = "luminance";

    float[] orientation = new float[3];
    float[] rMat = new float[9];
    int[] mAzimuth = new int[99];
    public double xoffset, yoffset = 0;
    private int aziCount = 0;
    private int numSteps;
    private int currentangle;
    //public double xcoor = 575;
    //public double ycoor = 125;
    //public double zcoor = 180;
    public double xcoor = 447;
    public double ycoor = 136;
    public double zcoor = 180;
    public long gondolaUpdate = 0;
    public boolean opencvUpdate = false;
    public boolean updateGondola = true;
    public boolean updateStep = false;

    public double xPixel = 0;
    public double yPixel = 0;

    private RelativeLayout mFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback))
        {
            Log.e("TEST", "Cannot connect to OpenCV Manager");
        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_activity_surface_view);
        mOpenCvCameraView.setCameraIndex(1);
        mOpenCvCameraView.setCvCameraViewListener(this);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
            /*
            FragmentTransaction secondtransaction = getSupportFragmentManager().beginTransaction();
            secondtransaction.replace(R.id.container, Camera2BasicFragment.newInstance());
            secondtransaction.commit();
            */
        }

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        sensorManager.unregisterListener(this);
        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(MainActivity.this, rotation, SensorManager.SENSOR_DELAY_GAME);

        angleStep = (TextView) findViewById(R.id.angleStep);
        angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     Waiting for a step...    " + TEXT_NUM_STEPS + numSteps + " " + luminance);
        coordinates = (TextView) findViewById(R.id.coordinates);
        coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);

        info = (TextView) findViewById(R.id.info);
        info.setText("Brightest pixel");

        Button BtnStart = (Button) findViewById(R.id.btn_start);
        Button BtnStop = (Button) findViewById(R.id.btn_stop);

        BtnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                numSteps = 0;
                xcoor = 575;
                ycoor = 125;
                zcoor = 180;

                angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     Waiting for a step...    " + TEXT_NUM_STEPS + numSteps + " " + luminance);
                coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);
                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(MainActivity.this, rotation, SensorManager.SENSOR_DELAY_NORMAL);
            }
        });


        BtnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                sensorManager.unregisterListener(MainActivity.this);
            }
        });

        mFrame = (RelativeLayout) findViewById(R.id.frame);

        // Exit unless both sensors are available
        if (null == accelerometer || null == rotation)
            finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

//    public static Bitmap CorrectBitmap(Bitmap source, float angle)
//    {
//        Matrix matrix = new Matrix();
//        matrix.preScale(-1.0f, 1.0f);
//        matrix.postRotate(angle);
//        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
//    }
//
//    public Bitmap toGrayscale(Bitmap bmpOriginal)
//    {
//        int width, height;
//        height = bmpOriginal.getHeight();
//        width = bmpOriginal.getWidth();
//
//        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        Canvas c = new Canvas(bmpGrayscale);
//        Paint paint = new Paint();
//        ColorMatrix cm = new ColorMatrix();
//        cm.setSaturation(0);
//        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
//        paint.setColorFilter(f);
//        c.drawBitmap(bmpOriginal, 0, 0, paint);
//        return bmpGrayscale;
//    }

    @Override
    protected void onResume() {
        super.onResume();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);

        // Register for sensor updates
        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this, rotation,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }

        // Unregister all sensors
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        // Unregister all sensors
        sensorManager.unregisterListener(this);
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     " + currentangle + "    " + TEXT_NUM_STEPS + numSteps + " " + luminance);
        updateStep = true;
        updateCoordinates();
    }

    public void updateCoordinates(){
        currentangle = getAverageAzimuth();
        double rad = Math.toRadians(360 - (currentangle + 30));
        double xstep;
        double ystep;
        if(updateStep){
            xstep = 78*(Math.cos(rad));
            ystep = 78*(Math.sin(rad));
            xcoor = xcoor - xstep;
            ycoor = ycoor + ystep;
            updateStep = false;
        }else {
            xstep = (xoffset * (Math.cos(rad))) + (yoffset * (Math.cos(rad)));
            ystep = (xoffset * (Math.sin(rad))) + (yoffset * (Math.sin(rad)));
            xcoor = xcoor - xstep;
            ycoor = ycoor + ystep;
        }

        angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     " + currentangle + "    " + TEXT_NUM_STEPS + numSteps + " " + luminance);
        coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);
        updateGondola((int)xcoor, (int)ycoor, (int)zcoor);
    }

    public void updateGondola(int xcoor, int ycoor, int zcoor){
        updateGondola = true;
        if(xcoor < 100.0 || xcoor > 600.0) {
            updateGondola = false;
        }else if(ycoor < 100.0 || ycoor > 500.0){
            updateGondola = false;
        }else if(zcoor < 0 || zcoor > 225){
            updateGondola = false;
        }
        if(updateGondola) {
            fragment.sendMessageFromMain((int) xcoor, (int) ycoor, (int) zcoor);
            coordinates.setText("" + xcoor + "    " + ycoor + "    " + "Sent to gondola!");
        }else{
            coordinates.setText("" + xcoor + "    " + ycoor + "    " + "Out of bounds!");
        }
    }

    public static void getCoordinates(String gondolaMessage){
        luminance = gondolaMessage;
    }

//    public Bitmap calculatePixels(Bitmap bMap, int height, int width){
//        int[] pixels;
//        int lightcount = 0;
//
//        pixels = new int[height * width];
//
//        bMap.getPixels(pixels, 0, width, 1, 1, width - 1, height - 1);
//
//        for(int i = 0; i < width*height; i++){
//            int R = Color.red(pixels[i]);
//            int B = Color.blue(pixels[i]);
//            int G = Color.green(pixels[i]);
//            double Y = (0.299 * R + 0.587 * G + 0.114 * B);
//            if(Y<230){
//                pixels[i]=0;
//            }else{
//                xPixel += Math.floor(i/width);
//                yPixel += i % width;
//                lightcount++;
//            }
//        }
//        xPixel = xPixel/lightcount;
//        yPixel = yPixel/lightcount;
//        coordinates.setText("" + xPixel + "    " + yPixel + "    " + width + "    " + luminance);
//        Bitmap filteredBMap = Bitmap.createBitmap(pixels, 0,width, width, height, Bitmap.Config.RGB_565);
//        xoffset = Math.round((xPixel - width)/14.2);
//        yoffset = Math.round(((yPixel - height)/13.25));
//        return filteredBMap;
//    }

    public int getAverageAzimuth()
    {
        int sum = 0;
        for(int j = 0; j < 99; j++){
            sum = sum + mAzimuth[j];
        }
        sum = sum / 99;
        return sum;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
        else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            // calculate the rotation matrix
            SensorManager.getRotationMatrixFromVector( rMat, event.values );
            // get the azimuth value (orientation[0]) in degree
            mAzimuth[aziCount] = (int) ( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;
            angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     " + currentangle + "    " + TEXT_NUM_STEPS + numSteps + " " + luminance);
            if(aziCount == 98){
                aziCount = 0;
            }else{
                aziCount++;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    private void resetVars() {
        mPrevGray = new Mat(mGray.rows(), mGray.cols(), CvType.CV_8UC1);
        features = new MatOfPoint();
        prevFeatures = new MatOfPoint2f();
        nextFeatures = new MatOfPoint2f();
        status = new MatOfByte();
        err = new MatOfFloat();
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    // Camera Code
    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CV_8UC4);
        mRgbaT = new Mat(height, width, CV_8UC4);
        mIntermediateMat = new Mat(height, width, CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mThres = new Mat(height, width, CvType.CV_8UC1);
        middle = new Point(mGray.width()/2,mGray.height()/2);

//        mCombo = new Mat(height, width, CvType.CV_8UC4);
//        mImg1 = new Mat(height, width, CvType.CV_8UC4);
//        mImg2 = new Mat(height, width, CvType.CV_8UC4);
//        Scalar black = new Scalar(0,0,0);
//        mGreen = new Mat(height, width, CV_8UC4, black);
        resetVars();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mRgbaT.release();
        mGray.release();
        mThres.release();
        mIntermediateMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mIntermediateMat.release();
        mGray.release();
        mRgba.release();
        mThres.release();
        mRgbaT.release();
        mGray = inputFrame.gray();
//        Imgproc.threshold(mGray,mThres,254,255, Imgproc.THRESH_BINARY);
//        mThres.empty();
//        Scalar black = new Scalar(0,0,0);
//        mGreen = new Mat(mRgba.size(), CV_8UC4, black);
//        mGreen.setTo(green, mThres);
//        mean = Core.mean(mThres);
        Imgproc.GaussianBlur(mGray,mGray,radius,2);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(mGray);
        lightPos = mmr.maxLoc;

        mGray = inputFrame.rgba();

//        if (features.toArray().length == 0) {
//            int rowStep = 50, colStep = 100;
//            int nRows = mGray.rows() / rowStep, nCols = mGray.cols() / colStep;
//
//            Point points[] = new Point[nRows * nCols];
//            for (int i = 0; i < nRows; i++) {
//                for (int j = 0; j < nCols; j++) {
//                    points[i * nCols + j] = new Point(j * colStep, i * rowStep);
//                }
//            }
//
//            features.fromArray(points);
//
//            prevFeatures.fromList(features.toList());
//            mPrevGray = mGray.clone();
//        }
//
//        nextFeatures.fromArray(prevFeatures.toArray());
//        Video.calcOpticalFlowPyrLK(mPrevGray, mGray, prevFeatures, nextFeatures, status, err);
//
//        prevList = features.toList();
//        nextList = nextFeatures.toList();
//        Scalar color = new Scalar(255, 0, 0);
//
//        for (int i = 0; i < prevList.size(); i++) {
//            Imgproc.line(mGray, prevList.get(i), nextList.get(i), color);
//            updateText(i, lightPos);
//        }
//        mPrevGray = mGray.clone();

//        mCombo = new Mat(mRgba.size(), CV_8UC4);
//        mImg1 = mGray;
//        mImg2 = mGreen;
//        Core.addWeighted(mImg1, 0.5, mImg2, 0.5, 0.0, mCombo);
//

        Imgproc.circle(mGray,lightPos,9,green,5);
        Imgproc.circle(mGray,middle,9,red, 5);

        checkupdate();

        mRgbaT = mGray.t();
        Core.flip(mGray.t(), mRgbaT, -1);
        Imgproc.resize(mRgbaT, mIntermediateMat, mGray.size());
        System.gc();
        return mIntermediateMat;
//        return mGray;
    }

    public void checkupdate(){
        if(!opencvUpdate){
            opencvUpdate = true;
            gondolaUpdate = System.currentTimeMillis();
        }

        if(System.currentTimeMillis() - gondolaUpdate > 10000) {
            updatePosition(lightPos, middle);
            opencvUpdate = false;
        }
    }

    public void updatePosition(final Point p, final Point m){
        currentangle = getAverageAzimuth();
        double rad = Math.toRadians(360 - (currentangle + 30));
        double xstep,ystep;
        xoffset = Math.round((p.x - m.x)/14.2);
        yoffset = Math.round((m.y - p.y)/13.25);
        xstep = (xoffset * (Math.cos(rad))) + (yoffset * (Math.cos(rad)));
        ystep = (xoffset * (Math.sin(rad))) + (yoffset * (Math.sin(rad)));

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                    info.setText(p.x + " " + p.y);
//                info.setText(mGray.width() + " " + mGray.height());
            }
        });
    }
}