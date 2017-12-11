package com.ty.actionspcbatest.misc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
//import com.mediatek.factorymode.Utils;
import android.content.SharedPreferences;

import com.ty.actionspcbatest.R;

public class FMRadio extends Activity{
	private Button failbuttonButton;
	private Button successbutton;
	private SharedPreferences mSp;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.mSp = getSharedPreferences("FactoryMode", 0);
		setContentView(R.layout.signal);
		successbutton = (Button)findViewById(R.id.signal_bt_ok);
		failbuttonButton = (Button)findViewById(R.id.signal_bt_failed);
		Button button2 = successbutton;
		successbutton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Utils.SetPreferences(FMRadio.this, mSp,R.string.fmradio_name, "success");
				finish();
			}
		});
		failbuttonButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Utils.SetPreferences(FMRadio.this, mSp,R.string.fmradio_name, "failed");
				finish();
			}
		});
		Intent intent = new Intent("android.intent.action.MAIN");
		//ComponentName componentname = new ComponentName("com.mediatek.FMRadio", "com.mediatek.FMRadio.FMRadioActivity");
		ComponentName componentname = Build.VERSION.SDK_INT == /*Build.VERSION_CODES.N*/24 ? 
				new ComponentName("com.android.fmradio", "com.android.fmradio.FmMainActivity") :
					new ComponentName("com.mediatek.FMRadio", "com.mediatek.FMRadio.FMRadioActivity");
		intent.setComponent(componentname);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}
	
	
}