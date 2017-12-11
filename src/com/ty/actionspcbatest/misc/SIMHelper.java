package com.ty.actionspcbatest.misc;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

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
    private static int mGeminiSimNum = PhoneConstants.GEMINI_SIM_NUM;
    private static boolean[] simInserted;

    public static boolean isSimInserted(int slotId) {
        if(simInserted == null) {
            updateSimInsertedStatus();
        }
        if (simInserted != null) {
            if(slotId <= simInserted.length -1) {
                Xlog.d(TAG, "isSimInserted(" + slotId + "), SimInserted=" + simInserted[slotId]);
                return simInserted[slotId];
            } else {
                Xlog.d(TAG, "isSimInserted(" + slotId + "), indexOutOfBound, arraysize=" + simInserted.length);
                return false; // default return false
            }
        } else {
            Xlog.d(TAG, "isSimInserted, simInserted is null");
            return false;
        }
    }

    public static void updateSimInsertedStatus() {

        ITelephonyEx mTelephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICEEX));
        if (mTelephonyEx != null) {
            try {
                if(simInserted == null) {
                    simInserted = new boolean[mGeminiSimNum];
                }
                for (int i = 0 ; i < mGeminiSimNum ; i++) {
                    simInserted[i] = mTelephonyEx.hasIccCard(i);
                    Xlog.d(TAG, "updateSimInsertedStatus, simInserted(" + i + ") = " + simInserted[i]);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Xlog.d(TAG, "updateSimInsertedStatus, phone is null");
        }
    }
    
    public static final boolean isGemini() {
        return mGeminiSimNum > 1;
    }

}
