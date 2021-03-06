package com.ty.actionspcbatest.misc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.ty.actionspcbatest.R;
//import com.mediatek.factorymode.Utils;

public class Signal extends Activity
  implements View.OnClickListener
{
  private Button mBtFailed;
  private Button mBtOk;
  SharedPreferences mSp;

  protected void onActivityResult(int paramInt1, int paramInt2, Intent paramIntent)
  {
    AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
    localBuilder.setTitle(R.string.FMRadio_notice);
    localBuilder.setMessage(R.string.HeadSet_hook_message);
    localBuilder.setPositiveButton(R.string.Success, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int which) {
            Utils.SetPreferences(Signal.this, Signal.this.mSp, R.string.headsethook_name, "success");
        }
    });
    localBuilder.setNegativeButton(R.string.Failed, new DialogInterface.OnClickListener(){
        public void onClick(DialogInterface dialog, int which) {
            Utils.SetPreferences(Signal.this, Signal.this.mSp, R.string.headsethook_name, "failed");
        }
        
    });
    localBuilder.create().show();
  }

  public void onClick(View paramView)
  {
    SharedPreferences localSharedPreferences = this.mSp;
    if(paramView.getId() == this.mBtOk.getId()){
        Utils.SetPreferences(this, localSharedPreferences, R.string.telephone_name, "success");
        finish();
    }
    else{
        Utils.SetPreferences(this, localSharedPreferences, R.string.telephone_name, "failed");
        finish();
    }
  }

  protected void onCreate(Bundle bundle)
  {
    super.onCreate(bundle);
    setContentView(R.layout.signal);
    this.mSp = getSharedPreferences("FactoryMode", 0);
    this.mBtOk = (Button)findViewById(R.id.signal_bt_ok);
    this.mBtOk.setOnClickListener(this);
    this.mBtFailed = (Button)findViewById(R.id.signal_bt_failed);
    this.mBtFailed.setOnClickListener(this);
    Intent intent = this.getIntent();
    String number = intent.getStringExtra("number");
    if(number == null || number.length() < 1){
    	number = "112";
    }
    Uri localUri = Uri.fromParts("tel", number, null);
    Intent localIntent = new Intent("android.intent.action.CALL_PRIVILEGED", localUri);
    startActivityForResult(localIntent, 5);
  }
}