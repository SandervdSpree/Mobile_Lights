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

    private TextView angleStep;
    private TextView coordinates;
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
    public int currentangle;
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
            FragmentTransaction secondtransaction = getSupportFragmentManager().beginTransaction();
            secondtransaction.replace(R.id.container, Camera2BasicFragment.newInstance());
            secondtransaction.commit();
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

        Button BtnStart = (Button) findViewById(R.id.btn_start);
        Button BtnStop = (Button) findViewById(R.id.btn_stop);

        BtnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                numSteps = 0;
                xcoor = 575;
                ycoor = 125;
                zcoor = 180;

                angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     Waiting for a step...    " + TEXT_NUM_STEPS + numSteps);
                coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);
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
        angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     " + currentangle + "    " + TEXT_NUM_STEPS + numSteps);

        currentangle = getAverageAzimuth();
        double rad = Math.toRadians(360 - (currentangle + 30));
        double xstep = stride*(Math.cos(rad));
        double ystep = stride*(Math.sin(rad));
        xcoor = xcoor - xstep;
        ycoor = ycoor + ystep;

        angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     " + currentangle + "    " + TEXT_NUM_STEPS + numSteps);
        coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor);
        boolean updateGondola = true;
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
            angleStep.setText("Azimuth: " + mAzimuth[aziCount] + "     " + currentangle + "    " + TEXT_NUM_STEPS + numSteps);
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