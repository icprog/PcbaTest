package com.ty.actionspcbatest;

import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothController {
	private static final String TAG = "hcj.BluetoothController";
	private BluetoothAdapter mAdapter;
	private Handler mUiHandler;
	private Context mContext;
	private boolean mStarted;
	
	public static final int BT_MSG_OPENING = 0;
	public static final int BT_MSG_OPENED = 1;
	public static final int BT_MSG_SCANNING = 2;
	public static final int BT_MSG_NONE_DEVICE = 3;
	public static final int BT_MSG_PASS = 4;
	public static final int BT_MSG_UNSUPPORT = 5;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			Log.i(TAG, "action="+action);
			if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
				int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				boolean enabled = (state == BluetoothAdapter.STATE_ON);
				if(enabled){
					mUiHandler.sendEmptyMessage(BT_MSG_SCANNING);
					startScan();
				}
			}else if("android.bluetooth.device.action.FOUND".equals(action)){
				BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
				if (device.getBondState() != 12){					
					Message msg = mUiHandler.obtainMessage(BT_MSG_PASS,device.getName());
					mUiHandler.sendMessage(msg);
					stop();
				}
			}else if("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)){
				Message msg = mUiHandler.obtainMessage(BT_MSG_PASS,"No bt device");
				mUiHandler.sendMessage(msg);
				stop();
			}
		}
	};
	
	public BluetoothController(Context context, Handler handler){
		mContext = context;
		mUiHandler = handler;
		
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		//BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		//mAdapter = bluetoothManager.getAdapter();
	}
	
	public void start(){
		Log.i(TAG, "start");
		if(mAdapter == null){
			mUiHandler.sendEmptyMessage(BT_MSG_UNSUPPORT);
			return;
		}
		registerListener();
		if(mAdapter.isEnabled()){
			mUiHandler.sendEmptyMessage(BT_MSG_SCANNING);
			startScan();
		}else{
			mAdapter.enable();
			mUiHandler.sendEmptyMessage(BT_MSG_OPENING);
		}
		mStarted = true;
	}
	
	public void stop(){
		if(!mStarted){
			return;
		}
		mStarted = false;
		unregisterListener();
		mAdapter.disable();
	}
	
	private void registerListener(){
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction("android.bluetooth.device.action.FOUND");
		filter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
		mContext.registerReceiver(mReceiver, filter);
	}
	
	private void unregisterListener(){
		mContext.unregisterReceiver(mReceiver);
	}
	
	private void startScan(){
		if (!mAdapter.startDiscovery()){
	    	mAdapter.startDiscovery();
		}
		//stop();
	}
}
