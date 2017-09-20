//package com.aviconics.petrobot.petrobotbody.module.ui.activity;
//
//import android.content.Intent;
//import android.graphics.ImageFormat;
//import android.graphics.Rect;
//import android.graphics.YuvImage;
//import android.hardware.Camera;
//import android.os.Bundle;
//import android.os.SystemClock;
//import android.provider.Settings;
//import android.util.Log;
//import android.view.View;
//import android.view.WindowManager;
//
//import com.accloud.utils.LogUtil;
//import com.aviconics.petrobot.petrobotbody.R;
//import com.aviconics.petrobot.petrobotbody.em.DemoHelper;
//import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.UIEvent;
//import com.aviconics.petrobot.petrobotbody.net.IssuedHelper;
//import com.aviconics.petrobot.petrobotbody.util.SignDevice;
//import com.hyphenate.chat.EMCallManager;
//import com.hyphenate.chat.EMCallManager.EMCameraDataProcessor;
//import com.hyphenate.chat.EMCallStateChangeListener;
//import com.hyphenate.chat.EMClient;
//import com.hyphenate.exceptions.HyphenateException;
//import com.hyphenate.media.EMLocalSurfaceView;
//import com.hyphenate.media.EMOppositeSurfaceView;
//import com.hyphenate.util.EMLog;
//import com.superrtc.sdk.VideoView;
//
//import org.greenrobot.eventbus.EventBus;
//
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//
//import cn.mindpush.petrobot.controlboardcom.controlboardcom;
//
//public class VideoCallActivity extends CallActivity {
//
//    private boolean isAnswered;
//    private boolean monitor;
//
//    // 视频通话画面显示控件，这里在新版中使用同一类型的控件，方便本地和远端视图切换
//    protected EMLocalSurfaceView localSurface;
//    protected EMOppositeSurfaceView oppositeSurface;
//
//    boolean isRecording = false;
//    private boolean mIsPauseVoice;
//    private boolean mIsPauseVideo;
//    //    private Button recordBtn;
//    private EMCallManager.EMVideoCallHelper callHelper;
//    private boolean isInCalling;
//
//    private static VideoCallActivity mVideoCall = null;
//
//    public static VideoCallActivity getVideoCall() {
//        return mVideoCall;
//    }
//
//    public static final int VIDEO_QUALITY_LOWER = 1;
//    public static final int VIDEO_QUALITY_NORMAL = 2;
//    public static final int VIDEO_QUALITY_HIGH = 3;
//    public static final int VIDEO_MODE_SINGLE = 4;
//    public static final int VIDEO_MODE_DOUBLE = 5;
//    public static final int VIDEO_CALL_END = 6;
//    public static final int VIDEO_CALL_PAUSE = 7;
//    public static final int VIDEO_CALL_RESUME = 8;
//    public static final int VIDEO_CALL_MIS_CONNECT = 9;
//    public static final int VIDEO_TAKE_PHOTO = 10;
//    public static final int VIDEO_CALL_PAUSE_VOICE = 11;
//    public static final int VIDEO_CALL_RESUME_VOICE = 12;
//    public static final int VIDEO_PLAY_RESUME_CALL = 13;
//    public static final int VIDEO_START_MONITOR = 14;
//    public static final int VIDEO_ACCEPTED = 15;
//
//    private BrightnessDataProcess dataProcessor = new BrightnessDataProcess();
//
//    // dynamic adjust brightness
//    class BrightnessDataProcess implements EMCameraDataProcessor {
//        byte yDelta = 0;
//
//        synchronized void setYDelta(byte yDelta) {
//            EMLog.d("VideoCallActivity", "brigntness uDelta:" + yDelta);
//            this.yDelta = yDelta;
//        }
//
//        // data size is width*height*2
//        // the first width*height is Y, second part is UV
//        // the storage layout detailed please refer 2.x demo CameraHelper.onPreviewFrame
//        @Override
//        public synchronized void onProcessData(byte[] data, Camera camera, final int width, final int height, final int rotateAngel) {
//            int wh = width * height;
//            for (int i = 0; i < wh; i++) {
//                int d = (data[i] & 0xFF) + yDelta;
//                d = d < 16 ? 16 : d;
//                d = d > 235 ? 235 : d;
//                data[i] = (byte) d;
//            }
//
//            if (IssuedHelper.getInstance().isTakePhoto()) {//从本地摄像头流数据中取一张图
//                takePhoto(data, camera);
//                IssuedHelper.getInstance().setTakePhoto(false);
//            }
//        }
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        if (savedInstanceState != null) {
//            finish();
//            return;
//        }
//        setContentView(R.layout.em_activity_video_call);
//        DemoHelper.getInstance().isVideoCalling = true;
//        getWindow().addFlags(
//                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//
//        mVideoCall = this;
//        AppManager.getAppManager().pushActivity2Stack(this);
//        EventBus.getDefault().register(this);//注册事件
//
//        setAutoScreenOrientation(true);
//
//        //if (!QueryKeyStateService.envirLightState) {
//        ControlBoardUtils.getInstance().envlight_seting(controlboardcom.EXTERN_ENVLIGHT_EN);
//        //}
//
//        callType = 1;
//
//        isInComingCall = getIntent().getBooleanExtra("isComingCall", false);
//
//        // local surfaceview
//        localSurface = (EMLocalSurfaceView) findViewById(R.id.local_surface);
//        localSurface.setZOrderMediaOverlay(true);
//        localSurface.setZOrderOnTop(true);
//
//        // remote surfaceview
//        oppositeSurface = (EMOppositeSurfaceView) findViewById(R.id.opposite_surface);
//
//        if (!isInComingCall) {// outgoing call
//            //soundPool = new SoundPool(1, AudioManager.STREAM_RING, 0);
//            //outgoing = soundPool.load(this, R.raw.outgoing, 1);
//            EMClient.getInstance().callManager().setSurfaceView(localSurface, oppositeSurface);
//            handler.sendEmptyMessage(MSG_CALL_MAKE_VIDEO);
//            handler.postDelayed(new Runnable() {
//                public void run() {
//                    //streamID = playMakeCallSounds();
//                }
//            }, 300);
//        } else { // incoming call
//            if (EMClient.getInstance().callManager().getCallState() == EMCallStateChangeListener.CallState.IDLE
//                    || EMClient.getInstance().callManager().getCallState() == EMCallStateChangeListener.CallState.DISCONNECTED) {
//                // the call has ended
//                finish();
//                return;
//            }
//            localSurface.setVisibility(View.INVISIBLE);
//            EMClient.getInstance().callManager().setSurfaceView(localSurface, oppositeSurface);
//        }
//
//        /*final int MAKE_CALL_TIMEOUT = 50 * 1000;
//        handler.removeCallbacks(timeoutHangup);
//        handler.postDelayed(timeoutHangup, MAKE_CALL_TIMEOUT);*/
//
//        // get instance of call helper, should be called after setSurfaceView was called
//        callHelper = EMClient.getInstance().callManager().getVideoCallHelper();
//
//        EMClient.getInstance().callManager().setCameraDataProcessor(dataProcessor);
//
//        answerCall();//2s直接接听电话
//        oppositeSurface.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
//
//        showUiDialog();
//    }
//
//    private void showUiDialog() {
//        EventBus.getDefault().post(new UIEvent(MainActivity.UI_CALL_SHOW));
//    }
//
//    private void dismissUiDialog() {
//        EventBus.getDefault().post(new UIEvent(MainActivity.UI_DISMISS));
//    }
//
//    private void showExpress() {
//        EventBus.getDefault().post(new UIEvent(MainActivity.UI_EXPRESS_SHOW));
//    }
//
//    private void showMusic() {
//        EventBus.getDefault().post(new UIEvent(MainActivity.UI_MUSIC_START));
//    }
//
//    @Override
//    protected void onResume() {
//        ControlBoardUtils.getInstance().back_light(1);
//        super.onResume();
//        if (isInCalling) {
//            try {
//                EMClient.getInstance().callManager().resumeVideoTransfer();
//            } catch (HyphenateException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    protected void onPause() {
//
//        super.onPause();
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        //        if (SignDevice.getSign().isSingleCall()) {
//        //            EventBus.getDefault().post(new UIEvent(MainActivity.UI_DISMISS));
//        //        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        DemoHelper.getInstance().isVideoCalling = false;
//        stopMonitor();
//        if (isRecording) {
//            callHelper.stopVideoRecord();
//            isRecording = false;
//        }
//        if (localSurface != null)
//            localSurface.getRenderer().dispose();
//        localSurface = null;
//        if (oppositeSurface != null)
//            oppositeSurface.getRenderer().dispose();
//        oppositeSurface = null;
//
//        isAnswered = false;
//
//        SignDevice.getSign().setCallByUser(false);
//        SignDevice.getSign().setSingleCall(true);
//
//        if (VideoPlayActivity.getVideoPlay() != null) {
//            sendBroadcast(new Intent().setAction(VideoPlayActivity.VIDEO_NORMAL_STOP_ACTION));
//        }
//        //显示正确的表情ui，启动监控
//        if (SignDevice.getSign().isPlayMusic()) {
//            showMusic();
//        } else {
//            showExpress();
//        }
//
//        super.onDestroy();
//
//        EventBus.getDefault().unregister(this);
//
//        //清除一次data
//        CleanDataManager.cleanCacheData(this);
//
//        ControlBoardUtils.getInstance().envlight_seting(controlboardcom.EXTERN_ENVLIGHT_DIS);
//
//        mVideoCall = null;
//        AppManager.getAppManager().popActivityStack(this);
//    }
//
//    /**
//     * 自动切换屏幕开关
//     *
//     * @param bool
//     */
//    private void setAutoScreenOrientation(boolean bool) {
//        //0为关闭 1为开启  自动旋转
//        Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, bool ? 1 : 0);
//        int flag = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
//
//        String state = flag == 0 ? "关闭" : "开启";
//        LogUtil.d("NICK-", "自动旋转" + state + "状态");
//    }
//
//    private void endCall() {
//        if (isRecording) {
//            callHelper.stopVideoRecord();
//            isRecording = false;
//        }
//        handler.sendEmptyMessage(MSG_CALL_END);
//    }
//
//    private void answerCall() {
//        EMLog.d(TAG, "btn_answer_call clicked");
//        handler.sendEmptyMessageDelayed(MSG_CALL_ANSWER, 100);
//        isAnswered = true;
//        //localSurface.setVisibility(View.VISIBLE);
//    }
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//    }
//
//
//    /**
//     * for debug & testing, you can remove this when release
//     */
//    void startMonitor() {
//        if (!monitor) {
//            monitor = true;
//            MonitorTask monitorTask = new MonitorTask();
//            new Thread(monitorTask, "CallMonitor").start();
//        }
//    }
//
//    void stopMonitor() {
//        monitor = false;
//    }
//
//    private class MonitorTask implements Runnable {
//        @Override
//        public void run() {
//            int code = 0;
//            while (monitor) {
//                int remoteBitrate = callHelper.getRemoteBitrate();
//                if (remoteBitrate == 0) {
//                    code++;
//                    if (code == 20) {
//                        if (isAnswered) {
//                            LogUtil.d("NICK", "连续30秒没有获取到远程视频数据的比特率，关闭视频通话");
//                            endCall();
//                            break;
//                        }
//                    }
//                } else {
//                    code = 0;
//                }
//                SystemClock.sleep(1500);
//            }
//
//        }
//    }
//
//    @Override
//    protected void onUserLeaveHint() {
//        super.onUserLeaveHint();
//        if (isInCalling) {
//            try {
//                EMClient.getInstance().callManager().pauseVideoTransfer();
//            } catch (HyphenateException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void onEventMainThread(VideoCallEvent VideoCallEvent) throws HyphenateException {
//        LogUtil.d("NICK", "接收到mode 消息" + VideoCallEvent.getMode());
//        if (!isAnswered) {
//            LogUtil.d("NICK", "视频通话还未接通");
//            return;
//        }
//        switch (VideoCallEvent.getMode()) {
//            case VIDEO_MODE_SINGLE://单向
//                setSingleMode();
//                break;
//            case VIDEO_MODE_DOUBLE://双向
//                setDoubleMode();
//                break;
//            case VIDEO_CALL_MIS_CONNECT://掉线5s关闭通话
//                finish();
//                break;
//            case VIDEO_CALL_PAUSE: //暂停语音、视频（图像）数据传输：
//                setPauseVoice();
//                setPauseVideo();
//                break;
//            case VIDEO_CALL_RESUME://恢复语音、视频（图像）数据传输：
//                setResumeVoice();
//                setResumeVideo();
//                break;
//            case VIDEO_CALL_END://挂断电话
//                endCall();
//                break;
//            case VIDEO_TAKE_PHOTO: //测试拍照（取对方摄像头数据成像）
//                MyFileUtils.mkDir(RobotApp.getUsbHelper().getPetPhotoDir());
//                String photoPath = RobotApp.getUsbHelper().getPetPhotoDir() + "/" + IssuedHelper.getInstance().getTimeStamp() + ".jpg";
//                EMClient.getInstance().callManager().getVideoCallHelper().takePicture(photoPath);
//                break;
//            case VIDEO_CALL_PAUSE_VOICE://  暂停语音、
//                setPauseVoice();
//                break;
//            case VIDEO_CALL_RESUME_VOICE: //  恢复语音、
//                setResumeVoice();
//                break;
//            case VIDEO_PLAY_RESUME_CALL:
//                //播放视频时，画面暂停恢复、声音根据之前状态恢复
//                if (!mIsPauseVoice) {
//                    setResumeVoice();
//                }
//                if (!mIsPauseVideo) {
//                    setResumeVideo();
//                }
//                break;
//            case VIDEO_START_MONITOR:
//                startMonitor();
//                break;
//            case VIDEO_ACCEPTED: //远程数据监控
//                isInCalling = true;
//                break;
//
//            default:
//                break;
//        }
//    }
//
//    private void setPauseVoice() throws HyphenateException {
//        EMClient.getInstance().callManager().pauseVoiceTransfer();
//        mIsPauseVoice = true;
//    }
//
//    private void setResumeVoice() throws HyphenateException {
//        mIsPauseVoice = false;
//        EMClient.getInstance().callManager().resumeVoiceTransfer();
//    }
//
//    private void setPauseVideo() throws HyphenateException {
//        mIsPauseVideo = true;
//        EMClient.getInstance().callManager().pauseVideoTransfer();
//    }
//
//    private void setResumeVideo() throws HyphenateException {
//        mIsPauseVideo = false;
//        EMClient.getInstance().callManager().resumeVideoTransfer();
//    }
//
//    /**
//     * 设置单向视频模式
//     */
//    public void setSingleMode() {
//        LogUtil.d("NICK", "切换单向视频");
//        SignDevice.getSign().setSingleCall(true);
//
//        showUiDialog();
//
//        ControlBoardUtils.getInstance().stop_music();
//    }
//
//    /**
//     * 设置双向视频模式
//     */
//    public void setDoubleMode() {
//        LogUtil.d("NICK", "切换双向视频");
//        SignDevice.getSign().setSingleCall(false);
//        ControlBoardUtils.getInstance().play_music();
//
//        //关闭显示
//        dismissUiDialog();
//        if (VideoPlayActivity.getVideoPlay() != null) {
//            sendBroadcast(new Intent().setAction(VideoPlayActivity.VIDEO_NORMAL_STOP_ACTION));
//        }
//    }
//
//    /**
//     * 抓取图片（本地摄像头）
//     *
//     * @param data
//     * @param camera
//     */
//    private synchronized void takePhoto(byte[] data, Camera camera) {
//        Camera.Size size = camera.getParameters().getPreviewSize();
//        try {
//            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
//            if (image != null) {
//                ByteArrayOutputStream stream = new ByteArrayOutputStream();
//                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, stream);
//                File photoDir = MyFileUtils.mkDir(RobotApp.getUsbHelper().getPetPhotoDir());
//                String timeStamp = IssuedHelper.getInstance().getTimeStamp();
//                FileOutputStream fos = new FileOutputStream(new File(photoDir, timeStamp + ".jpg"));
//                //Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
//                //stream.close();
//                fos.write(stream.toByteArray());
//                fos.flush();
//                fos.close();
//
//                LogUtil.d("NICK", "---------------获取到了----------------" + size.width + "----" + size.height + RobotApp.getUsbHelper().getPetPhotoDir() + "/" + timeStamp + ".jpg");
//            }
//        } catch (Exception ex) {
//            Log.e("NICK", "Sys Error:" + ex.getMessage());
//        }
//    }
//
//}
