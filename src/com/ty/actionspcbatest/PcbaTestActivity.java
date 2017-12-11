package com.ty.actionspcbatest;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ty.actionspcbatest.ShellUtils.CommandResult;
import com.ty.actionspcbatest.misc.FMRadio;
import com.ty.actionspcbatest.misc.SIMHelper;
import com.ty.actionspcbatest.misc.Signal;
import com.ty.actionspcbatest.misc.Utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
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

import com.android.internal.app.IMediaContainerService;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.MemInfoReader;
import com.mediatek.common.featureoption.*;

public class PcbaTestActivity extends Activity {
	private static final String TAG = "hcj.PcbaTestActivity";
	private LayoutInflater mLayoutInflater;
	private KeyTestPresenter mKeyTestPresenter;
	private ArrayList<MiscItemPresenter> mMiscPresenters;
	private SharedPreferences mSharedPreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mSharedPreferences = getSharedPreferences("FactoryMode", 0);
		
		setContentView(R.layout.main);
		
		Intent intent = getIntent();
		if(intent != null){
			/*
			boolean autoLaunch = intent.getBooleanExtra("auto_launch", false);
			//if(!autoLaunch){
			setAutoLaunchFlag();
			//}
			 *			 
			*/
			setAutoLaunched();
		}
		
		mLayoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mItemPresenters = new ArrayList<TestItemPresenter>();
		
		LinearLayout autoTestContainer = (LinearLayout)findViewById(R.id.auto_test_container);
		TestItem[] autoItems = Config.IS_TELEPHONY_SUPPORT ? AUTO_TEST_ITEMS_TELEPHONY : AUTO_TEST_ITEMS;
		for(int i=0;i<autoItems.length;i++){
			TestItemView child = new TestItemView(this);
			child.setTitle(autoItems[i].mTitleId);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1,-2);
			autoTestContainer.addView(child, params);
			
			TestItemPresenter presenter = getTestItemPresenter(autoItems[i].mKey);
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
		
