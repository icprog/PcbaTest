/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ty.actionspcbatest;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

public class VisualizerFx extends LinearLayout {
    private static final String TAG = "VisualizerFx";

    private static final float VISUALIZER_HEIGHT_DIP = 150f;

    private MediaPlayer mMediaPlayer;
    private Visualizer mVisualizer;

    private VisualizerView mVisualizerView;

	public VisualizerFx(Context context){
		super(context);
		initialize();
	}

	public VisualizerFx(Context context, AttributeSet atributeSet){
		super(context, atributeSet);
		initialize();
	}
	
    public void initialize() {
        //setVolumeControlStream(AudioManager.STREAM_MUSIC);

        //mStatusTextView = new TextView(this);
		
        // Create the MediaPlayer
        mMediaPlayer = MediaPlayer.create(getContext(), R.raw.scarborough_fair);
        
        setupVisualizerFxAndUI();
        // Make sure the visualizer is enabled only when you actually want to receive data, and
        // when it makes sense to receive data.
        mVisualizer.setEnabled(true);

        // When the stream ends, we don't need to collect any more data. We don't do this in
        // setupVisualizerFxAndUI because we likely want to have more, non-Visualizer related code
        // in this callback.
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mediaPlayer) {
                //mVisualizer.setEnabled(false);
                mMediaPlayer.start();
            }
        });

        //mMediaPlayer.start();
    }

    private void setupVisualizerFxAndUI() {
        // Create a VisualizerView (defined below), which will render the simplified audio
        // wave form to a Canvas.
        mVisualizerView = new VisualizerView(getContext());
        mVisualizerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                (int)(VISUALIZER_HEIGHT_DIP * getResources().getDisplayMetrics().density)));
        this.removeAllViews();
        this.addView(mVisualizerView);

        // Create the Visualizer object and attach it to our media player.
        mVisualizer = new Visualizer(mMediaPlayer.getAudioSessionId());
        mVisualizer.setCaptureSize(/*Visualizer.getCaptureSizeRange()[1]*/256);
        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                    int samplingRate) {
                mVisualizerView.updateVisualizer(bytes);
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            	mVisualizerView.updateVisualizer(bytes);
            }
        }, Visualizer.getMaxCaptureRate() / 2, false, true);
    }
    
    public void start(){
    	if (mMediaPlayer != null) {
    		mMediaPlayer.start();
    	}
    }
    
    public void pause(){
    	if (mMediaPlayer != null) {
    		mMediaPlayer.pause();
    	}
    }
    
    public void stop(){
    	if (mMediaPlayer != null) {
    		mMediaPlayer.stop();
    	}
    }
    
    public void toggle(){
    	if (mMediaPlayer != null) {
    		if (mMediaPlayer.isPlaying()) {
    	        mMediaPlayer.pause();
    	    }else{
    	    	mMediaPlayer.start();
    	    }
    	}
    }
    
    public void release(){
    	mVisualizer.setEnabled(false);
    	releaseMediaPlayer();
    }
    
    public void changePlayFile(Uri uri){
    	try{
    		Log.i("hcj", "changePlayFile uri="+uri);
    		mVisualizer.setEnabled(false);
    		releaseMediaPlayer();
    		mMediaPlayer = MediaPlayer.create(this.mContext, uri);
    		setupVisualizerFxAndUI();
    		mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){            
              public void onCompletion(MediaPlayer paramAnonymousMediaPlayer){              
                mMediaPlayer.start();
              }
            });
    		mMediaPlayer.start();
    		mVisualizer.setEnabled(true);
    	}catch(Exception e){
    		Log.i("hcj", "changePlayFile e="+e);
    	}
    }
    
    private void releaseMediaPlayer(){    
      if (mMediaPlayer != null){      
        if (mMediaPlayer.isPlaying()) {
          mMediaPlayer.stop();
        }
        mMediaPlayer.release();
        mMediaPlayer = null;
      }
    }
    
    public void testStart(File file){
    	//mMediaPlayer = MediaPlayer.create(getContext(), R.raw.scarborough_fair);
    	mMediaPlayer = MediaPlayer.create(getContext(), Uri.fromFile(file));
    	mMediaPlayer.start();
    }
    
    public void testStop(){
    	mMediaPlayer.stop();
    	mMediaPlayer.release();
    	mMediaPlayer = null;
    }
}

class VisualizerView extends View {
    private byte[] mBytes;
    private float[] mPoints;
    private Rect mRect = new Rect();

    private Paint mForePaint = new Paint();
    private static final int mSpectrumNum = 48;

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mBytes = null;

        mForePaint.setStrokeWidth(8f);
        mForePaint.setAntiAlias(true);
        mForePaint.setColor(Color.rgb(0, 128, 255));
    }

    public void updateVisualizer(byte[] bytes) {
        //mBytes = bytes;
    	byte[] arrayOfByte = new byte[bytes.length / 2 + 1];
        arrayOfByte[0] = ((byte)Math.abs(bytes[0]));
        int j = 2;
        int i = 1;
        while (i < mSpectrumNum)
        {
          arrayOfByte[i] = ((byte)(int)Math.hypot(bytes[j], bytes[(j + 1)]));
          j += 2;
          i += 1;
        }
        this.mBytes = arrayOfByte;
        invalidate();
    }
/*
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mBytes == null) {
            return;
        }

        if (mPoints == null || mPoints.length < mBytes.length * 4) {
            mPoints = new float[mBytes.length * 4];
        }

        mRect.set(0, 0, getWidth(), getHeight());

        for (int i = 0; i < mBytes.length - 1; i++) {
            mPoints[i * 4] = mRect.width() * i / (mBytes.length - 1);
            mPoints[i * 4 + 1] = mRect.height() / 2
                    + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
            mPoints[i * 4 + 2] = mRect.width() * (i + 1) / (mBytes.length - 1);
            mPoints[i * 4 + 3] = mRect.height() / 2
                    + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
        }

        canvas.drawLines(mPoints, mForePaint);
    }
*/    


   protected void onDraw(Canvas paramCanvas){    
      super.onDraw(paramCanvas);
      if (this.mBytes == null) {
        return;
      }
      if ((this.mPoints == null) || (this.mPoints.length < this.mBytes.length * 4)) {
        this.mPoints = new float[this.mBytes.length * 4];
      }
      this.mRect.set(0, 0, getWidth(), getHeight());
      int j = this.mRect.width() / mSpectrumNum;
      int k = this.mRect.height();
      
      int i = 0;
      while (i < mSpectrumNum)
      {
        if (this.mBytes[i] < 0) {
          this.mBytes[i] = Byte.MAX_VALUE;
        }
        int m = j * i + j / 2;
        this.mPoints[(i * 4)] = m;
        this.mPoints[(i * 4 + 1)] = k;
        this.mPoints[(i * 4 + 2)] = m;
        this.mPoints[(i * 4 + 3)] = (k - this.mBytes[i]);
        i += 1;
      }
      paramCanvas.drawLines(this.mPoints, this.mForePaint);
    }
}
