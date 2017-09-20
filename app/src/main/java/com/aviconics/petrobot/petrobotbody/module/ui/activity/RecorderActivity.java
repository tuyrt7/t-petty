package com.aviconics.petrobot.petrobotbody.module.ui.activity;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.TextureView;
import android.view.WindowManager;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.Event;
import com.aviconics.petrobot.petrobotbody.util.MCameraHelper;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;
import com.blankj.utilcode.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;

/**
 * Created by win7 on 2016/9/10.
 */
public class RecorderActivity extends BaseActivity {

    @BindView(R.id.mPreview_Recorder)
    TextureView mPreview;
    private String TAG = "Recorder";

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int MAX_TIME = 1 * 60 * 1000;//默认录制视频的时间
    private static final long DEFAULT_SIZE = 100 * 1024 * 1024;//U盘默认留余空间 - 100M
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final long MIN_FILE_SIZE = 1 * 1024 * 1024;
    private static final int RATE = 30;
    private boolean isRecording = false;
    private File mOutputFile;
    private final int STOP = 0x1;
    private final int CALL_IN = 0x2;
    private boolean isCallIn;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case STOP:
                    break;
                case CALL_IN:
                    break;
                default:
                    break;
            }
        }
    };


    private Runnable timingTask = new Runnable() {
        @Override
        public void run() {
            //stop recoder
            stopRecorder();
            RecorderActivity.this.finish();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isCallIn) {
                        //录像结束，根据开关重启2个监控
                        // CameraMonitorHelper.openMonitor(RecorderActivity.this);
                    }
                }
            }, 3000);
        }
    };

    @Override
    protected boolean isRegisterEventBus() {
        return true;
    }

    @Override
    protected void beforeInit() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_recorder;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {

    }

    @Override
    protected void initData() {
        if (SignDevice.getSign().isPetUpdating()) {
            finish();
            return;
        }

        mPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                /*if (!checkUsbExists()) {
                    Pop.popToast(getApplicationContext(), "usb 未插入，不支持录像");
                    startActivity(new Intent(RecorderActivity.this, HintActivity.class).putExtra("info", "no_usb"));
                    RecorderActivity.this.finish();
                } else {*/
                    if (!isRecording) {
                        new MediaPrepareTask().execute(null, null, null);
                    }
                //}
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // if we are using MediaRecorder, release it first
        releaseMediaRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean checkUsbExists() {
        if (App.getUsbHelper().isUsbEnable()) {// usb是否存在
            String usbCardPath = App.getUsbHelper().getUsbCardPath();
            if (TextUtils.isEmpty(usbCardPath)) {
                RecorderActivity.this.finish();//不可能有这情况
                return false;
            }
            if (!FileUtils.createOrExistsDir(App.getUsbHelper().getPetVideoDir())) {
                return false;
            }
            long usbFreeBytes = App.getUsbHelper().getFreeBytes(usbCardPath);
            if (usbFreeBytes < DEFAULT_SIZE) { //内存不够时
                App.getUsbHelper().coverFile();
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean prepareVideoRecorder() {
        mCamera = getCameraInstance();
        if (mCamera == null || !initCamera()) {
            return false;
        }

        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        // mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        // Step 3: Set output format and encoding (for versions prior to API Level 8)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        mMediaRecorder.setVideoSize(WIDTH, HEIGHT);
        mMediaRecorder.setVideoEncodingBitRate(2 * WIDTH * HEIGHT);
        mMediaRecorder.setVideoFrameRate(RATE);

        mOutputFile = getOutputMediaFile();
        if (mOutputFile == null) {
            return false;
        }
        // Step 4: Set output file
        mMediaRecorder.setOutputFile(mOutputFile.getAbsolutePath());

        // Step 5: Set the preview output
        // mMediaRecorder.setPreviewDisplay(mPreview.getSurfaceTexture());//预览不行（）
        try {
            // Step 6: Prepare configured MediaRecorder
            mMediaRecorder.prepare();
        } catch (IOException exception) {
            releaseMediaRecorder();
            return false;
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            LogUtil.d("NICK", "Recorder,Camera is not available (in use or does not exist)");
        }

        if (c == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            boolean connected = false;
            for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                LogUtil.d(TAG, "Recorder,Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                try {
                    c = Camera.open(camIdx);
                    connected = true;
                } catch (RuntimeException e) {
                    Log.e(TAG, "Recorder ,Camera #" + camIdx + " failed to open. ");
                }
                if (connected)
                    break;
            }
        }
        return c; // returns null if camera is unavailable
    }


    private boolean initCamera() {
        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size previewSize = MCameraHelper.getOptimalVideoSize(parameters.getSupportedVideoSizes(),
                parameters.getSupportedPreviewSizes(), mPreview.getWidth(), mPreview.getHeight());
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        mCamera.setParameters(parameters);
        //mCamera.setDisplayOrientation(90);
        try {
            mCamera.setPreviewTexture(mPreview.getSurfaceTexture());
        } catch (IOException e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile() {
        String dirPath = App.getUsbHelper().getPetVideoDir();
        dirPath = Environment.getExternalStorageDirectory().getPath() + "/PetRobot/video";
        if (!FileUtils.createOrExistsDir(dirPath)) {
            return null;
        }
        // Create a media file name
        String timeStamp = getTimeStamp();
        File file = new File(dirPath + "/" + timeStamp + ".mp4");
        return file;
    }

    private String getTimeStamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
    }


    private void stopRecorder() {
        if (isRecording) {
            // BEGIN_INCLUDE(stop_release_media_recorder)
            // stop recording and release camera
            try {
                mMediaRecorder.stop();  // stop the recording
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mOutputFile.delete();
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            mCamera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
            isRecording = false;
            releaseCamera();
            // END_INCLUDE(stop_release_media_recorder)
        }
        checkOutputFile();
    }

    private void checkOutputFile() {
        if (mOutputFile != null && mOutputFile.exists() && mOutputFile.length() < MIN_FILE_SIZE) {
            try {
                mOutputFile.delete();
            } catch (Exception e) {
            }
        }
    }


    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            LogUtil.d("NICK", "---recorder_camera_release--");
            mCamera.release();
            mCamera = null;
        }
    }


    @Override
    protected void receiveEvent(Event event) {

    }
/* */

    /**
     * 接通 视频通话，格式化操作 停止录制
     *
     * @param
     *//*
    public void onEventMainThread(RecordEvent recordEvent) {
        LogUtil.d("NICK", "电话来了,收到停止录制消息:" + recordEvent.getInfo());
        call_info = recordEvent.getInfo();//传过来的来电号码

        releaseMediaRecorder();
        releaseCamera();

        if (videoFile != null && videoFile.exists() && videoFile.delete()) {
        }

        ControlBoardUtils.getInstance().envlight_seting(controlboardcom.EXTERN_ENVLIGHT_DIS);

        finish();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecorder = null;
                if (DemoHelper.getInstance().isCallArrive()) {
                    RecorderActivity.this.startActivity(
                            new Intent(RecorderActivity.this, VideoCallActivity.class).
                                    putExtra("username", call_info).putExtra("isComingCall", true).
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    if (DialogControl.getInstance().getDialogType() == DialogControl.Type.CALL) {
                        EventBus.getDefault().post(new UIEvent(MainActivity.UI_DISMISS));
                    }
                }
            }
        }, 3000);
    }*/

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                mMediaRecorder.start();
                isRecording = true;
            } else {
                releaseMediaRecorder();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                RecorderActivity.this.finish();
            } else {
                mHandler.postDelayed(timingTask, MAX_TIME);
            }
        }
    }
}

