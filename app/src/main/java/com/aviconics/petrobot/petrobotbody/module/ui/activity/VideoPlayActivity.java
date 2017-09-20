//package com.aviconics.petrobot.petrobotbody.module.ui.activity;
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.media.MediaPlayer;
//import android.os.Bundle;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.MediaController;
//import android.widget.VideoView;
//
//import com.accloud.utils.LogUtil;
//import com.aviconics.petrobot.petrobotbody.R;
//import com.aviconics.petrobot.petrobotbody.app.App;
//import com.aviconics.petrobot.petrobotbody.module.service.MusicPlayerService;
//import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.UIEvent;
//import com.aviconics.petrobot.petrobotbody.util.CameraMonitorHelper;
//import com.aviconics.petrobot.petrobotbody.util.Pop;
//import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;
//import com.aviconics.petrobot.petrobotbody.util.SignDevice;
//import com.aviconics.petrobot.petrobotbody.zxing.activity.CaptureActivity;
//import com.badoo.mobile.util.WeakHandler;
//
//import org.greenrobot.eventbus.EventBus;
//
//import butterknife.BindView;
//import cn.mindpush.petrobot.controlboardcom.ControlUtil;
//import cz.msebera.android.httpclient.util.TextUtils;
//
///**
// * Created by win7 on 2017/2/24.
// */
//
//public class VideoPlayActivity extends BaseActivity implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
//
//    @BindView(R.id.vv_vp_player)
//    VideoView mVideoView;
//
//    public static final String VIDEO_MSG = "video_name";
//
//    public static final String VIDEO_STOP_AND_KEEP_POS_ACTION = "video_stop_and_keep_pos";//关 记录状态
//    public static final String VIDEO_NORMAL_STOP_ACTION = "video_normal_stop"; // 正常关
//    public static final String VIDEO_STOP_TO_MUSIC_ACTION = "video_stop_to_music"; // 关视频-切换播放音乐
//
//
//    private MediaController mMediaController;
//    private String mCurPath;
//    private VideoPlayReceiver vReceiver;
//    private int mPositionWhenStop = -1;
//    private boolean isStopVideoToMusic;
//    private WeakHandler mHandler;
//
//    private class VideoPlayReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            LogUtil.i("NICK", "VideoPlay stop action: " + intent.getAction());
//            isStopVideoToMusic = false;
//            switch (intent.getAction()) {
//                case VIDEO_STOP_AND_KEEP_POS_ACTION:
//                    notifyVideoCall();
//                    stopAndKeepPos(true);
//                    break;
//                case VIDEO_STOP_TO_MUSIC_ACTION:
//                    isStopVideoToMusic = true;
//                    notifyVideoCall();
//                    stopAndKeepPos(false);
//                    break;
//                case VIDEO_NORMAL_STOP_ACTION:
//                    stopAndKeepPos(false);
//                    break;
//                default:
//                    break;
//            }
//            VideoPlayActivity.this.finish();
//        }
//    }
//
//
//    @Override
//    protected void onNewIntent(Intent intent) {
//        super.onNewIntent(intent);
//        setIntent(intent);
//    }
//
//    @Override
//    protected void beforeInit() {
//        getWindow().addFlags(
//                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
//    }
//
//    @Override
//    protected int getLayoutId() {
//        return R.layout.actvity_videoplay;
//    }
//
//    @Override
//    protected void initView(Bundle savedInstanceState) {
//        mHandler = new WeakHandler();
//        registerVideoReceiver();
//
//        //Create media controller，组件可以控制视频的播放，暂停，回复，seekTo
//        mMediaController = new MediaController(this);
//        mVideoView.setMediaController(mMediaController);
//        mMediaController.setVisibility(View.INVISIBLE);
//        mVideoView.setOnCompletionListener(this);
//        mVideoView.setOnErrorListener(this);
//    }
//
//    @Override
//    protected void initData() {
//        if (SignDevice.getSign().isPetUpdating()) {
//            VideoPlayActivity.this.finish();
//            return;
//        }
//    }
//
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//    }
//
//    @Override
//    protected void onResume() {
//        SignDevice.getSign().setVideoPlayOn(true);
//        super.onResume();
//        ControlUtil.getInstance().playMusic();
//        String extra = getIntent().getStringExtra(VIDEO_MSG);
//
//        //排除正在播视频时再次播放当前视频
//        if (mVideoView.isPlaying() && mCurPath.equals(extra)) {
//            return;
//        }
//        if (!TextUtils.isEmpty(extra))
//            mCurPath = extra;
//        play();
//
//    }
//
//    @Override
//    protected void onPause() {
//        LogUtil.i("NICK", "videoplay  onPause");
//        ControlUtil.getInstance().stop_music();
//        super.onPause();
//        SignDevice.getSign().setVideoPlayOn(false);
//    }
//
//    @Override
//    protected void onStop() {
//        LogUtil.i("NICK", "videoplay  onStop");
//        mHandler.removeCallbacks(null);
//        super.onStop();
//        stopAndKeepPos(false);
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        LogUtil.i("NICK", "videoplay  onDestroy");
//        unregisterReceiver(vReceiver);
//        if (!isStopVideoToMusic) {
//            reportVideoStop();
//            if (!SignDevice.getSign().isUiDialogShow()) {
//                //根据开关,运动检测 + 声音检测
//                CameraMonitorHelper.openMonitor(VideoPlayActivity.this);
//            }
//        }
//    }
//
//    @Override
//    public void onPrepared(MediaPlayer mp) {
//        if (mVideoView != null) {
//            if (mCurPath.equals(getStopVideoUrlFromSp())) {
//                int lastPos = getStopVideoPosFromSp();
//                if (lastPos < 0) {
//                    lastPos = 0;
//                } else if (lastPos > mVideoView.getDuration()) {
//                    lastPos = mVideoView.getDuration();
//                }
//                mVideoView.seekTo(lastPos);
//            } else {
//                removeStopVideoToSp();
//            }
//            //上报并记录 播放状态 关闭dialog 显示背光
//            SignDevice.getSign().setVideoPlay(true);
//            reportVideoPlay();
//
//            //环信视频通话状态恢复
//            if (SignDevice.getSign().isCallByUser() && SignDevice.getSign().isSingleCall()) {
//                EventBus.getDefault().post(new VideoCallEvent(VideoCallActivity.VIDEO_PLAY_RESUME_CALL));
//            }
//
//            //start video
//            mVideoView.start();
//            ControlBoardUtils.getInstance().back_light(1);
//
//            //在视频界面完成之后 再关闭ui窗口
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    dismissUiDialog();
//                    //关音频
//                    if (SignDevice.getSign().isPlayMusic()) {
//                        sendBroadcast(new Intent().setAction(MusicPlayerService.STOP_MUSIC_ACTION));
//                    }
//                }
//            }, 1000);
//        }
//    }
//
//    @Override
//    public void onCompletion(MediaPlayer mp) {
//        notifyVideoCall();
//        // 播放结束后的动作
//        removeStopVideoToSp();
//        VideoPlayActivity.this.finish();
//    }
//
//    @Override
//    public boolean onError(MediaPlayer mp, int what, int extra) {
//        notifyVideoCall();
//        VideoPlayActivity.this.finish();
//        return true;
//    }
//
//    private void registerVideoReceiver() {
//        vReceiver = new VideoPlayReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(VIDEO_STOP_AND_KEEP_POS_ACTION);
//        filter.addAction(VIDEO_STOP_TO_MUSIC_ACTION);
//        filter.addAction(VIDEO_NORMAL_STOP_ACTION);
//        registerReceiver(vReceiver, filter);
//    }
//
//    private void play() {
//        if (TextUtils.isEmpty(mCurPath)) {
//            VideoPlayActivity.this.finish();
//            return;
//        }
//        // Play Video
//        try {
//            mVideoView.setOnPreparedListener(this);
//            mVideoView.setVideoPath(mCurPath);
//        } catch (Exception e) {
//            String s = "";
//            if (mCurPath != null) {
//                s = mCurPath.substring(mCurPath.lastIndexOf("/") + 1);
//            }
//            Pop.popToast(App.getContext(), s + "播放错误");
//            VideoPlayActivity.this.finish();
//        }
//    }
//
//    private void stopAndKeepPos(boolean isKeepPos) {
//        SignDevice.getSign().setVideoPlay(false);
//        if (isKeepPos) {
//            mPositionWhenStop = mVideoView.getCurrentPosition();
//            keepStopVideoToSp();
//        }
//        // Stop Video
//        mVideoView.stopPlayback();
//
//    }
//
//    private static final String VIDEO_LAST_URL = "video_last_url";
//    private static final String VIDEO_LAST_POSITION = "video_last_position";
//
//    private void keepStopVideoToSp() {
//        if (mPositionWhenStop > 0) {
//            SharedPrefs.putString(App.getContext(), VIDEO_LAST_URL, mCurPath);
//            SharedPrefs.putInt(App.getContext(), VIDEO_LAST_POSITION, mPositionWhenStop);
//        }
//    }
//
//    private void removeStopVideoToSp() {
//        SharedPrefs.remove(App.getContext(), VIDEO_LAST_URL);
//        SharedPrefs.remove(App.getContext(), VIDEO_LAST_POSITION);
//    }
//
//    private String getStopVideoUrlFromSp() {
//        return SharedPrefs.getString(App.getContext(), VIDEO_LAST_URL, "");
//    }
//
//    private int getStopVideoPosFromSp() {
//        return SharedPrefs.getInt(App.getContext(), VIDEO_LAST_POSITION, 0);
//    }
//
//    private void reportVideoPlay() {
//        EventBus.getDefault().post(new StateReportEvent(MainActivity.VIDEO_PLAY_TYPE, mCurPath));
//    }
//
//    private void reportVideoStop() {
//        EventBus.getDefault().post(new StateReportEvent(MainActivity.VIDEO_STOP_TYPE, ""));
//    }
//
//    private void dismissUiDialog() {
//        EventBus.getDefault().post(new UIEvent(MainActivity.UI_DISMISS));
//    }
//
//    //单向通话-->视频关闭显示ui
//    private void notifyVideoCall() {
//        if (SignDevice.getSign().isCallByUser() && SignDevice.getSign().isSingleCall()) {
//            EventBus.getDefault().post(new UIEvent(MainActivity.UI_CALL_SHOW));
//        }
//    }
//
//    private void showPressUI() {
//        if (VideoCallActivity.getVideoCall() == null && CaptureActivity.getCapture() == null)
//            EventBus.getDefault().post(new UIEvent(MainActivity.UI_EXPRESS_SHOW));
//    }
//}
