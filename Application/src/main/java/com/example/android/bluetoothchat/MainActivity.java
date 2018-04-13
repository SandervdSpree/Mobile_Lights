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
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.android.common.activities.SampleActivityBase;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase implements SensorEventListener, StepListener {

    private TextView angle;
    private TextView azi;
    private TextView TvSteps;
    private TextView xCoordinate;
    private TextView yCoordinate;
    private TextView zCoordinate;
    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    // Main View
    private RelativeLayout mFrame;

    // Sensors & SensorManager
    private Sensor accelerometer;
    private Sensor rotation;
    BluetoothChatFragment fragment = new BluetoothChatFragment();

    float[] orientation = new float[3];
    float[] rMat = new float[9];

    int[] mAzimuth = new int[99];
    int aziCount = 0;

    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps;

    public static final String TAG = "MainActivity";

    public int stride = 78;
    public double xcoor = 575;
    public double ycoor = 125;
    public double zcoor = 180;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
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

        angle = (TextView) findViewById(R.id.angle);
        azi = (TextView) findViewById(R.id.azi);
        azi.setText("Waiting for a step...");
        TvSteps = (TextView) findViewById(R.id.tv_steps);
        xCoordinate = (TextView) findViewById(R.id.xCoordinate);
        xCoordinate.setText("" + xcoor);
        yCoordinate = (TextView) findViewById(R.id.yCoordinate);
        yCoordinate.setText("" + ycoor);
        zCoordinate = (TextView) findViewById(R.id.zCoordinate);
        zCoordinate.setText("" + zcoor);

        Button BtnStart = (Button) findViewById(R.id.btn_start);
        Button BtnStop = (Button) findViewById(R.id.btn_stop);

        BtnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                numSteps = 0;
                xcoor = 575;
                ycoor = 125;
                zcoor = 180;

                TvSteps.setText(TEXT_NUM_STEPS + numSteps);
                xCoordinate.setText("" + xcoor);
                yCoordinate.setText("" + ycoor);
                zCoordinate.setText("" + zcoor);
                sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(MainActivity.this, rotation, SensorManager.SENSOR_DELAY_GAME);
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
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();


        // Register for sensor updates

        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this, rotation,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister all sensors
        sensorManager.unregisterListener(this);
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        TvSteps.setText(TEXT_NUM_STEPS + numSteps);

        int currentAngle = getAverageAzimuth();
        double rad = Math.toRadians(360 - (currentAngle + 30));
        double xstep = stride*(Math.cos(rad));
        double ystep = stride*(Math.sin(rad));
        xcoor = xcoor - xstep;
        ycoor = ycoor + ystep;

        azi.setText("" + currentAngle);
        xCoordinate.setText("" + xcoor);
        yCoordinate.setText("" + ycoor);
        boolean updateGondola = true;
        if(xcoor < 100.0 || xcoor > 600.0) {
            updateGondola = false;
            zCoordinate.setText("Out of bounds!");
        }else if(ycoor < 100.0 || ycoor > 500.0){
            updateGondola = false;
            zCoordinate.setText("Out of bounds!");
        }else if(zcoor < 0 || zcoor > 225){
            updateGondola = false;
            zCoordinate.setText("Out of bounds!");
        }
        if(updateGondola) {
            fragment.sendMessageFromMain((int) xcoor, (int) ycoor, (int) zcoor);
            zCoordinate.setText("Sent to Gondola!");
        }
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
            angle.setText("Azimuth: " + mAzimuth[aziCount]);
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
}