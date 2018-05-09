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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.common.activities.SampleActivityBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public String luminance = "luminance";
    public int jpeg_count = 0;
    public boolean saved_picture = false;

    private Button takePictureButton;
    private Button getPictureButton;
    private TextureView textureView;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private RelativeLayout mFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        assert takePictureButton != null;
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        getPictureButton = (Button) findViewById(R.id.btn_getpicture);
        getPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(saved_picture){
                    File root = Environment.getExternalStorageDirectory();
                    ImageView IV = (ImageView) findViewById(R.id.picture);
                    Bitmap bMap = BitmapFactory.decodeFile(root + "/pic" + (jpeg_count - 1) + ".jpg");
                    bMap = CorrectBitmap(bMap, 90);
                    Bitmap graybMap = toGrayscale(bMap);
                    IV.setImageBitmap(graybMap);
                    int pixel = bMap.getPixel(0,0);
                    int R = Color.red(pixel);
                    int B = Color.blue(pixel);
                    int G = Color.green(pixel);
                    double Y = (0.299 * R + 0.587 * G + 0.114 * B);
                    double U = -0.147 * R - 0.289 * G + 0.436 * B;
                    double V =  0.615 * R - 0.515 * G - 0.100 * B;
                    R = (int) (Y + 1.140 * V);
                    G = (int) (Y - 0.395 * U - 0.581 * V);
                    B = (int) (Y + 2.032 * U);
                    //Color pixelcolor = new Color(R, G, B);
                    luminance = "" + Y;
                    coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor + "    " + luminance);
                    /*
                    for(int i = 0; i < graybMap.getWidth(); i++){
                        for(int j = 0; j < graybMap.getHeight(); j++){

                        }
                    }*/


                    saved_picture = false;
                }
            }
        });

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
        coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor + "    " + luminance);

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
                coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor + "    " + luminance);
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
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };
    final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
            createCameraPreview();
        }
    };
    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    protected void takePicture() {
        if(null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if (characteristics != null) {
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width = 640;
            int height = 480;
            if (jpegSizes != null && 0 < jpegSizes.length) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory()+"/pic" + jpeg_count + ".jpg");
            jpeg_count++;
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        luminance = String.valueOf(bytes[0]&0xff);
                        save(bytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    saved_picture = true;
                    createCameraPreview();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public static Bitmap CorrectBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }

        // Register for sensor updates

        sensorManager.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this, rotation,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
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
        coordinates.setText("" + xcoor + "    " + ycoor + "    " + zcoor + "    " + luminance);
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
            coordinates.setText("" + xcoor + "    " + ycoor + "    " + "Sent to gondola!" + "    " + luminance);
        }else{
            coordinates.setText("" + xcoor + "    " + ycoor + "    " + "Out of bounds!" + "    " + luminance);
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