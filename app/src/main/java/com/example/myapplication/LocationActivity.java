package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class LocationActivity extends AppCompatActivity implements SensorEventListener {
    TextView TV1;
    LocationManager lm;
    //Screen recorder
    private ToggleButton toggleButton;
    private Chronometer chronometer;
    private TextView readtext;
    private int screenDensity;
    private MediaRecorder mediaRecorder;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjectionCallback mediaProjectionCallback;
    private MediaProjection mediaProjection;
     private VirtualDisplay virtualDisplay;

     private static final String TAG = "LocationActivity";
     private static final int DISPLAY_WIDTH = 720;
     private static final int DISPLAY_HEIGHT = 1280;
    private static final int REQUEST_PERMISSION=10;
    private static final int REQUEST_CODE = 1000;

    private String videoUrl="";
    private static final SparseIntArray ORIENTATION=new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0,90);
        ORIENTATION.append(Surface.ROTATION_90,0);
        ORIENTATION.append(Surface.ROTATION_180,270);
        ORIENTATION.append(Surface.ROTATION_270,180);
    }




    //Step counter
    private SensorManager sensorManager =null;
    private Sensor stepSensor;
    private int totalSteps = 0;
    private int previewsTotalSteps = 0;
    private ProgressBar progressBar;
    private TextView steps;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        //Screen recording

        chronometer =(Chronometer) findViewById(R.id.crmtr);
        readtext = (TextView)findViewById(R.id.tvRecord);
        toggleButton = (ToggleButton)findViewById(R.id.tgbtn);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        screenDensity = metrics.densityDpi;


        //Step counter
        TV1 = findViewById(R.id.TV1);
        progressBar = findViewById(R.id.pgbar);
        steps = findViewById(R.id.steps);

        resetSteps();
        loadData();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        //Screen Recording
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(LocationActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED  &&
                ContextCompat.checkSelfPermission(LocationActivity.this,Manifest.permission.RECORD_AUDIO)!=
                PackageManager.PERMISSION_GRANTED)
                {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(LocationActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)||
                    ActivityCompat.shouldShowRequestPermissionRationale(LocationActivity.this,
                            Manifest.permission.RECORD_AUDIO))
                    {
                     toggleButton.setChecked(false);
                        Snackbar.make(findViewById(android.R.id.content),
                                R.string.permission_text,Snackbar.LENGTH_INDEFINITE).setAction("Enable", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(LocationActivity.this,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.RECORD_AUDIO},REQUEST_PERMISSION);
                            }
                        }).show();
                    }
                    else {
                        ActivityCompat.requestPermissions(LocationActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO},REQUEST_PERMISSION);
                    }
                }
                else
                {
                    onScreenShare(view);
                }
            }
        });


        //runtime code permission
//Location
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&(ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED));

        {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION},0);
        }

        lm = (LocationManager)getSystemService(LOCATION_SERVICE);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                TV1.setText("lat:-"+latitude+"\nlong:-"+longitude);

                try {
                    Geocoder geocoder = new Geocoder(LocationActivity.this);
                    List<Address> list = geocoder.getFromLocation(latitude,longitude,1);
                    String addressLine = list.get(0).getAddressLine(0);

                    TV1.append("\n\n"+addressLine
                    );

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
//Screen recorder

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode!=REQUEST_CODE){
            return;
        }if (requestCode!=  RESULT_OK){
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            toggleButton.setChecked(true);
            return;
        }
        mediaProjectionCallback=new MediaProjectionCallback();
        mediaProjection=mediaProjectionManager.getMediaProjection(resultCode,data);
        mediaProjection.registerCallback(mediaProjectionCallback,null);

        virtualDisplay=createVirtualDisplay();
        mediaRecorder.start();

    }

    private VirtualDisplay createVirtualDisplay()
    {
        return mediaProjection.createVirtualDisplay(TAG,DISPLAY_HEIGHT,DISPLAY_WIDTH,screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,mediaRecorder.getSurface(),null,null);

    }

    private void onScreenShare(View view)
    {
        if (((ToggleButton)view).isChecked())
        {
            readtext.setVisibility(View.VISIBLE);
            chronometer.start();

            initiateRecorder();
            shareScreen();
        }
        else
        {
            readtext.setVisibility(View.INVISIBLE);
            mediaProjection.stop();
            mediaRecorder.reset();

            stopScreenShareing();
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_PERMISSION: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED)
                {
                    onScreenShare(toggleButton);
                }
                else {
                    toggleButton.setChecked(false);
                    Snackbar.make(findViewById(android.R.id.content), R.string.permission_text,
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE", new  View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

                            startActivity(intent);
                        }
                    }).show();
                }
                return;
            }
        }

    }

    private void stopScreenShareing() {
        if (virtualDisplay==null)
        {
            return;
        }
        virtualDisplay.release();
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mediaProjection!=null);
        {
            mediaProjection.unregisterCallback(mediaProjectionCallback);
            mediaProjection.stop();
            mediaProjection =null;
        }
    }

    private void shareScreen() {
        if (mediaProjection==null)
        {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),REQUEST_CODE);
            return;
        }
        virtualDisplay=createVirtualDisplay();
        mediaRecorder.start();
    }

    private void initiateRecorder() {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            videoUrl= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+
                    new StringBuilder("/ScreenRecorder ").append(new SimpleDateFormat("dd-mm-yyyy-hh mm ss").
                            format(new Date())).append(".mp4").toString();
            mediaRecorder.setOutputFile(videoUrl);
            mediaRecorder.setVideoSize(DISPLAY_WIDTH,DISPLAY_HEIGHT);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(512*1000);
            mediaRecorder.setVideoFrameRate(2);

            int rotation=getWindowManager().getDefaultDisplay().getRotation();
            int orientation=ORIENTATION.get(rotation +90);

            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
        }catch (Exception e){
            e.printStackTrace();

        }

    }


    //Steps Counter
    protected void onResume() {

        super.onResume();
        if (stepSensor ==null){
            Toast.makeText(this,"This device has no Sensor",Toast.LENGTH_SHORT).show();
        }else {
            sensorManager.registerListener(this,stepSensor,SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (event.sensor.getType() ==Sensor.TYPE_STEP_COUNTER){
            totalSteps = (int) event.values[0];
            int currentsteps = totalSteps-previewsTotalSteps;
            steps.setText(String.valueOf(currentsteps));

            progressBar.setProgress(currentsteps);
        }
    }

   private void resetSteps(){
        steps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(LocationActivity.this, "Long press to reset Steps", Toast.LENGTH_SHORT).show();

            }
        });
        steps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                previewsTotalSteps = totalSteps;
                steps.setText(0);
                progressBar.setProgress(0);
                saveData();
            }
        });
    }

    private void saveData() {
        SharedPreferences sharedpref = getSharedPreferences("mypref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpref.edit();
        editor.putString("key1",String.valueOf(previewsTotalSteps));
        editor.apply();
    }
    private void loadData(){
        SharedPreferences sharedpref = getSharedPreferences("mypref", Context.MODE_PRIVATE);
        int savedNumber = (int) sharedpref.getFloat("key1",0f);
        previewsTotalSteps = savedNumber;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    //Screen record
    private class MediaProjectionCallback extends MediaProjection.Callback
    {
        public void onStop(){
            if (toggleButton.isChecked())
            {
                toggleButton.setChecked(false);
                mediaRecorder.stop();
                mediaRecorder.reset();
            }
            mediaProjection=null;
            stopScreenShareing();
        }

    }

    //
}