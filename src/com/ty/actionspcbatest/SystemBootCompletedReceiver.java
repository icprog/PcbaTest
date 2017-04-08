package com.ty.actionspcbatest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SystemBootCompletedReceiver extends BroadcastReceiver{
	
	@Override
	public void onReceive(Context context, Intent intent){
		SwitchLogo swLogo = new SwitchLogo();
		byte logoIndex = swLogo.getLogoIndex();
		android.util.Log.i("hcj","SystemBootCompletedReceiver, index "+logoIndex);
		if(logoIndex == 0){
			startTestActivity(context);
			int result = swLogo.setLogoIndex((byte)1);
			android.util.Log.i("hcj","SystemBootCompletedReceiver, result "+result);
		}
	}
	
	private void startTestActivity(Context context){
		Intent intent = new Intent(context,PcbaTestActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

}
