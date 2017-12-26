package com.ty.actionspcbatest;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class WifiController {
	private static final String TAG = "hcj.WifiController";
	private WifiManager mWifiManager;
	private WifiInfo mWifiInfo;
	private Handler mUiHandler;
	private Context mContext;
	private boolean mScanning;
	private boolean mStarted;
	public static final int WIFI_MSG_OPENING = 0;
	public static final int WIFI_MSG_OPENED = 1;
	public static final int WIFI_MSG_SCANNING = 2;
	public static final int WIFI_MSG_NONE_DEVICE = 3;
	public static final int WIFI_MSG_PASS = 4;
	public static final int WIFI_MSG_CLOSE = 5;

	private HandlerThread mWorkThread;
	private Handler mWorkHandler;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			Log.i(TAG, "action="+action);
			if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)){
				int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,WifiManager.WIFI_STATE_UNKNOWN);
				boolean enabled = (state == WifiManager.WIFI_STATE_ENABLED);
				if(enabled){
					mUiHandler.sendEmptyMessage(WIFI_MSG_SCANNING);
					mWorkHandler.sendEmptyMessage(WIFI_MSG_SCANNING);
					//startScan();
				}
			}
		}
	};
	
	public WifiController(Context context, Handler handler){
		mContext = context;
		mUiHandler = handler;
		
		mWifiManager = ((WifiManager)context.getSystemService("wifi"));
		mWifiInfo = mWifiManager.getConnectionInfo();

		mWorkThread = new HandlerThread("wifi_work");
		mWorkThread.start();
		mWorkHandler = new Handler(mWorkThread.getLooper()){
			@Override
			public void handleMessage(Message msg) {
				switch(msg.what){
					case WIFI_MSG_OPENING:
						wifiEnable(true);
						break;
					case WIFI_MSG_CLOSE:
						wifiEnable(false);
						break;
					case WIFI_MSG_SCANNING:
						startScan();
						break;
						/*
					case MSG_GPS_UPDATE:
						GpsStatus localGpsStatus = mLocationManager.getGpsStatus(null);
						updateGpsStatus(0,localGpsStatus);
						break;*/
				}
			}
		};
	}
	
	public void start(){
		Log.i(TAG, "start");
		registerListener();
		mWorkHandler.sendEmptyMessageDelayed(WIFI_MSG_OPENING,1000);
		/*
		if(mWifiManager.isWifiEnabled()){
			mUiHandler.sendEmptyMessage(WIFI_MSG_SCANNING);
			startScan();
		}else{
			mWifiManager.setWifiEnabled(true);
			mUiHandler.sendEmptyMessage(WIFI_MSG_OPENING);
		}*/
		mStarted = true;
	}
	
	public void stop(){
		if(!mStarted){
			return;
		}
		mStarted = false;
		unregisterListener();
		mWorkHandler.sendEmptyMessage(WIFI_MSG_CLOSE);
		//mWifiManager.setWifiEnabled(false);
	}

	private void wifiEnable(boolean enable){
		if(!enable){
			mWifiManager.setWifiEnabled(false);
			return;
		}
		if(mWifiManager.isWifiEnabled()){
			mUiHandler.sendEmptyMessage(WIFI_MSG_SCANNING);
			startScan();
		}else{
			mWifiManager.setWifiEnabled(true);
			mUiHandler.sendEmptyMessage(WIFI_MSG_OPENING);
		}
	}
	
	private void registerListener(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mContext.registerReceiver(mReceiver, filter);
	}
	
	private void unregisterListener(){
		mContext.unregisterReceiver(mReceiver);
	}
	
	private void startScan(){
		if(!mScanning){
			mScanning = true;
			mWifiManager.startScan();
			/*
			try{
				Thread.sleep(1000*2);
			}catch(Exception e){
			}*/
		}
		//new Thread(){
			//public void run(){
				int retryCount = 15;
				List<ScanResult> listResult = null;
				while(retryCount > 0){
					listResult = mWifiManager.getScanResults();
					if(listResult == null || listResult.size() < 1){
						retryCount--;
						try{
							Thread.sleep(1000);
						}catch(Exception e){
						}
					}else{
						break;
					}
				}
				if(listResult == null || listResult.size() < 1){
					mUiHandler.sendEmptyMessage(WIFI_MSG_NONE_DEVICE);
				}else{
					Message msg = mUiHandler.obtainMessage(WIFI_MSG_PASS, listResult.get(0).SSID);
					mUiHandler.sendMessage(msg);
				}
				
				WifiController.this.stop();
			//}
		//}.start();
	}
}
