package com.ty.actionspcbatest.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

public class Utils
{
  public static void SetPreferences(Context paramContext, SharedPreferences paramSharedPreferences, int paramInt, String paramString)
  {
    String str = paramContext.getResources().getString(paramInt);
    SharedPreferences.Editor localEditor = paramSharedPreferences.edit();
    localEditor.putString(str, paramString);
    localEditor.commit();
  }
  
  public static int getState(SharedPreferences sharedPreferences, String key){
	  String state = sharedPreferences.getString(key, null);
	  if("success".equals(state)){
		  return 1;
	  }else if("failed".equals(state)){
		  return 2;
	  }
	  return 0;
  }
}
