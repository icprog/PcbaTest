package com.ty.actionspcbatest;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;

public class SystemBootCompletedReceiver extends BroadcastReceiver{
	private Handler mHandler = new Handler();	
	
	@Override
	public void onReceive(Context context, Intent intent){
		if(isNotAutoLaunched(context)){
			startTestActivity(context);
		}
	}
	
	private void startTestActivity(Context context){
		Intent intent = new Intent(context,PcbaTestActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		intent.putExtra("auto_launch", true);
		context.startActivity(intent);
		
		disableSelf(context);
	}
	
	private void disableSelf(Context context){
		ComponentName name = new ComponentName("com.ty.actionspcbatest", "com.ty.actionspcbatest.SystemBootCompletedReceiver");
		PackageManager pm = context.getPackageManager();
		pm.setComponentEnabledSetting(name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
			PackageManager.DONT_KILL_APP);
	}

	private boolean isNotAutoLaunched(Context context){
		if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1){
			SharedPreferences settings = context.getSharedPreferences("flag",0);
			boolean autoLaunched = settings.getBoolean("auto_launched", false);
			return !autoLaunched;
		}else{
			SwitchLogo swLogo = new SwitchLogo();
			byte logoIndex = swLogo.getLogoIndex();
			return (logoIndex == 0);
		}
	}
}