		if(Config.IS_TELEPHONY_SUPPORT){
			mItemPresenters.add(new VibrateTestPresenter());
			
			mMiscPresenters = new ArrayList<MiscItemPresenter>();
			View miscTitleView = findViewById(R.id.misc_test_title);
			LinearLayout miscTestContainer = (LinearLayout)findViewById(R.id.misc_test_container);
			miscTitleView.setVisibility(View.VISIBLE);
			miscTestContainer.setVisibility(View.VISIBLE);
			for(int i=0;i<MISC_TEST_ITEMS.length;i++){
				MiscItemView child = new MiscItemView(this);
				child.setTitle(MISC_TEST_ITEMS[i].mTitleId);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2,-2);
				miscTestContainer.addView(child, params);
				
				TestItemPresenter presenter = getTestItemPresenter(MISC_TEST_ITEMS[i].mKey);
				presenter.setTestItemView(child);
				mItemPresenters.add(presenter);
				
				mMiscPresenters.add((MiscItemPresenter)presenter);
				child.setTestItemPresenter((MiscItemPresenter)presenter);
			}
		}
		
		int presenterSize = mItemPresenters.size();
		for(int i=0;i<presenterSize;i++){
			TestItemPresenter presenter = mItemPresenters.get(i);
			presenter.doTest();
		}
	}
	
	@Override
	public void onResume(){
		super.onResume();
		if(mMiscPresenters != null){
		int presenterSize = mMiscPresenters.size();
		for(int i=0;i<presenterSize;i++){
			MiscItemPresenter presenter = mMiscPresenters.get(i);
			presenter.onResume();
		}
		}
	}
	
	private void setAutoLaunchFlag(){
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
			SharedPreferences settings = getSharedPreferences("flag",0);
			boolean autoLaunched = settings.getBoolean("auto_launched", false);
			if(!autoLaunched){
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("auto_launched", true);
				editor.commit();
			}			
		}else{
			SwitchLogo swLogo = new SwitchLogo();
			byte logoIndex = swLogo.getLogoIndex();
			if(logoIndex == 0){
				swLogo.setLogoIndex((byte)1);
				android.util.Log.i(TAG,"disable boot auto launch");
			}
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
		private String mExtCardPath;
		private boolean mRegistered;
		private StorageManager mStorageManager;
		private Handler mHandler = new Handler();
		
		private volatile IMediaContainerService mContainerService;

	    private final ServiceConnection mContainerConnection = new ServiceConnection() {
	        @Override
	        public void onServiceConnected(ComponentName name, IBinder service) {
	            mContainerService = IMediaContainerService.Stub.asInterface(service);
	            if(isExtCardOk()){
					doRealTest();						
				}
	        }

	        @Override
	        public void onServiceDisconnected(ComponentName name) {
	            mContainerService = null;
	        }
	    };
		
		private Runnable mDelayTestRunnable = new Runnable(){
			@Override
			public void run(){
				Log.i(TAG, "mDelayTestRunnable doTest");
				if(isExtCardOk()){
					doRealTest();
				}
			}
		};
		
		private boolean isExtCardOk(){
			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN && (mContainerService == null)){
				return false;
			}
			mExtCardPath = null;		
			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
				if(Environment.MEDIA_MOUNTED.equals(mStorageManager.getVolumeState("/mnt/sdcard2"))){
					mExtCardPath = "/mnt/sdcard";
				}
			}else{
				String path = "/storage/sdcard1";
				//state = Environment.getStorageState(new File(mExtCardPath));
				String state = mStorageManager.getVolumeState(path);
				if(Environment.MEDIA_MOUNTED.equals(state)){
					mExtCardPath = path;
				}
			}			
			if(mExtCardPath != null){
				return true;
			}
			return false;
		}
		
		private BroadcastReceiver mReceiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent){
				String action = intent.getAction();
				if(Intent.ACTION_MEDIA_MOUNTED.equals(action)){
					String mountPath = intent.getData().getPath();
					Log.i(TAG, "mountPath="+mountPath);
					if(isExtCardOk()){
						doRealTest();						
					}
				}
			}
		};
		
		public void doTest(){
			showHint(getHint());
			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN){
				final Intent containerIntent = new Intent().setComponent(
						new ComponentName(
								"com.android.defcontainer", "com.android.defcontainer.DefaultContainerService"));
			 	PcbaTestActivity.this.bindService(containerIntent, mContainerConnection, Context.BIND_AUTO_CREATE);
			}
			if(mStorageManager == null){
				mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
			}
		
			if(isExtCardOk()){
				doRealTest();
			}else{
				registerListener();
			}
		}
		
		public void doStop(){
			unregisterListener();
			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN){
				PcbaTestActivity.this.unbindService(mContainerConnection);
			}
		}
		
		public void doRealTest(){		
			android.util.Log.i("hcj", "mExtCardPath="+mExtCardPath);
			long sizeGb = 0;
			if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN){
				if(mContainerService != null){
					try {
                        final long[] stats = mContainerService.getFileSystemStats(
                        		mExtCardPath);
                        sizeGb = stats[0];
                    } catch (Exception e) {
                        Log.w(TAG, "Problem in container service", e);
                    }
				}
			}if (Build.VERSION.SDK_INT == /*Build.VERSION_CODES.N*/24){
				sizeGb = new File(mExtCardPath).getTotalSpace();
			}else{
				StatFs localStatFs = new StatFs(mExtCardPath);
				long bc = localStatFs.getBlockCount();
				long bz = localStatFs.getBlockSize();
				Log.i(TAG, "mExtCardPath="+mExtCardPath+",bc="+bc+",bz="+bz);
				sizeGb = (bc*bz/1024L/1024L/1024L);
			}
			if(sizeGb > 0f){
				String size = Formatter.formatFileSize(PcbaTestActivity.this, sizeGb);
				showSuccess("PASS("+size+")");				
			}else{
				Toast.makeText(PcbaTestActivity.this, PcbaTestActivity.this.getString(R.string.sdcard_size_delay_show_hint), Toast.LENGTH_SHORT).show();;
				mHandler.postDelayed(mDelayTestRunnable, 4000);
			}
			//unregisterListener();
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
					//mVisualizerFx.testStop();
					//mVisualizerFx.testStart(new File(PcbaTestActivity.this.getCacheDir().getPath() + File.separator + "record.3gpp"));
					//mVisualizerFx.changePlayFile(Uri.fromFile(new File(PcbaTestActivity.this.getCacheDir().getPath() + File.separator + "TestRecordFile.3gpp")));
				}
			});
			
			mHandler.postDelayed(delayPlayRunnable,4000);
			mPlayerStateView.setText(R.string.waiting_music);
		}
		
		private void startRecord(){			
			mVisualizerFx.stop();
			mHandler.removeCallbacks(delayPlayRunnable);
						
			//mRecordFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "TestRecordFile.aac");
			mRecordFile = new File(PcbaTestActivity.this.getFilesDir() + File.separator + "TestRecordFile.3gpp");
			//mRecordFile = new File(PcbaTestActivity.this.getCacheDir().getPath() + File.separator + "TestRecordFile.3gpp");
			//mRecordFile = new File(Environment.getExternalStorageDirectory()+File.separator +"TestRecordFile.3gpp");
			Log.i(TAG, "startRecord mRecordFile="+mRecordFile);
			if(mRecorder == null){
				mRecorder = new Recorder();
			}
			mRecorder.stopRecording();
			mRecordFile.delete();
			
			try{
				mRecordFile.createNewFile();
				String path = mRecordFile.getAbsolutePath();
				Process p = Runtime.getRuntime().exec("chmod 777 " + path);  
				p.waitFor();  
			}catch(Exception e){
				Log.i(TAG, "createNewFile e="+e);
			}
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
		private Vibrator mVibrator;
		
		public void doTest(){
			mVibrator = ((Vibrator)getSystemService("vibrator"));
			mHandler.postDelayed(delayPlayRunnable,4000);
		}
		
		public void doStop(){
			mHandler.removeCallbacks(delayPlayRunnable);
		}
		
		private Runnable delayPlayRunnable = new Runnable(){
			@Override
			public void run(){
				mVibrator.vibrate(new long[] { 1000L, 10L, 100L, 1000L }, -1);
				Toast.makeText(PcbaTestActivity.this, PcbaTestActivity.this.getString(R.string.vibration_confirm), Toast.LENGTH_LONG).show();
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
		}else if(key == ITEM_KEY_GPS){
			return new GpsTestPresenter();
		}else if(key == ITEM_KEY_CALL){
			return new CallItemPresenter();
		}else if(key == ITEM_KEY_SIM){
			return new SimTestPresenter();
		}else if(key == ITEM_KEY_FM){
			return new FmItemPresenter();
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
		protected ItemViewInterface mTestItemView;
		public void setTestItemView(ItemViewInterface view){
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
	public static final int ITEM_KEY_GPS = ITEM_KEY_AUTO_START+7;
	public static final int ITEM_KEY_SIM = ITEM_KEY_AUTO_START+8;
	
	public static final int ITEM_KEY_MANUAL_START = 20;
	public static final int ITEM_KEY_TFCARD = ITEM_KEY_MANUAL_START;
	public static final int ITEM_KEY_USB = ITEM_KEY_MANUAL_START+1;
	public static final int ITEM_KEY_HEADSET = ITEM_KEY_MANUAL_START+2;
	public static final int ITEM_KEY_KEY = ITEM_KEY_MANUAL_START+3;	
	
	public static final int ITEM_KEY_CAMERA_PREVIEW = 40;
	
	public static final int ITEM_KEY_LCD = 60;
	
	public static final int ITEM_KEY_AUDIO = 80;
	
	public static final int ITEM_KEY_CALL = 100;
	public static final int ITEM_KEY_FM = 101;
	
	public static final int ITEM_KEY_CATEGORY_COUNT = 20;
	
	public static final TestItem ITEM_MEMORY = new TestItem(ITEM_KEY_MEMORY,R.string.item_lable_ddr);
	public static final TestItem ITEM_FLASH = new TestItem(ITEM_KEY_FLASH,R.string.item_lable_flash);
	public static final TestItem ITEM_WIFI = new TestItem(ITEM_KEY_WIFI,R.string.item_lable_wifi);
	public static final TestItem ITEM_GSENSOR = new TestItem(ITEM_KEY_GSENSOR,R.string.item_lable_gsensor);
	public static final TestItem ITEM_CAMERA = new TestItem(ITEM_KEY_CAMERA,R.string.item_lable_camera);
	public static final TestItem ITEM_BLUETOOTH = new TestItem(ITEM_KEY_BLUETOOTH,R.string.item_lable_bluetooth);
	public static final TestItem ITEM_RTC = new TestItem(ITEM_KEY_RTC,R.string.item_lable_rtc);
	public static final TestItem ITEM_GPS = new TestItem(ITEM_KEY_GPS,R.string.item_lable_gps);
	public static final TestItem ITEM_SIM = new TestItem(ITEM_KEY_SIM,R.string.item_lable_sim);
	public static final TestItem[] AUTO_TEST_ITEMS = {
		ITEM_MEMORY,ITEM_FLASH,ITEM_WIFI,ITEM_GSENSOR
		,ITEM_CAMERA,ITEM_BLUETOOTH
	};
	public static final TestItem[] AUTO_TEST_ITEMS_TELEPHONY = {
		ITEM_MEMORY,ITEM_FLASH,ITEM_WIFI,ITEM_GSENSOR
		,ITEM_CAMERA,ITEM_BLUETOOTH,ITEM_GPS,ITEM_SIM
	};
	
	public static final TestItem ITEM_TFCARD = new TestItem(ITEM_KEY_TFCARD,R.string.item_lable_card);
	public static final TestItem ITEM_USB = new TestItem(ITEM_KEY_USB,R.string.item_lable_usb);
	public static final TestItem ITEM_HEADSET = new TestItem(ITEM_KEY_HEADSET,R.string.item_lable_headset);
	public static final TestItem ITEM_KEY = new TestItem(ITEM_KEY_KEY,R.string.item_lable_key);
	public static final TestItem[] MANUAL_TEST_ITEMS = {
			ITEM_TFCARD,/*ITEM_USB,*/ITEM_HEADSET,ITEM_KEY
	};
	
	public static final TestItem ITEM_CALL = new TestItem(ITEM_KEY_CALL,R.string.item_lable_call);
	public static final TestItem ITEM_FM = new TestItem(ITEM_KEY_FM,R.string.fmradio_name);
	public static final TestItem[] MISC_TEST_ITEMS = {
			ITEM_CALL,ITEM_FM
	};
	
	public static class TestItem{
		private int mTitleId;
		private int mKey;
		public TestItem(int key, int titleId){
			mTitleId = titleId;
			mKey = key;
		}
	}
		
	public class TestItemView extends FrameLayout implements ItemViewInterface{
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

		@Override
		public void setDefault() {
			
		}
	}
	
	private static final int AP_CFG_REEB_PRODUCT_INFO_LID = 36;
	private static final int FLAG_IDX = 105;
	private static final byte FLAG_VALUE = 105;
	public static boolean isAutoLaunched(){
		try{
			IBinder binder = ServiceManager.getService("NvRAMAgent");
			NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
			byte[] buff = null;
			buff = agent .readFile(AP_CFG_REEB_PRODUCT_INFO_LID);
			if(buff == null || buff.length <= FLAG_IDX){
				//cannot get flag, skip auto launch
				return true;
			}
			Log.i(TAG,"isAutoLaunched read flag="+buff[FLAG_IDX]);
			if(buff[FLAG_IDX] != FLAG_VALUE){
				return false;
			}
			return true;
		}catch(Exception e){
			Log.i(TAG,"isAutoLaunched e="+e);
		}
		return true;//

	}
	
	public static void setAutoLaunched(){
		try{
			IBinder binder = ServiceManager.getService("NvRAMAgent");
			NvRAMAgent agent = NvRAMAgent.Stub.asInterface(binder);
			byte[] buff = null;
			buff = agent .readFile(AP_CFG_REEB_PRODUCT_INFO_LID);
			if(buff == null || buff.length <= FLAG_IDX){
				//cannot get flag, skip auto launch
				Log.i(TAG, "setAutoLaunched read fail buff="+buff);
				return;
			}
			Log.i(TAG,"isAutoLaunched read flag="+buff[FLAG_IDX]);
			if(buff[FLAG_IDX] != FLAG_VALUE){
				buff[FLAG_IDX] = FLAG_VALUE;
				int flag = agent.writeFile(AP_CFG_REEB_PRODUCT_INFO_LID,buff);
				Log.i(TAG, "setAutoLaunched save flag="+flag);
			}
		}catch(Exception e){
			Log.i(TAG,"setAutoLaunched e="+e);
		}
	}
	
	public interface ItemViewInterface{
		void setTitle(int resId);
		void setSummary(String summary);
		void setError();
		void setSuccess();
		void setDefault();
	}
	
	public class MiscItemView extends TextView implements ItemViewInterface{
		private MiscItemPresenter mTestItemPresenter;

		public MiscItemView(Context context) {
			super(context);
			this.setPadding(20, 10, 20, 10);
			this.setBackgroundResource(R.drawable.btn_default);
			this.setTextSize(28);
			this.setOnClickListener(new View.OnClickListener() {				
				@Override
				public void onClick(View arg0) {
					if(mTestItemPresenter == null){
						return;
					}
					mTestItemPresenter.doClickTest();
				}
			});
		}
		
		public void setTestItemPresenter(MiscItemPresenter presenter){
			mTestItemPresenter = presenter;
		}
		
		public void setTitle(int resId){
			this.setText(resId);
		}
		
		public void setSummary(String summary){
			this.setText(summary);
		}
		
		public void setDefault(){
			this.setTextColor(0xFF878787);
		}
		
		public void setError(){
			this.setTextColor(0xFFFF0000);
		}
		
		public void setSuccess(){
			this.setTextColor(0xFF0000FF);
		}
	}
	
	public class GpsTestPresenter extends TestItemPresenter{
		private GpsController mGpsController;
		
		private Handler mUiHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
				Log.i(TAG, "handleMessage what="+msg.what);
				switch(msg.what){
					case GpsController.GPS_MSG_UPDATE:
						updateGpsText();
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
			mGpsController = new GpsController(PcbaTestActivity.this,mUiHandler);
			mGpsController.start();
			updateGpsText();
		}
		
		public void doStop(){
			mGpsController.stop();
		}
		
		private void updateGpsText(){
			StringBuilder sb = new StringBuilder();
			if(mGpsController.isGpsConnecting()){
				sb.append(PcbaTestActivity.this.getString(R.string.gps_connecting));
				sb.append("|");
			}
			sb.append(PcbaTestActivity.this.getString(R.string.satellite_num));
			int satelliteNum = mGpsController.getSatelliteNumber();
			sb.append(satelliteNum);
			/*
			sb.append("|");			
			sb.append(PcbaTestActivity.this.getString(R.string.gps_signal));
			sb.append(mGpsController.getSatelliteSignals());
			*/
			//sb.append("|");
			if(satelliteNum > 4){
				showSuccess(sb.toString());
			}else{
				showHint(sb.toString());
			}
		}
	}
	
	public class MiscItemPresenter extends TestItemPresenter{
		public void doTest(){
			
		}
		
		public void doClickTest(){
			
		}
		
		public void onResume(){
			
		}
	}
	
	public class CallItemPresenter extends MiscItemPresenter{
		public void doClickTest(){
			Intent intent = new Intent(PcbaTestActivity.this, Signal.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			PcbaTestActivity.this.startActivity(intent);
		}	
		
		public void onResume(){
			int state = Utils.getState(mSharedPreferences, PcbaTestActivity.this.getString(R.string.telephone_name));
			if(state == 1){
				mTestItemView.setSuccess();
			}else if(state == 2){
				mTestItemView.setError();
			}else{
				mTestItemView.setDefault();
			}
		}
	}	
	
	public class FmItemPresenter extends MiscItemPresenter{
		public void doClickTest(){
			Intent intent = new Intent(PcbaTestActivity.this, FMRadio.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			PcbaTestActivity.this.startActivity(intent);
		}	
		
		public void onResume(){
			int state = Utils.getState(mSharedPreferences, PcbaTestActivity.this.getString(R.string.fmradio_name));
			if(state == 1){
				mTestItemView.setSuccess();
			}else if(state == 2){
				mTestItemView.setError();
			}else{
				mTestItemView.setDefault();
			}
		}
	}	
	
	public class SimTestPresenter extends TestItemPresenter{
		public void doTest(){
			SIMHelper simHelper = new SIMHelper(PcbaTestActivity.this);
			boolean sim1Exist = simHelper.isSimInserted(0);
			boolean sim2Exist = true;
			StringBuilder sb = new StringBuilder();
			/*
			TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
			String sim1State = telephonyManager.getNetworkOperatorGemini(0);
			if(sim1State != null && sim1State.length() > 0){
				sim1Exist = true;
			}*/
			sb.append(sim1Exist ? "SIM1:ÒÑ¼ì²â" : "SIM1:Î´¼ì²â");
			if(simHelper.isGemini()){
				/*
				String sim2State = telephonyManager.getNetworkOperatorGemini(1);
				if(sim2State == null || sim2State.length() == 0){
					sim2Exist = false;
				}*/
				sim2Exist = simHelper.isSimInserted(1);
				sb.append("|");
				sb.append(sim2Exist ? "SIM2:ÒÑ¼ì²â" : "SIM2:Î´¼ì²â");
			}
			String result = sb.toString();
			if(sim1Exist && sim2Exist){
				showSuccess(result);
			}else{
				showFail(result);
			}
		}
	}
}
