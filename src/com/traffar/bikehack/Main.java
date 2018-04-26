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
   Sensor accel;
   private static final String TAG = "BikeHack";
   public Analyzer analyzer;
   public SensorEventListener accelListner;
   public long activateTime = 0;

   SharedPreferences mPrefs;

   public Main()
   {
      accelListner = new SensorEventListener()
      {
         public long lastTime = 0;
         public int index = 0;
         private static final int DATAPOINTS = 20;
         float[][] latestValues = new float[DATAPOINTS][3];
         public void onAccuracyChanged(Sensor sensor, int acc) { }
         public void onSensorChanged(SensorEvent event)
         {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            long t = System.currentTimeMillis();
            if (t-activateTime < 60*1000)
            {
                view1.setText((CharSequence)String.format("%d %.1f, %.1f, %.1f\n", (int)(t - lastTime),
                            Float.valueOf(event.values[0]),
                            Float.valueOf(event.values[1]),
                            Float.valueOf(event.values[2])));
                Log.d(TAG, String.format("SENSOR %d  %.1f, %.1f, %.1f", (t-activateTime), event.values[0], event.values[1], event.values[2]));
            }

            latestValues[index][0] = x;
            latestValues[index][1] = y;
            latestValues[index][2] = z;


            int i5b = (index-5)%DATAPOINTS;
            int i4b = (index-4)%DATAPOINTS;
            int i3b = (index-3)%DATAPOINTS;
            int i2b = (index-2)%DATAPOINTS;
            int i1b = (index-1)%DATAPOINTS;

            lastTime = t;
            index += 1;
            index %= DATAPOINTS;

            if (latestValues[i5b][0] > 6 && latestValues[i4b][0] > 6 && x < 2)
            {
                Log.d(TAG, String.format("WHEEL SLEEP %.1f, %.1f, %.1f", x, y, z));
                Log.d(TAG, String.format("WHEEL SLEEP %.1f, %.1f, %.1f, %.1f, %.1f", latestValues[i5b][0], latestValues[i4b][0], latestValues[i3b][0], latestValues[i2b][0], latestValues[i1b][0]));
            }

            lastTime = t;
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
      sensorManager.registerListener(accelListner, accel,
            SensorManager.SENSOR_DELAY_GAME);
      view1 = (TextView)findViewById(R.id.view1);
      view2 = (TextView)findViewById(R.id.view2);
      analyzer = new Analyzer();
      initSounds();
      button = (Button) findViewById(R.id.button_start);
      button.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
              activateTime = System.currentTimeMillis();
          }
      });
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
