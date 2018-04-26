package com.traffar.bikehack;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.AlertDialog;
import android.view.WindowManager;
import android.widget.Button;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

import android.media.SoundPool;
import android.media.AudioManager;
import android.preference.PreferenceManager;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Comparator;
import java.util.HashMap;


public class Main extends Activity
{
   TextView view1;
   TextView view2;
   Button button;
   SensorManager sensorManager;
   LocationManager locationManager;
   Sensor accel;
   private static final String TAG = "BikeHack";
   public Analyzer analyzer;
   public SensorEventListener accelListner;
   public LocationListener locListner;
   public long activateTime = 0;

   SharedPreferences mPrefs;

   public Main()
   {
       locListner = new LocationListener()
       {
           public float speed1 = 0;
           public float speed2 = 0;
           public float speed3 = 0;

           public Location prev_loc = new Location(LocationManager.GPS_PROVIDER);

           @Override
           public void onLocationChanged(Location loc) {
               String longitude = "Longitude: " + loc.getLongitude();
               String latitude = "Latitude: " + loc.getLatitude();
               Log.d(TAG, String.format("GPS %.6f, %.6f", loc.getLongitude(), loc.getLatitude()));
               view2.setText((CharSequence)String.format("GPS %.6f\n%.6f\n%.6f", loc.getLongitude(), loc.getLatitude(), loc.distanceTo(prev_loc)));
               prev_loc = loc;
               speed3 = speed2;
               speed2 = speed1;
               speed1 = loc.distanceTo(prev_loc);
           }

           @Override
           public void onProviderDisabled(String provider) {}

           @Override
           public void onProviderEnabled(String provider) {}

           @Override
           public void onStatusChanged(String provider, int status, Bundle extras) {} 
       };
           
      accelListner = new SensorEventListener()
      {
         public long lastTime = 0;
         public int index = 0;
         private static final int DATAPOINTS = 20;
         private static final float THRESHOLD = 100;

         float[][] latestValues = new float[DATAPOINTS][3];
         public void onAccuracyChanged(Sensor sensor, int acc) { }
         private float d(float x[], float y[])
         {
             return
                 (x[0]-y[0])*(x[0]-y[0]);
         }

         public void onSensorChanged(SensorEvent event)
         {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long t = System.currentTimeMillis();

            latestValues[index][0] = x;
            latestValues[index][1] = y;
            latestValues[index][2] = z;


            int i5b = (index-5)%DATAPOINTS;
            int i4b = (index-4)%DATAPOINTS;
            int i3b = (index-3)%DATAPOINTS;
            int i2b = (index-2)%DATAPOINTS;
            int i1b = (index-1)%DATAPOINTS;
            if (i5b < 0) i5b += 20;
            if (i4b < 0) i4b += 20;
            if (i3b < 0) i3b += 20;
            if (i2b < 0) i2b += 20;
            if (i1b < 0) i1b += 20;
            //Log.d(TAG, String.format("%d %d %d %d %d %d", index, i1b, i2b, i3b, i4b, i5b));

            float shock =
                d(latestValues[i5b], latestValues[i4b])
                +d(latestValues[i4b], latestValues[i3b])
                +d(latestValues[i3b], latestValues[i2b])
                +d(latestValues[i2b], latestValues[i1b])
                +d(latestValues[i1b], latestValues[index]);
            if (shock > THRESHOLD && t - lastTime > 3000)
            {
                lastTime = t;
                view1.setText((CharSequence)String.format("%d %.1f, %.1f, %.1f\n", (int)(t - lastTime),
                            Float.valueOf(event.values[0]),
                            Float.valueOf(event.values[1]),
                            Float.valueOf(event.values[2])));
                Log.d(TAG, String.format("SENSOR %d  %.1f, %.1f, %.1f shock %.1f", (t-activateTime), event.values[0], event.values[1], event.values[2], shock));
            }

            if (t-activateTime < 20*1000)
            {
                view1.setText((CharSequence)String.format("%d %.1f, %.1f, %.1f\n", (int)(t - lastTime),
                            Float.valueOf(event.values[0]),
                            Float.valueOf(event.values[1]),
                            Float.valueOf(event.values[2])));
                Log.d(TAG, String.format("SENSOR %d  %.1f, %.1f, %.1f shock %.1f", (t-activateTime), event.values[0], event.values[1], event.values[2], shock));
            }

            index += 1;
            index %= DATAPOINTS;
         }
      };
   }

   public void onCreate(Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
      setContentView(R.layout.main);
      sensorManager = (SensorManager)getSystemService("sensor");
      accel = sensorManager.getDefaultSensor(10);
      sensorManager.registerListener(accelListner, accel, SensorManager.SENSOR_DELAY_GAME);
      locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
      view1 = (TextView)findViewById(R.id.view1);
      view2 = (TextView)findViewById(R.id.view2);
      analyzer = new Analyzer();
      initSounds();
      button = (Button) findViewById(R.id.button_start);
      button.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
              if (activateTime != 0)
                  activateTime = 0;
              else
                  activateTime = System.currentTimeMillis();
          }
      });
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locListner);
   }


   protected void onPause()
   {
      super.onPause();
   }

   protected void onResume()
   {
      super.onResume();
   }

   public class Analyzer
   {
      public void go(float[][] values)
      {
      }
   }

   private SoundPool soundPool;
   public int Beep1;
   public int Shutter;

   /** Populate the SoundPool*/
   public void initSounds()
   {
      soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 100);
      Beep1 = soundPool.load(this, R.raw.beep1, 1);
      Shutter = soundPool.load(this, R.raw.shutter, 1);
   }

   /** Play a given sound in the soundPool */
   public void playSound(int soundID)
   {
      if (soundPool == null)
      {
         initSounds();
      }
      float volume = (float)0.5;
      // play sound with same right and left volume, with a priority of 1, 
      // zero repeats (i.e play once), and a playback rate of 1f
      soundPool.play(soundID, volume, volume, 1, 0, 1f);
   }
}
