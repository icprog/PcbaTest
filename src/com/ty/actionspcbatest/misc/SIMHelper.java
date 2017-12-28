package com.ty.actionspcbatest.misc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;

//import com.android.systemui.R;

import com.mediatek.common.telephony.ITelephonyEx;
import com.mediatek.telephony.TelephonyManagerEx;
import com.mediatek.xlog.Xlog;
import com.mediatek.telephony.SimInfoManager;
import com.mediatek.common.featureoption.FeatureOption;

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;

/**
 * M: [SystemUI] Support "dual SIM" and "Notification toolbar".
 */
public class SIMHelper {

    public static final String TAG = "SIMHelper";
    
    private static TelephonyManagerEx mTMEx = null;
    private static int mGeminiSimNum = Build.VERSION.SDK_INT == /*Build.VERSION_CODES.N*/24 ? 2 : PhoneConstants.GEMINI_SIM_NUM;
    private static boolean[] simInserted;
    //7.0
    private static List<SubscriptionInfo> sSimInfos;
    private static Context mContext;
    
    public SIMHelper(Context context){
    	mContext = context;
    }

    public static boolean isSimInserted(int slotId) {
        if(simInserted == null) {
            updateSimInsertedStatus();
        }
        if (simInserted != null) {
            if(slotId <= simInserted.length -1) {
                Log.d(TAG, "isSimInserted(" + slotId + "), SimInserted=" + simInserted[slotId]);
                return simInserted[slotId];
            } else {
            	Log.d(TAG, "isSimInserted(" + slotId + "), indexOutOfBound, arraysize=" + simInserted.length);
                return false; // default return false
            }
        } else {
        	Log.d(TAG, "isSimInserted, simInserted is null");
            return false;
        }
    }

    @SuppressLint("NewApi")
	public static void updateSimInsertedStatus() {
    	if(Build.VERSION.SDK_INT == /*Build.VERSION_CODES.N*/24){
    		sSimInfos = SubscriptionManager.from(mContext).getActiveSubscriptionInfoList();
    		if(simInserted == null) {
                simInserted = new boolean[mGeminiSimNum];
            }
    		for (int i = 0 ; i < mGeminiSimNum ; i++) {
    			simInserted[i] = isSimInsertedBySlot(mContext,i);
    		}
    		return;
    	}

        ITelephonyEx mTelephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICEEX));
        if (mTelephonyEx != null) {
            try {
                if(simInserted == null) {
                    simInserted = new boolean[mGeminiSimNum];
                }
                for (int i = 0 ; i < mGeminiSimNum ; i++) {
                    simInserted[i] = mTelephonyEx.hasIccCard(i);
                    Log.d(TAG, "updateSimInsertedStatus, simInserted(" + i + ") = " + simInserted[i]);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "updateSimInsertedStatus, phone is null");
        }
    }
    
    public static final boolean isGemini() {
        return mGeminiSimNum > 1;
    	//return TelephonyManager.getDefault().getPhoneCount() > 1;
    }
    
    @SuppressLint("NewApi")
	public static SubscriptionInfo getSubInfoBySlot(Context context, int slotId) {
        if (sSimInfos == null || sSimInfos.size() == 0) {
            android.util.Log.d(TAG, "getSubInfoBySlot, SubscriptionInfo is null");
            return null;
        }

        for (SubscriptionInfo info : sSimInfos) {
            if (info.getSimSlotIndex() == slotId) {
                return info;
            }
        }
        return null;
    }

    public static boolean isSimInsertedBySlot(Context context, int slotId) {
        if (sSimInfos != null) {
            if (slotId <= mGeminiSimNum - 1) {
                SubscriptionInfo info = getSubInfoBySlot(context, slotId);
                if (info != null) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false; // default return false
            }
        } else {
            android.util.Log.d(TAG, "isSimInsertedBySlot, SubscriptionInfo is null");
            return false;
        }
    }
    
    private static ITelephony sITelephony;
    private static ITelephonyRegistry mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
    private static ITelephonyRegistry mRegistry2 = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry2"));
    
    public static ITelephony getITelephony() {
        return sITelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
    }
    
    public static void listen(PhoneStateListener listener, int events, int slotId) {
    	Log.i("hcj.SIM", "listen listener="+listener);
        try {
            Boolean notifyNow = (getITelephony() != null);
            if (PhoneConstants.GEMINI_SIM_1 == slotId) {
                mRegistry.listen("SystemUI SIMHelper", listener.getCallback(), events, notifyNow);
            } else if(PhoneConstants.GEMINI_SIM_2 == slotId) {
                mRegistry2.listen("SystemUI SIMHelper", listener.getCallback(), events, notifyNow);
            }
        } catch (Exception e) {
            Log.i("hcj.SIM", "listen e="+e);
        }
    }
}
