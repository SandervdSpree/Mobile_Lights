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

import java.util.ArrayList;
import java.util.Iterator;
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
    private Mat mPrevGray;
    /*
    private Mat mCombo;
    private Mat mGreen;
    private Mat mImg1;
    private Mat mImg2;
    private Scalar mean;
    */
    private Scalar green = new Scalar(0,255,0);
    private Scalar red = new Scalar(255,0,0);
    private Scalar blue = new Scalar(0,0,255);
    private Point lightPos = new Point(0,0);
    private Point lightPosMaxLoc = new Point();
    private Point middle = new Point();

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

    float[] orientation = new float[3];
    float[] rMat = new float[9];
    int[] mAzimuth = new int[99];
    public double xoffset, yoffset, angle, rad = 0;
    private int aziCount = 0;
    private int numSteps;
    private int currentangle;
    //public double xcoor = 575;
    //public double ycoor = 125;
    //public double zcoor = 180;
//    public double xcoor = 447;
//    public double ycoor = 136;
//    public double zcoor = 180;
    public static double xcoor = 400;
    public static double ycoor = 300;
    public static double zcoor = 0;
    public long gondolaUpdate = 0;
    public boolean opencvUpdate = false;
    public boolean updateGondola = true;
    public boolean updateStep = false;
    public boolean testFix = false;
    public static boolean discovery = false;
    public boolean buttonToggle = false;

    //private RelativeLayout mFrame;

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
        angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     Waiting for a step...    " + TEXT_NUM_STEPS + numSteps);
        coordinates = (TextView) findViewById(R.id.coordinates);
        coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);

        info = (TextView) findViewById(R.id.info);
        info.setText("Brightest pixel");

        Button BtnPedo = (Button) findViewById(R.id.btn_offset);
        Button BtnDiscov = (Button) findViewById(R.id.btn_discov);

        BtnPedo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
//                if(buttonToggle){
//                    numSteps = 0;
//                    xcoor = 575;
//                    ycoor = 125;
//                    zcoor = 180;
//
//                    angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     Waiting for a step...    " + TEXT_NUM_STEPS + numSteps);
//                    coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);
//                    sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
//                    sensorManager.registerListener(MainActivity.this, rotation, SensorManager.SENSOR_DELAY_NORMAL);
//                }else{
//                    sensorManager.unregisterListener(MainActivity.this);
//                }
//                buttonToggle = !buttonToggle;

                //fragment.sendDiscoveryFromMain("seen");
                testFix = true;
            }
        });


        BtnDiscov.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                fragment.sendDiscoveryFromMain("disc");
                discovery = true;
            }
        });

        //mFrame = (RelativeLayout) findViewById(R.id.frame);

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
        angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     Waiting for a step...    " + TEXT_NUM_STEPS + numSteps);
        updateStep = true;
        updateCoordinates();
    }

    public void updateCoordinates(){
        currentangle = getAverageAzimuth();
        rad = Math.toRadians(360 - (currentangle + 115));
        final double xstep;
        final double ystep;
        if(updateStep){
            xstep = 78*(Math.cos(rad));
            ystep = 78*(Math.sin(rad));
            xcoor = xcoor - xstep;
            ycoor = ycoor + ystep;
            updateStep = false;
        }else {
            xoffset = Math.round((lightPos.y - middle.y)/9.47);
            yoffset = Math.round((middle.x - lightPos.x)/9.41);
            double distance = Math.hypot(xoffset, yoffset);
            angle = Math.atan2(yoffset,xoffset);
            xstep = distance*(Math.cos(angle + rad));
            ystep = distance*(Math.sin(angle + rad));
            xcoor = xcoor - xstep;
            ycoor = ycoor + ystep;
            testFix = false;
        }

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     Waiting for a step...    " + TEXT_NUM_STEPS + numSteps);
                coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);
                info.setText(middle.x + " " + middle.y + " " + lightPos.x + " " + lightPos.y + " angle: " + Math.toDegrees(angle + rad) + " " + (int)xstep + " " + (int)ystep);
