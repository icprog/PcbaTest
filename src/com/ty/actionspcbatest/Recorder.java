package com.ty.actionspcbatest;

import android.media.MediaRecorder;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.util.Log;

import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Recorder implements MediaRecorder.OnErrorListener {
    public static final int STATE_IDLE = 1;
    public static final int STATE_RECORDING = 2;
    public static final int STATE_PAUSE_RECORDING = 3;
    public static final int STATE_PLAYING = 4;
    public static final int STATE_PAUSE_PLAYING = 5;
    public static final int STATE_ERROR = 6;
    public static final int STATE_ERROR_CODE = 100;
    public static final int STATE_SAVE_SUCESS = 7;
    
    public static final int ERROR_SD_UNMOUNTED_ON_FILE_LIST = 0;
    // receive EJECT/UNMOUNTED broadcast when record
    public static final int ERROR_SD_UNMOUNTED_ON_RECORD = 1;
    // when record, storage is becoming full
    public static final int ERROR_STORAGE_FULL_WHEN_RECORD = 2;
    // when launch SoundRecorder, storage is already full
    public static final int ERROR_STORAGE_FULL_WHEN_LAUNCH = 3;
    // when launch SoundRecorder or click record button, no SD card mounted
    public static final int ERROR_NO_SD = 4;
    // when record, MediaRecorder is occupied by other application
    public static final int ERROR_RECORDER_OCCUPIED = 5;
    // when record, catch other exceptions
    public static final int ERROR_RECORDING_FAILED = 6;
    // when play, can not get audio focus
    public static final int ERROR_PLAYER_OCCUPIED = 7;
    // when play, catch other exceptions
    public static final int ERROR_PLAYING_FAILED = 8;
    // when delete recording file, failed to access data base
    public static final int ERROR_FILE_DELETED_WHEN_PLAY = 9;
    // when create recording file, catch other exceptions
    public static final int ERROR_CREATE_FILE_FAILED = 10;
    // when save file, fail to access data base
    public static final int ERROR_SAVE_FILE_FAILED = 11;
    // when delete file, fail to access data base
    public static final int ERROR_DELETING_FAILED = 12;
    // when list recording files, fail to access data base
    public static final int ERROR_ACCESSING_DB_FAILED_WHEN_QUERY = 13;
    // receive EJECT/UNMOUNTED broadcast when idle/play in SoundRecorder
    public static final int ERROR_SD_UNMOUNTED_WHEN_IDLE = 14; 


    public static final String RECORD_FOLDER = "Recording";
    public static final String SAMPLE_SUFFIX = ".tmp";

    private static final String TAG = "SR/Recorder";
    private static final String SAMPLE_PREFIX = "record";

    // M: the three below are all in millseconds
    private long mSampleLength = 0;
    private long mSampleStart = 0;
    private long mPreviousTime = 0;

    private File mSampleFile = null;
    //private final StorageManager mStorageManager;
    private MediaRecorder mRecorder = null;
    private RecorderListener mListener = null;
    private int mCurrentState = STATE_IDLE;

    // M: used for audio pre-process
    private boolean[] mSelectEffect = null;

    // M: the listener when error occurs and state changes
    public interface RecorderListener {
        // M: when state changes, we will notify listener the new state code
        void onStateChanged(Recorder recorder, int stateCode);

        // M: when error occurs, we will notify listener the error code
        void onError(Recorder recorder, int errorCode);
    }
    
    public void setOutputFile(String filePath){    
      mSampleFile = new File(filePath);
    }

    @Override
    /**
     * M: the error callback of MediaRecorder
     */
    public void onError(MediaRecorder recorder, int errorType, int extraCode) {
        stopRecording();
        setError(ERROR_RECORDING_FAILED);
    }

    /**
     * M: get the current amplitude of MediaRecorder, used by VUMeter
     * @return the amplitude value
     */
    public int getMaxAmplitude() {
        synchronized (this) {
            if (null == mRecorder) {
                return 0;
            }
            return (STATE_RECORDING != mCurrentState) ? 0 : mRecorder
                    .getMaxAmplitude();
        }
    }

    /**
     * M: get the file path of current sample file
     * @return
     */
    public String getSampleFilePath() {
        return (null == mSampleFile) ? null : mSampleFile.getAbsolutePath();
    }

    public long getSampleLength() {
        return mSampleLength;
    }

    public File getSampFile() {
        return mSampleFile;
    }

    /**
     * M: get how long time we has recorded
     * @return the record length, in millseconds
     */
    public long getCurrentProgress() {
        if (STATE_RECORDING == mCurrentState) {
            long current = SystemClock.elapsedRealtime();
            return (long) (current - mSampleStart + mPreviousTime);
        } else if (STATE_PAUSE_RECORDING == mCurrentState) {
            return (long) (mPreviousTime);
        }
        return 0;
    }

    /**
     * M: set Recorder to initial state
     */
    public boolean reset() {
        /** M:modified for stop recording failed. @{ */
        boolean result = true;
        synchronized (this) {
            if (null != mRecorder) {
                try {
                    /**M: To avoid NE while mCurrentState is not prepared.@{**/
                    if (mCurrentState == STATE_PAUSE_RECORDING
                            || mCurrentState == STATE_RECORDING) {
                        mRecorder.stop();
                    }
                    /**@}**/
                } catch (RuntimeException exception) {
                    exception.printStackTrace();
                    result = false;
                } finally {
                    mRecorder.reset();
                    mRecorder.release();
                    mRecorder = null;
                }
            }
        }
        //mSampleFile = null;
        mPreviousTime = 0;
        mSampleLength = 0;
        mSampleStart = 0;
        /**
         * M: add for some error case for example pause or goon recording
         * failed. @{
         */
        mCurrentState = STATE_IDLE;
        /** @} */
        return result;
    }

    public boolean startRecording(int fileSizeLimit) {
        if (STATE_IDLE != mCurrentState) {
            return false;
        }
        reset();

        if (!initAndStartMediaRecorder(fileSizeLimit)) {
            return false;
        }
        mSampleStart = SystemClock.elapsedRealtime();
        setState(STATE_RECORDING);
        return true;
    }
    
    public boolean stopRecording() {
        //LogUtils.i(TAG, "<stopRecording> start");
        if (((STATE_PAUSE_RECORDING != mCurrentState) && 
             (STATE_RECORDING != mCurrentState)) || (null == mRecorder)) {
            //LogUtils.i(TAG, "<stopRecording> end 1");
            setError(STATE_ERROR_CODE);
            return false;
        }
        boolean isAdd = (STATE_RECORDING == mCurrentState) ? true : false;
        synchronized (this) {
            try {
                if (mCurrentState != STATE_IDLE) {
                    mRecorder.stop();
                }
                setState(STATE_IDLE);
            } catch (RuntimeException exception) {
                /** M:modified for stop recording failed. @{ */
                handleException(false, exception);
                setError(ERROR_RECORDING_FAILED);
                //LogUtils.e(TAG, "<stopRecording> recorder illegalstate exception in recorder.stop()");
            } finally {
                if (null != mRecorder) {
                    mRecorder.reset();
                    mRecorder.release();
                    mRecorder = null;
                }
                if (isAdd) {
                    mPreviousTime += SystemClock.elapsedRealtime() - mSampleStart;
                }
                mSampleLength = mPreviousTime;
                setState(STATE_IDLE);
            }
            /** @} */
        }        
        return true;
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    private void setState(int state) {
        mCurrentState = state;
        if(mListener != null){
        	mListener.onStateChanged(this, state);
        }
    }

    private boolean initAndStartMediaRecorder(int fileSizeLimit) {
    	if(true){
    		mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(1);
            mRecorder.setOutputFile(mSampleFile.getAbsolutePath());
            /**@}*/
            mRecorder.setAudioEncoder(3);
            mRecorder.setAudioChannels(2);
            mRecorder.setAudioEncodingBitRate(48000);
            mRecorder.setAudioSamplingRate(32000);
            if (fileSizeLimit > 0) {
                mRecorder.setMaxFileSize(fileSizeLimit);
            }
            mRecorder.setOnErrorListener(this);
            /**@}**/
            try{
            mRecorder.prepare();
            }catch(Exception e){
            	
            }
            mRecorder.start();
            setState(STATE_RECORDING);
    		return true;
    	}
        try {
            /**
             * M:Changed to catch the IllegalStateException and NullPointerException.
             * And the IllegalStateException will be caught and handled in RuntimeException
             * .@{
             */
            
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(1);
            mRecorder.setOutputFile(mSampleFile.getAbsolutePath());
            /**@}*/
            mRecorder.setAudioEncoder(3);
            mRecorder.setAudioChannels(2);
            mRecorder.setAudioEncodingBitRate(48000);
            mRecorder.setAudioSamplingRate(32000);
            if (fileSizeLimit > 0) {
                mRecorder.setMaxFileSize(fileSizeLimit);
            }
            mRecorder.setOnErrorListener(this);
            /**@}**/
            mRecorder.prepare();
            mRecorder.start();
            setState(STATE_RECORDING);
        } catch (IOException exception) {
            // M:Add for when error ,the tmp file should been delete.
            handleException(true, exception);
            setError(ERROR_RECORDING_FAILED);
            return false;
        }
        /**
         * M: used to catch the null pointer exception in ALPS01226113, 
         * and never show any toast or dialog to end user. Because this
         * error just happened when fast tapping the file list button 
         * after tapping record button(which triggered by tapping the 
         * recording button in audio play back view).@{
         */
        catch (NullPointerException exception){
            handleException(true, exception);
            return false;
        }
        /**
         * @}
         */
        catch (RuntimeException exception) {
            // M:Add for when error ,the tmp file should been delete.
            handleException(true, exception);
            setError(ERROR_RECORDER_OCCUPIED);
            return false;
        }
        
        return true;
    }

    /**
     * M: Handle Exception when call the function of MediaRecorder
     */
    public void handleException(boolean isDeleteSample, Exception exception) {
        Log.i("hcj", "handleException e="+exception);
        if (isDeleteSample && mSampleFile != null) {
            mSampleFile.delete();
        }
        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }
    
    private OnStateChangedListener mOnStateChangedListener = null;
    private void setError(int paramInt){    
      if (mOnStateChangedListener != null) {
         mOnStateChangedListener.onError(paramInt);
      }
    }
    
    public static abstract interface OnStateChangedListener{    
      public abstract void onError(int paramInt);      
      public abstract void onStateChanged(int paramInt);
    }
}
