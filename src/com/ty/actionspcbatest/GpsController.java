package com.ty.actionspcbatest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

public class GpsController {
	public static final int GPS_MSG_UPDATE = 0;
	private Handler mUiHandler;
	private Context mContext;
	private static final String PROVIDER = "gps";
	private GpsStatus.Listener mGpsStatusListener;
	private LocationListener mLocationListener;
	private LocationManager mLocationManager;
	private int mSatelliteNum;
	private List mSatelliteSignal;
	private boolean mIsGpsConnecting;
	
	public GpsController(Context context, Handler handler){
		mContext = context;
		mUiHandler = handler;
		
		mLocationManager = (LocationManager)this.mContext.getSystemService("location");
		mGpsStatusListener = new GpsStatus.Listener(){
			@Override
			public void onGpsStatusChanged(int arg0) {
				GpsStatus localGpsStatus = mLocationManager.getGpsStatus(null);
				android.util.Log.i("hcj", "onGpsStatusChanged arg0="+arg0+",localGpsStatus="+localGpsStatus);
				updateGpsStatus(arg0,localGpsStatus);
			}			
		};
		mLocationListener = new LocationListener(){
			@Override
			public void onLocationChanged(Location arg0) {
			}

			@Override
			public void onProviderDisabled(String arg0) {
			}

			@Override
			public void onProviderEnabled(String arg0) {
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			}			
		};
		mSatelliteSignal = new ArrayList();
	}
	
	public void start(){
		//new Thread(){
			//public void run(){
		android.util.Log.i("hcj", "start");
				gpsOpen();				
			//}
		//}.start();
	}
	
	public void stop(){
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
		if (mLocationManager.isProviderEnabled(PROVIDER)) {
			gpsInit();
			return;
		}
		Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(), PROVIDER, true);		
		if (!mLocationManager.isProviderEnabled(PROVIDER)){
			mUiHandler.postDelayed(mOpenGpsRunnable, 1000);
		}else{
			gpsInit();
		}
	}
	
	private void gpsInit(){
		Log.i("hcj", "gpsInit");
		mLocationManager.getLastKnownLocation(PROVIDER);
		mLocationManager.requestLocationUpdates(PROVIDER, 2000L, 0, mLocationListener);
		boolean isSuccess = mLocationManager.addGpsStatusListener(mGpsStatusListener);
		android.util.Log.i("hcj", "addGpsStatusListener isSuccess="+isSuccess);
		updateGpsStatus(0,null);
	}
	
	private Runnable mOpenGpsRunnable = new Runnable(){
		@Override
		public void run(){
			gpsOpen();
		}
	};
	
	private void gpsClose(){
		if (!mLocationManager.isProviderEnabled(PROVIDER)) return;
		Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(), PROVIDER, false);
	}
	
	private void updateGpsStatus(int paramInt, GpsStatus gpsStatus){		
		/*
		if(paramInt != 4){
			return;
		}*/
		mIsGpsConnecting = true;
		if(gpsStatus == null){
			mSatelliteNum = 0;
			mUiHandler.sendEmptyMessage(GPS_MSG_UPDATE);
			return;
		}
		Iterator<GpsSatellite> iterator = gpsStatus.getSatellites().iterator();
		while(iterator.hasNext()){
			float f = ((GpsSatellite)iterator.next()).getSnr();
			android.util.Log.i("hcj", "f="+f);
			 if (f <30){
				 continue;
			 }
			 Float localFloat = Float.valueOf(f);
			 mSatelliteSignal.add(localFloat);
		     mSatelliteNum ++;
		}
		mUiHandler.sendEmptyMessage(GPS_MSG_UPDATE);
	}
}