//                info.setText(mGray.width() + " " + mGray.height());
            }
        });
        //updateGondola((int)xcoor, (int)ycoor, (int)zcoor);
    }

    public void updateGondola(int xcoor, int ycoor, int zcoor){
        updateGondola = true;
        if(xcoor < 100.0){
            xcoor = 100;
            updateGondola = false;
        }else if(xcoor > 600.0) {
            xcoor = 600;
            updateGondola = false;
        }else if(ycoor < 100.0){
            ycoor = 100;
            updateGondola = false;
        }else if(ycoor > 500.0){
            ycoor = 500;
            updateGondola = false;
        }else if(zcoor < 0){
            zcoor = 0;
            updateGondola = false;
        }else if(zcoor > 180){
            zcoor = 180;
            updateGondola = false;
        }
        if(updateGondola) {
            fragment.sendCoordinatesFromMain((int) xcoor, (int) ycoor, (int) zcoor);
            coordinates.setText("" + xcoor + "    " + ycoor + "    " + "Sent to gondola!");
        }else{
            fragment.sendCoordinatesFromMain((int) xcoor, (int) ycoor, (int) zcoor);
            coordinates.setText("" + xcoor + "    " + ycoor + "    " + "Out of bounds! Sent to gondola with boundaries.");
        }
    }

    public static void getCoordinates(String gondolaMessage){
        if(gondolaMessage != null && !gondolaMessage.isEmpty()) {
            String ltrim = gondolaMessage.replaceAll("^\\s+","");
            String rtrim = ltrim.replaceAll("\\s+$","");
            String test = "";
            if (!rtrim.equals(test)) {
                String Coor = rtrim.substring(0,1);
                Coordinates coorTest = Coordinates.valueOf(Coor.toUpperCase());
                int position = 0;
                if(!coorTest.equals("D")){
                    position = Integer.parseInt(rtrim.substring(1,rtrim.length()));
                }
                switch(coorTest){
                    case D:
                        discovery = !discovery;
                        break;
                    case X:
                        xcoor = position;
                        break;
                    case Y:
                        ycoor = position;
                        break;
                    case Z:
                        zcoor = position;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public enum Coordinates {
        D,
        X,
        Y,
        Z
    }

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
            angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     Waiting for a step...    " + TEXT_NUM_STEPS + numSteps);
            coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);
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

        // Thresholding: leaving only light sources
        Imgproc.threshold(mGray,mThres,254,255, Imgproc.THRESH_BINARY);

        // Find contours of light sources
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mThres,contours,new Mat(),Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);
        double maxArea = 0;
        MatOfPoint max_contour = new MatOfPoint();

        //
        if(discovery){
            if(!contours.isEmpty()){
                fragment.sendDiscoveryFromMain("seen");
                discovery = false;
            }
        }

        // Select biggest contour = LED in dark room
        Iterator<MatOfPoint> iterator = contours.iterator();
        while (iterator.hasNext()) {
            MatOfPoint contour = iterator.next();
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                max_contour = contour;
            }
        }

        // Determine center of light source
        centerPolygon(max_contour);

        // Gaussian blur and brightest pixel detection
        Imgproc.GaussianBlur(mGray,mGray,radius,2);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(mGray);
        lightPosMaxLoc = mmr.maxLoc;

        // Get color frame back for display
        mGray = inputFrame.rgba();

        // Draw functions for light source contours and middle point, LED point en brightest pixel point
        for(int i = 0; i<contours.size();i++) {
            Imgproc.drawContours(mGray, contours, i, green);
        }
        Imgproc.circle(mGray,lightPos,9,green,5);
        Point line = new Point((middle.x + (middle.x - lightPos.x)), (middle.y + (middle.y - lightPos.y)));
        Imgproc.line(mGray,middle,line, green);
        Imgproc.circle(mGray,lightPosMaxLoc,9,blue,5);
        Imgproc.circle(mGray,middle,9,red, 5);

        //
        if(testFix){
            checkupdate();
        }

        mRgbaT = mGray.t();
        Core.flip(mGray.t(), mRgbaT, -1);
        Imgproc.resize(mRgbaT, mIntermediateMat, mGray.size());
        System.gc();
        return mIntermediateMat;
//        return mGray;
    }

    private void centerPolygon(MatOfPoint points)
    {
        List<Point> p = points.toList();
        double x=0, y=0;
        double length = 0;

        Iterator<Point> iterator = p.iterator();
        while (iterator.hasNext()) {
            Point poi = iterator.next();
            x += poi.x;
            y += poi.y;
            length++;
        }
        lightPos.x = (x/length);
        lightPos.y = (y/length);
    }

    public void checkupdate(){
        if(!opencvUpdate){
            opencvUpdate = true;
            gondolaUpdate = System.currentTimeMillis();
        }

        if(System.currentTimeMillis() - gondolaUpdate > 10000) {
            if(testFix){
                updateCoordinates();
            }
            opencvUpdate = false;
        }
    }
}