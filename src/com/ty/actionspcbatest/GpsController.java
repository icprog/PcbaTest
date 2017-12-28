package com.ty.actionspcbatest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

public class GpsController {
	public static final int GPS_MSG_UPDATE = 0;
	public static final int GPS_MSG_UPDATE_DBG = 1;
	private Handler mUiHandler;
	private Context mContext;
	private static final String PROVIDER = "gps";
	private GpsStatus.Listener mGpsStatusListener;
	private LocationListener mLocationListener;
	private LocationManager mLocationManager;
	private int mSatelliteNum;
	private List<Float> mSatelliteSignal;
	private boolean mIsGpsConnecting;
	
	private HandlerThread mWorkThread;
	private Handler mWorkHandler;
	public static final int MSG_GPS_OPEN = 0;
	public static final int MSG_GPS_INIT = 1;
	public static final int MSG_GPS_UPDATE = 2;
	
	private BroadcastReceiver mLocReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (LocationManager.MODE_CHANGED_ACTION.equals(action)) {
            	android.util.Log.i("hcj", "MODE_CHANGED_ACTION");
            	if (gpsIsOpen()) {
        			mWorkHandler.sendEmptyMessage(MSG_GPS_INIT);
        		}
            }
        }
    };
	
	public GpsController(Context context, Handler handler){
		mContext = context;
		mUiHandler = handler;
		
		mLocationManager = (LocationManager)this.mContext.getSystemService(Context.LOCATION_SERVICE);
		mGpsStatusListener = new GpsStatus.Listener(){
			@Override
			public void onGpsStatusChanged(int arg0) {
				android.util.Log.i("hcj", "onGpsStatusChanged arg0="+arg0);
				/*
				GpsStatus localGpsStatus = mLocationManager.getGpsStatus(null);
				updateGpsStatus(arg0,localGpsStatus);
				*/
				mWorkHandler.sendEmptyMessage(MSG_GPS_UPDATE);
			}			
		};
		mLocationListener = new LocationListener(){
			@Override
			public void onLocationChanged(Location arg0) {
				//mUiHandler.sendMessage(mUiHandler.obtainMessage(GPS_MSG_UPDATE_DBG, "onLocationChanged"));
			}

			@Override
			public void onProviderDisabled(String arg0) {
			}

			@Override
			public void onProviderEnabled(String arg0) {
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
				android.util.Log.i("hcj", "onStatusChanged");
				/*
				GpsStatus localGpsStatus = mLocationManager.getGpsStatus(null);
				updateGpsStatus(0,localGpsStatus);
				*/
				mWorkHandler.sendEmptyMessage(MSG_GPS_UPDATE);
			}			
		};
		mSatelliteSignal = new ArrayList<Float>();
		
		mWorkThread = new HandlerThread("gps_work");
		mWorkThread.start();
		mWorkHandler = new Handler(mWorkThread.getLooper()){
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
					case MSG_GPS_OPEN:
						gpsOpen();
						break;
					case MSG_GPS_INIT:
						gpsInit();
						break;
					case MSG_GPS_UPDATE:
						GpsStatus localGpsStatus = mLocationManager.getGpsStatus(null);
						updateGpsStatus(0,localGpsStatus);
						break;
				}
			}
		};
	}
	
	public void start(){
		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LocationManager.MODE_CHANGED_ACTION);
		mContext.registerReceiver(mLocReceiver, intentFilter);
		mWorkHandler.sendEmptyMessage(MSG_GPS_OPEN);
		//gpsOpen();
	}
	
	public void stop(){
		mContext.unregisterReceiver(mLocReceiver);
		mLocationManager.removeGpsStatusListener(mGpsStatusListener);
		mLocationManager.removeUpdates(mLocationListener);
		gpsClose();
		mIsGpsConnecting = false;
	}
	
	public boolean isGpsConnecting(){
		return mIsGpsConnecting;
	}
	
	public int getSatelliteNumber(){
		return this.mSatelliteNum;
	}
	
	public String getSatelliteSignals(){
		return mSatelliteSignal.toString();
	}
	
	private void gpsOpen(){
		if (gpsIsOpen()) {
			//gpsInit();
			mWorkHandler.sendEmptyMessage(MSG_GPS_INIT);
			return;
		}
		gpsEnable(true);
		//mWorkHandler.sendEmptyMessageDelayed(MSG_GPS_OPEN, 5000);
        /*
		if (!gpsIsOpen()){
			//mUiHandler.postDelayed(mOpenGpsRunnable, 1000);
			mWorkHandler.sendEmptyMessageDelayed(MSG_GPS_OPEN, 1000);
		}else{
			//gpsInit();
			mWorkHandler.sendEmptyMessage(MSG_GPS_INIT);
		}*/
	}
	
	private boolean gpsIsOpen(){
		if (false){//Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
			int mode = Settings.Secure.getInt(mContext.getContentResolver(), 
					Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
			return (mode != Settings.Secure.LOCATION_MODE_OFF);
		}else{
			return mLocationManager.isProviderEnabled(PROVIDER);
		}
	}
	
	@SuppressLint("NewApi")
	private boolean isRestricted() {
        final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        return um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
    }
	
	private void gpsEnable(boolean enable){
		Log.i("hcj", "gpsEnable "+enable);
		if (false){//Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
			if(isRestricted()){
				Log.i("hcj", "gpsEnable restricted");
				return;
			}
			int currMode = Settings.Secure.getInt(mContext.getContentResolver(), 
					Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
			int newMode = enable ? Settings.Secure.LOCATION_MODE_HIGH_ACCURACY : Settings.Secure.LOCATION_MODE_OFF;
			Settings.Secure.putInt(mContext.getContentResolver(), 
				Settings.Secure.LOCATION_MODE, newMode);
			Intent intent = new Intent("com.android.settings.location.MODE_CHANGING");
	        intent.putExtra("CURRENT_MODE", currMode);
	        intent.putExtra("NEW_MODE", newMode);
	        mContext.sendBroadcast(intent, android.Manifest.permission.WRITE_SECURE_SETTINGS);
		}else{
			Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(), PROVIDER, enable);	
		}
	}
	
	private void gpsInit(){
		//mUiHandler.sendMessage(mUiHandler.obtainMessage(GPS_MSG_UPDATE_DBG, "gpsInit"));
		Log.i("hcj", "gpsInit");
		mLocationManager.getLastKnownLocation(PROVIDER);
		boolean isSuccess = mLocationManager.addGpsStatusListener(mGpsStatusListener);
		android.util.Log.i("hcj", "addGpsStatusListener isSuccess="+isSuccess);
		mLocationManager.requestLocationUpdates(PROVIDER, 2000L, 0, mLocationListener);
		updateGpsStatus(0,null);
	}
	/*
	private Runnable mOpenGpsRunnable = new Runnable(){
		@Override
		public void run(){
			gpsOpen();
		}
	};
	*/
	private void gpsClose(){
		if (!gpsIsOpen()) return;
		//Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(), PROVIDER, false);
		gpsEnable(false);
	}
	
	private void updateGpsStatus(int paramInt, GpsStatus gpsStatus){		
		//mUiHandler.sendMessage(mUiHandler.obtainMessage(GPS_MSG_UPDATE_DBG, "updateGpsStatus"));
		mIsGpsConnecting = true;
		if(gpsStatus == null){
			mSatelliteNum = 0;
			mUiHandler.sendEmptyMessage(GPS_MSG_UPDATE);
			//mUiHandler.sendMessage(mUiHandler.obtainMessage(GPS_MSG_UPDATE_DBG, "gpsStatus null"));
			return;
		}
		
		//mSatelliteSignal.clear();
		List<Float> signals = new ArrayList<Float>();
		Iterator<GpsSatellite> iterator = gpsStatus.getSatellites().iterator();
		while(iterator.hasNext()){
			float f = ((GpsSatellite)iterator.next()).getSnr();
			//android.util.Log.i("hcj", "f="+f);
			 if (f <30){
				 continue;
			 }
			 Float localFloat = Float.valueOf(f);
			 //mSatelliteSignal.add(localFloat);
			 signals.add(localFloat);
		}
		//mSatelliteNum = mSatelliteSignal.size();
		mSatelliteNum = signals.size();
		android.util.Log.i("hcj", "gpsStatus mSatelliteNum="
				+mSatelliteNum+",signales="+getSatelliteSignals());
		mUiHandler.sendMessage(mUiHandler.obtainMessage(GPS_MSG_UPDATE, signals));
		//mUiHandler.sendMessage(mUiHandler.obtainMessage(GPS_MSG_UPDATE_DBG, "gpsStatus mSatelliteNum="
				//+mSatelliteNum+",signales="+getSatelliteSignals()));
	}
}
