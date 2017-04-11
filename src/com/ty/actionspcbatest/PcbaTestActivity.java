package com.ty.actionspcbatest;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ty.actionspcbatest.ShellUtils.CommandResult;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.MemInfoReader;

public class PcbaTestActivity extends Activity {
	private static final String TAG = "hcj.PcbaTestActivity";
	private LayoutInflater mLayoutInflater;
	private KeyTestPresenter mKeyTestPresenter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mLayoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mItemPresenters = new ArrayList<TestItemPresenter>();
		
		LinearLayout autoTestContainer = (LinearLayout)findViewById(R.id.auto_test_container);
		for(int i=0;i<AUTO_TEST_ITEMS.length;i++){
			TestItemView child = new TestItemView(this);
			child.setTitle(AUTO_TEST_ITEMS[i].mTitleId);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2);
			autoTestContainer.addView(child, params);
			
			TestItemPresenter presenter = getTestItemPresenter(AUTO_TEST_ITEMS[i].mKey);
			presenter.setTestItemView(child);
			mItemPresenters.add(presenter);
		}
		LinearLayout manualTestContainer = (LinearLayout)findViewById(R.id.manual_test_container);
		for(int i=0;i<MANUAL_TEST_ITEMS.length;i++){
			TestItemView child = new TestItemView(this);
			child.setTitle(MANUAL_TEST_ITEMS[i].mTitleId);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2);
			manualTestContainer.addView(child, params);
			
			TestItemPresenter presenter = getTestItemPresenter(MANUAL_TEST_ITEMS[i].mKey);
			presenter.setTestItemView(child);
			mItemPresenters.add(presenter);
		}
		
		mItemPresenters.add(new MediaTestPresenter());
		mItemPresenters.add(new TouchTestPresenter());
		//mItemPresenters.add(new VibrateTestPresenter());
		
		int presenterSize = mItemPresenters.size();
		for(int i=0;i<presenterSize;i++){
			TestItemPresenter presenter = mItemPresenters.get(i);
			presenter.doTest();
		}
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		int presenterSize = mItemPresenters.size();
		for(int i=0;i<presenterSize;i++){
			TestItemPresenter presenter = mItemPresenters.get(i);
			presenter.doStop();
		}
	}
	
	@Override
	public boolean onKeyDown(int keycode, KeyEvent keyevent){
		mKeyTestPresenter.onKeyDown(keycode, keyevent);
		return super.onKeyDown(keycode, keyevent);
	}
	
	@Override
	public boolean onKeyUp(int keycode, KeyEvent keyevent){
		mKeyTestPresenter.onKeyUp(keycode, keyevent);
		return super.onKeyUp(keycode, keyevent);
	}
		
	public class WifiTestPresenter extends TestItemPresenter{
		private WifiController mWifiController;
		
		private Handler mUiHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				Log.i(TAG, "handleMessage what="+msg.what);
				switch(msg.what){
					case WifiController.WIFI_MSG_OPENING:
						showHint(PcbaTestActivity.this.getString(R.string.wifi_is_openning));
						break;
					case WifiController.WIFI_MSG_SCANNING:
						showHint(PcbaTestActivity.this.getString(R.string.wifi_is_searching));
						break;
					case WifiController.WIFI_MSG_NONE_DEVICE:
						showFail("FAIL(Unable to find device)");						
						break;
					case WifiController.WIFI_MSG_PASS:
						showSuccess("PASS(Search to "+msg.obj+")");
						break;
					default:
						break;
				}
			}
		};
		
		public String getResult(){
			return null;
		}
		
		public void doTest(){
			mWifiController = new WifiController(PcbaTestActivity.this,mUiHandler);
			mWifiController.start();
		}
		
		public void doStop(){
			mWifiController.stop();
		}
	}
	
	public class BlutoothTestPresenter extends TestItemPresenter{
		private BluetoothController mBluetoothController;
		
		private Handler mUiHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				Log.i(TAG, "handleMessage what="+msg.what);
				switch(msg.what){
					case BluetoothController.BT_MSG_OPENING:
						showHint(PcbaTestActivity.this.getString(R.string.bluetooth_is_openning));
						break;
					case BluetoothController.BT_MSG_SCANNING:
						showHint(PcbaTestActivity.this.getString(R.string.bluetooth_is_searching));
						break;
					case BluetoothController.BT_MSG_NONE_DEVICE:
						showFail("FAIL(Unable to find device)");						
						break;
					case BluetoothController.BT_MSG_PASS:
						showSuccess("PASS(Search to "+msg.obj+")");
						break;
					case BluetoothController.BT_MSG_UNSUPPORT:
						showFail("FAIL(Unsupport)");
						break;
					default:
						break;
				}
			}
		};
		
		public String getResult(){
			return null;
		}
		
		public void doTest(){
			mBluetoothController = new BluetoothController(PcbaTestActivity.this,mUiHandler);
			mBluetoothController.start();
		}
		
		public void doStop(){
			mBluetoothController.stop();
		}
	}
	
	public class GsensorTestPresenter extends TestItemPresenter{
		private SensorManager mSensorManager;
		private Sensor mSensor;
		private SensorEventListener mListener;
		
		public void doTest(){
			mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
			mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			mListener = new SensorEventListener(){
				public void onAccuracyChanged(Sensor sensor, int accuracy) {					
				}

				public void onSensorChanged(SensorEvent event) {
					// TODO Auto-generated method stub
					float x = event.values[SensorManager.DATA_X];
					float y = event.values[SensorManager.DATA_Y];
					float z = event.values[SensorManager.DATA_Z];
					showSuccess("PASS("+x+","+y+","+z+")");
					//showResult("PASS("+")");
				}
		    };
		    mSensorManager.registerListener(mListener, mSensor, SensorManager.SENSOR_DELAY_GAME);
		}
		
		public void doStop(){
			mSensorManager.unregisterListener(mListener);
		}
	}
	
	public class MemoryTestPresenter extends TestItemPresenter{
		public void doTest(){			
			MemInfoReader mMemInfoReader = new MemInfoReader();
			mMemInfoReader.readMemInfo();
			long totalSize = mMemInfoReader.getTotalSize();
			float sizeMb = totalSize/1024f/1024f;
			int resultSizeMb = 0;
			if(sizeMb <= 256f){
				resultSizeMb = 256;
			}else if(sizeMb <= 512f){
				resultSizeMb = 512;
			}else if(sizeMb <= 1024f){
				resultSizeMb = 1024;
			}else if(sizeMb <= 2048f){
				resultSizeMb = 2048;
			}
			String result = "PASS("+resultSizeMb+"MB)";
			showSuccess(result);
		}
	}
	
	public class FlashTestPresenter extends TestItemPresenter{
		public void doTest(){
			ShellUtils.CommandResult result = ShellUtils.execCommand("cat /proc/partitions", false);
			if(result.result == -1 || result.successMsg == null){
				showFail("FAIL");
				return;
			}
			String[] resultLines = result.successMsg.split("\n");
			Pattern p = Pattern.compile("\\s*\\d+\\s+\\d+\\s+(\\d+)\\s+(\\w+)");
			int totalBlocks = 0;
			for(String line : resultLines){
				Matcher m = p.matcher(line);
				if(m.find() && m.groupCount() == 2){
					//mmcblk1 is external sdcard partition
					if(m.group(2).startsWith("mmcblk1")){
						continue;
					}
					totalBlocks += Integer.parseInt(m.group(1));
				}
			}
			
			long totalSize = totalBlocks*512L;
			float sizeGb = Math.round(totalSize/1024F/1024F/1024F);
			String resultStr = "PASS("+sizeGb+"GB)";
			showSuccess(resultStr);
		}
	}
	
	public class TFCardTestPresenter extends ManualItemPresenter{
		public static final String TFCARD_PATH = "/storage/sdcard1";
		private boolean mRegistered;
		
		private BroadcastReceiver mReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent){
				String action = intent.getAction();
				if(Intent.ACTION_MEDIA_MOUNTED.equals(action)){
					String mountPath = intent.getData().getPath();
					Log.i(TAG, "mountPath="+mountPath);
					if(TFCardTestPresenter.TFCARD_PATH.equals(mountPath)){
						doRealTest();
					}
				}
			}
		};
		
		public void doTest(){
			showHint(getHint());
			String state = Environment.getStorageState(new File(TFCARD_PATH));
			if(Environment.MEDIA_MOUNTED.equals(state)){
				doRealTest();
			}else{
				registerListener();
			}
		}
		
		public void doStop(){
			unregisterListener();
		}
		
		public void doRealTest(){			
			StatFs localStatFs = new StatFs(TFCARD_PATH);
			long bc = localStatFs.getBlockCount();
			long bz = localStatFs.getBlockSize();
			float sizeGb = (float)(bc*bz/1024L/1024L/1024L);
			showSuccess("PASS("+sizeGb+"GB)");
			unregisterListener();
		}
		
		private void registerListener(){
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
			filter.addDataScheme("file");
			registerReceiver(mReceiver, filter);
			mRegistered = true;
		}
		
		private void unregisterListener(){
			if(!mRegistered){
				return;
			}
			mRegistered = false;
			unregisterReceiver(mReceiver);
		}
	}
	
	public class KeyTestPresenter extends ManualItemPresenter{
		private boolean mVolUpOk;
		private boolean mVolDnOk;
		
		public String getHint(){
			return PcbaTestActivity.this.getString(R.string.key_notify);
		}
		
		public void onKeyDown(int keycode, KeyEvent keyevent){
			String volUpStatus = mVolUpOk ? "ok" : "wait";
			String volDnStatus = mVolDnOk ? "ok" : "wait";
			if(keycode == KeyEvent.KEYCODE_VOLUME_DOWN){
				volDnStatus = "down";
			}else if(keycode == KeyEvent.KEYCODE_VOLUME_UP){
				volUpStatus = "down";
			}
			String result = "Vol-: "+volDnStatus+", Vol+: "+volUpStatus;
			if(mVolUpOk && mVolDnOk){
				showSuccess(result);
			}else{
				showHint(result);
			}
		}
		
		public void onKeyUp(int keycode, KeyEvent keyevent){			
			if(keycode == KeyEvent.KEYCODE_VOLUME_DOWN){				
				mVolDnOk = true;
			}else if(keycode == KeyEvent.KEYCODE_VOLUME_UP){
				mVolUpOk = true;
			}
			String volUpStatus = mVolUpOk ? "ok" : "wait";
			String volDnStatus = mVolDnOk ? "ok" : "wait";
			String result = "Vol-: "+volDnStatus+", Vol+: "+volUpStatus;
			if(mVolUpOk && mVolDnOk){
				showSuccess(result);
			}else{
				showHint(result);
			}
		}
	}
	
	public class HeadsetTestPresenter extends ManualItemPresenter{
		private AudioManager mAudioManager;
		private boolean mSuccess;
		private boolean mRegistered;
		
		private BroadcastReceiver mReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent){
				String action = intent.getAction();
				if("android.intent.action.HEADSET_PLUG".equals(action)){
					onHeadsetPlugStateChanged();
				}
			}
		};
		
		public void doTest(){
			showHint(getHint());
			
			mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			if(!onHeadsetPlugStateChanged()){
				registerListener();
			}
		}
		
		public void doStop(){
			unregisterListener();
		}
		
		private void registerListener(){
			IntentFilter filter = new IntentFilter();
			filter.addAction("android.intent.action.HEADSET_PLUG");
			registerReceiver(mReceiver, filter);
			mRegistered = true;
		}
		
		private void unregisterListener(){
			if(!mRegistered){
				return;
			}
			mRegistered = false;
			unregisterReceiver(mReceiver);
		}
		
		public boolean onHeadsetPlugStateChanged(){
			boolean pluged = mAudioManager.isWiredHeadsetOn();
			if(!mSuccess && !pluged){
				return pluged;
			}
			if(pluged){
				mSuccess = true;
			}
			String status = pluged ? "Connected" : "Plug out";
			showSuccess("PASS("+status+")");
			return pluged;
		}
	}
	
	public class CameraTestPresenter extends TestItemPresenter{
		private Camera mCamera;
		private Camera.Parameters mCameraParam;
		private SurfaceHolder mHolder;
		private int mCurrentIdx;
		private static final int SWITCH_CAMERA_PERIOD = 4*1000;
		
		public void doTest(){
			showHint(getHint());
			initPreview();
		}
		
		public void doStop(){
			mHandler.removeCallbacks(switchCameraRunnable);
			closeCamera();
		}
		
		private void openCamera(){
			if (mCamera != null) {
				return;
			}
			try{
		          mCamera = Camera.open(mCurrentIdx);
		          mCameraParam = mCamera.getParameters();
		          mCameraParam.setFlashMode(Parameters.FLASH_MODE_TORCH);
		          mCamera.setParameters(mCameraParam);
			}catch(Exception e){
				Log.i(TAG, "openCamera e="+e);
				showFail("FAIL(fail to open camera)");
			}
		}
		
		private void closeCamera() {
			if (null != mCamera) {
				mCamera.stopPreview();
				mCamera.release();
				mCamera = null;
			}
		}
		
		private void startPreview() {
			if(mCamera == null){
				return;
			}
			mCamera.startPreview();
			showSuccess("PASS");
			
			int num = Camera.getNumberOfCameras();
			if(num > 1){
				//mCurrentIdx = 0;
				mHandler.postDelayed(switchCameraRunnable, SWITCH_CAMERA_PERIOD);
			}
		}
		
		private void initPreview(){
			SurfaceView surfaceView = (SurfaceView)findViewById(R.id.camera_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.addCallback(mCallBack);
			mCurrentIdx = 0;
		}
		
		private SurfaceHolder.Callback mCallBack = new SurfaceHolder.Callback(){
			@Override
			public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
				startPreview();
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder surfaceHolder) {
				mHolder = surfaceHolder;
				openCamera();
				if(mCamera != null){
					try{
						mCamera.setPreviewDisplay(surfaceHolder);
					}catch(Exception e){
						Log.i(TAG, "setPreviewDisplay e="+e);
						showFail("FAIL(fail to show preview)");
						closeCamera();
					}
				}				
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder arg0) {
			}			
		};
		
		private Runnable switchCameraRunnable = new Runnable(){
			@Override
			public void run(){
				mCurrentIdx++;
				int num = Camera.getNumberOfCameras();
				if(mCurrentIdx >= num){
					mCurrentIdx = 0;
				}
				//close
				closeCamera();
				//open
				openCamera();
				try{
					mCamera.setPreviewDisplay(mHolder);
				}catch(Exception e){
					Log.i(TAG, "setPreviewDisplay e="+e);
					showFail("FAIL(fail to show preview)");
					closeCamera();
				}
				mCamera.startPreview();
				
				if(num > 1){
					mHandler.postDelayed(switchCameraRunnable, SWITCH_CAMERA_PERIOD);
				}
			}
		};
	}
	
	public class MediaTestPresenter extends TestItemPresenter{
		private VUMeter mVUMeter;
		private Recorder mRecorder;
		private File mRecordFile;
		private View mRecordBtn;
		private VisualizerFx mVisualizerFx;
		private TextView mPlayerStateView;
		private static final int RECORD_TIME = 5*1000;
		
		public void doTest(){
			init();
		}
		
		public void doStop(){
			mVisualizerFx.release();
			if(mRecorder != null){
				mRecorder.stopRecording();
			}
			if (mRecordFile != null) {
		        mRecordFile.delete();
		    }
		}
		
		private void init(){
			mPlayerStateView = (TextView)findViewById(R.id.recorder_player_states);
			mVUMeter = (VUMeter)findViewById(R.id.vumeter);
			mVisualizerFx = (VisualizerFx)findViewById(R.id.visualizer_fx);
			
			mRecordBtn = findViewById(R.id.start_or_stop_record);
			mRecordBtn.setOnClickListener(new View.OnClickListener() {				
				@Override
				public void onClick(View arg0) {
					startRecord();
					//mVisualizerFx.toggle();
				}
			});
			
			mHandler.postDelayed(delayPlayRunnable,4000);
			mPlayerStateView.setText(R.string.waiting_music);
		}
		
		private void startRecord(){			
			mVisualizerFx.stop();
			mHandler.removeCallbacks(delayPlayRunnable);
			
			mRecordFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "TestRecordFile.aac");
			if(mRecorder == null){
				mRecorder = new Recorder();
			}
			mRecorder.stopRecording();
			mRecordFile.delete();
			mVUMeter.setRecorder(this.mRecorder);
			mRecorder.setOutputFile(mRecordFile.getAbsolutePath());
			mRecorder.startRecording(0);
			mVUMeter.invalidate();
			
			mHandler.postDelayed(stopRecordRunnable,RECORD_TIME);
			
			mRecordBtn.setEnabled(false);
			mVUMeter.setVisibility(View.VISIBLE);
			mVisualizerFx.setVisibility(View.GONE);
			mPlayerStateView.setVisibility(View.GONE);
		}
		
		private void stopRecord(){
			if(mRecorder == null){
				return;
			}
			mRecorder.stopRecording();
			mRecorder = null;
			
			mVUMeter.setVisibility(View.GONE);
			mVisualizerFx.setVisibility(View.VISIBLE);
			mPlayerStateView.setVisibility(View.VISIBLE);
			mRecordBtn.setEnabled(true);
		}
		
		private Runnable stopRecordRunnable = new Runnable(){
			@Override
			public void run(){
				stopRecord();
				mVisualizerFx.changePlayFile(Uri.fromFile(mRecordFile));
			}
		};
		
		private Runnable delayPlayRunnable = new Runnable(){
			@Override
			public void run(){
				mVisualizerFx.start();
				mPlayerStateView.setText(R.string.playing_music);
			}
		};
		
		public void showSuccess(String result){
		}
		
		public void showHint(String hint){
		}
		
		public void showFail(String result){
		}
	}
	
	private class TouchTestPresenter extends TestItemPresenter{
		public void doTest(){
			Settings.System.putInt(PcbaTestActivity.this.getContentResolver(),Settings.System.POINTER_LOCATION,1);
			Settings.System.putInt(PcbaTestActivity.this.getContentResolver(),Settings.System.SHOW_TOUCHES,1);
		}
		
		public void doStop(){
			Settings.System.putInt(PcbaTestActivity.this.getContentResolver(),Settings.System.POINTER_LOCATION,0);
			Settings.System.putInt(PcbaTestActivity.this.getContentResolver(),Settings.System.SHOW_TOUCHES,0);
		}
	}
	
	private class VibrateTestPresenter extends TestItemPresenter{
		private Vibrator vibrator;
		
		public void doTest(){
			mHandler.postDelayed(delayPlayRunnable,4000);
		}
		
		public void doStop(){
			mHandler.removeCallbacks(delayPlayRunnable);
		}
		
		private Runnable delayPlayRunnable = new Runnable(){
			@Override
			public void run(){
				vibrator.vibrate(new long[] { 1000L, 10L, 100L, 1000L }, -1);
				Toast.makeText(PcbaTestActivity.this, PcbaTestActivity.this.getString(R.string.vibration_confirm), 0).show();
			}
		};
	}
	
	private TestItemPresenter getTestItemPresenter(int key){
		if(key == ITEM_KEY_MEMORY){
			return new MemoryTestPresenter();
		}else if(key == ITEM_KEY_FLASH){
			return new FlashTestPresenter();
		}else if(key == ITEM_KEY_WIFI){
			return new WifiTestPresenter();
		}else if(key == ITEM_KEY_BLUETOOTH){
			return new BlutoothTestPresenter();
		}else if(key == ITEM_KEY_GSENSOR){
			return new GsensorTestPresenter();
		}else if(key == ITEM_KEY_TFCARD){
			return new TFCardTestPresenter();
		}else if(key == ITEM_KEY_KEY){
			mKeyTestPresenter = new KeyTestPresenter();
			return mKeyTestPresenter;
		}else if(key == ITEM_KEY_HEADSET){
			return new HeadsetTestPresenter();
		}else if(key == ITEM_KEY_CAMERA){
			return new CameraTestPresenter();
		}else if(key >= ITEM_KEY_MANUAL_START && key < (ITEM_KEY_MANUAL_START+ITEM_KEY_CATEGORY_COUNT)){
			return new ManualItemPresenter();
		}
		return new TestItemPresenter();
	}
	
	private ArrayList<TestItemPresenter> mItemPresenters;
	
	public class ManualItemPresenter extends TestItemPresenter{
		public String getHint(){
			return PcbaTestActivity.this.getString(R.string.is_waiting_plugin);
		}
	}
	
	public class TestItemPresenter{
		protected Handler mHandler = new Handler();
		private TestItemView mTestItemView;
		public void setTestItemView(TestItemView view){
			mTestItemView = view;
		}
		
		public String getResult(){
			return PcbaTestActivity.this.getString(R.string.testing);
		}
		
		public String getHint(){
			return PcbaTestActivity.this.getString(R.string.testing);
		}
		
		public void doTest(){
			showHint(getHint());
		}
		
		public void showSuccess(String result){
			mTestItemView.setSummary(result);
			mTestItemView.setSuccess();
		}
		
		public void showHint(String hint){
			mTestItemView.setSummary(hint);
		}
		
		public void showFail(String result){
			mTestItemView.setSummary(result);
			mTestItemView.setError();
		}
		
		public void doStop(){
			
		}
	}
	
	public static final int ITEM_KEY_AUTO_START = 0;
	public static final int ITEM_KEY_MEMORY = ITEM_KEY_AUTO_START;
	public static final int ITEM_KEY_FLASH = ITEM_KEY_AUTO_START+1;
	public static final int ITEM_KEY_WIFI = ITEM_KEY_AUTO_START+2;
	public static final int ITEM_KEY_GSENSOR = ITEM_KEY_AUTO_START+3;
	public static final int ITEM_KEY_CAMERA = ITEM_KEY_AUTO_START+4;
	public static final int ITEM_KEY_BLUETOOTH = ITEM_KEY_AUTO_START+5;
	public static final int ITEM_KEY_RTC = ITEM_KEY_AUTO_START+6;
	
	public static final int ITEM_KEY_MANUAL_START = 20;
	public static final int ITEM_KEY_TFCARD = ITEM_KEY_MANUAL_START;
	public static final int ITEM_KEY_USB = ITEM_KEY_MANUAL_START+1;
	public static final int ITEM_KEY_HEADSET = ITEM_KEY_MANUAL_START+2;
	public static final int ITEM_KEY_KEY = ITEM_KEY_MANUAL_START+3;
	
	public static final int ITEM_KEY_CAMERA_PREVIEW = 40;
	
	public static final int ITEM_KEY_LCD = 60;
	
	public static final int ITEM_KEY_AUDIO = 80;
	
	public static final int ITEM_KEY_CATEGORY_COUNT = 20;
	
	public static final TestItem ITEM_MEMORY = new TestItem(ITEM_KEY_MEMORY,R.string.item_lable_ddr);
	public static final TestItem ITEM_FLASH = new TestItem(ITEM_KEY_FLASH,R.string.item_lable_flash);
	public static final TestItem ITEM_WIFI = new TestItem(ITEM_KEY_WIFI,R.string.item_lable_wifi);
	public static final TestItem ITEM_GSENSOR = new TestItem(ITEM_KEY_GSENSOR,R.string.item_lable_gsensor);
	public static final TestItem ITEM_CAMERA = new TestItem(ITEM_KEY_CAMERA,R.string.item_lable_camera);
	public static final TestItem ITEM_BLUETOOTH = new TestItem(ITEM_KEY_BLUETOOTH,R.string.item_lable_bluetooth);
	public static final TestItem ITEM_RTC = new TestItem(ITEM_KEY_RTC,R.string.item_lable_rtc);
	public static final TestItem[] AUTO_TEST_ITEMS = {
		ITEM_MEMORY,ITEM_FLASH,ITEM_WIFI,ITEM_GSENSOR
		,ITEM_CAMERA,ITEM_BLUETOOTH
	};
	
	public static final TestItem ITEM_TFCARD = new TestItem(ITEM_KEY_TFCARD,R.string.item_lable_card);
	public static final TestItem ITEM_USB = new TestItem(ITEM_KEY_USB,R.string.item_lable_usb);
	public static final TestItem ITEM_HEADSET = new TestItem(ITEM_KEY_HEADSET,R.string.item_lable_headset);
	public static final TestItem ITEM_KEY = new TestItem(ITEM_KEY_KEY,R.string.item_lable_key);
	public static final TestItem[] MANUAL_TEST_ITEMS = {
			ITEM_TFCARD,/*ITEM_USB,*/ITEM_HEADSET,ITEM_KEY
	};
	
	public static class TestItem{
		private int mTitleId;
		private int mKey;
		public TestItem(int key, int titleId){
			mTitleId = titleId;
			mKey = key;
		}
	}
		
	public class TestItemView extends FrameLayout{
		private int mTitle;
		private TextView mSummaryView;
		private TextView mTitleView;

		public TestItemView(Context context) {
			super(context);
			mLayoutInflater.inflate(R.layout.test_item_view, this, true);
			mTitleView = (TextView)findViewById(R.id.item_title);
			mSummaryView = (TextView)findViewById(R.id.item_summary);
		}
		
		public void setTitle(int resId){
			mTitleView.setText(resId);
		}
		
		public void setSummary(String summary){
			mSummaryView.setText(summary);
		}
		
		public void setError(){
			mSummaryView.setTextColor(0xFFFF0000);
		}
		
		public void setSuccess(){
			mSummaryView.setTextColor(0xFF0000FF);
		}
	}
}
