package com.aviconics.petrobot.petrobotbody.util;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.module.ui.activity.RecorderActivity;
import com.aviconics.petrobot.petrobotbody.view.CustomVideoView;
import com.aviconics.petrobot.petrobotbody.view.VideoPlayDialog;
import com.blankj.utilcode.util.ActivityUtils;


import static android.view.animation.Animation.INFINITE;

/**
 * Created by futao on 2017/4/21.
 */
public class DialogControl implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener {

    private int mType;

    private DialogControl() {
    }

    public static DialogControl getInstance() {
        return InnerSingle.ourInstance;
    }

    private static class InnerSingle {
        private static final DialogControl ourInstance = new DialogControl();
    }

    private VideoPlayDialog mVideoDialog;
    private Context mContext;

    private ImageView mDisk;
    private ImageView mIndicator;
    private TextView mTvLoading;
    private CustomVideoView mVideoView;
    private MediaController mMediaController;
    private Uri mUri;
    private RelativeLayout mMusicContainer;
    private RelativeLayout mVideoContainer;


    public void initDialog(Activity context) {
        mContext = context;
        if (mContext != null) {
            mVideoDialog = new VideoPlayDialog(mContext, R.style.call_dialog);
            mVideoDialog.setCanceledOnTouchOutside(false);
            mVideoDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

            View view = mVideoDialog.getDialogView();

            mMusicContainer = (RelativeLayout) view.findViewById(R.id.ll_container_music);
            mVideoContainer = (RelativeLayout) view.findViewById(R.id.ll_container_video);

            mVideoView = (CustomVideoView) view.findViewById(R.id.vv_video_dialog);
            //mVideoView.setZOrderOnTop(true);//解决videoview 透明的问题,但不能动态切换它上面的view（Gone和visible）显示、隐藏
            mTvLoading = (TextView) view.findViewById(R.id.tv_dialog_loading);

            mDisk = (ImageView) view.findViewById(R.id.iv_disk);
            mIndicator = (ImageView) view.findViewById(R.id.iv_indicator);

            mMediaController = new MediaController(context);
            mVideoView.setOnCompletionListener(this);
            mVideoView.setOnErrorListener(this);
            mVideoView.setMediaController(mMediaController);
            mMediaController.setVisibility(View.INVISIBLE);
        } else {
            dismissVideoDialog();
            mVideoDialog = null;
        }
    }

    private Uri getUri(int dialogType) {
        mType = dialogType;
        Uri uri = null;
        switch (dialogType) {
            case Type.HINT_ADD:
                uri = getUriFromRawId(R.raw.ui_hint_down_apk);
                break;
            case Type.HINT_DOWN:
                uri = getUriFromRawId(R.raw.ui_hint_add);
                break;
            case Type.UPDATE_APP:
                uri = getUriFromRawId(R.raw.ui_update_1);
                break;
            case Type.SCAN:
                uri = getUriFromRawId(R.raw.ui_scan_2_2);
                break;
            case Type.CONNECTING:
                uri = getUriFromRawId(R.raw.ui_connecting_3);
                break;
            case Type.HANDLE:
                uri = getUriFromRawId(R.raw.ui_handle_3_0);
                break;
            case Type.CONN_OK:
                uri = getUriFromRawId(R.raw.ui_conn_succese_4_1);
                break;
            case Type.CONN_FAIL:
                uri = getUriFromRawId(R.raw.ui_conn_fail_4_2);
                break;
            case Type.FEED:
                uri = getUriFromRawId(R.raw.ui_feed_5);
                break;
            case Type.EXEPRESS:
                uri = getUriFromRawId(R.raw.ui_express_6);
                break;
            case Type.CALL:
                uri = getUriFromRawId(R.raw.ui_on_call_7);
                break;
            case Type.WIFI_DIS:
                uri = getUriFromRawId(R.raw.ui_wifi_weak_8);
                break;
            case Type.MUSIC:
                break;
            case Type.DISS:
                break;
            default:
                break;
        }
        return uri;
    }

    private Uri getUriFromRawId(int rawId) {
        return Uri.parse("android.resource://" + App.getContext().getPackageName() + "/" + rawId);
    }


    //---------showDevDialog----小视频MP4播放-----------------------------------------------------

    public void showDevDialog(int type) {
        if (Type.DISS != type) {
            Uri uri = getUri(type);
            selectUiWithType();
            if (mType == Type.MUSIC) {
                showMusic2();
            } else {
                showVideo(uri);
            }
        } else {
            dismissVideoDialog();
        }
    }


    /**
     * dialog 是否正在显示
     *
     * @return
     */
    public boolean isDialogShowing() {
        return mVideoDialog != null && mVideoDialog.isShowing();
    }


    /**
     * dialog 显示类型（根据内容划分）
     *
     * @return
     */
    public int getDialogType() {
        return this.mType;
    }

    public void dismissVideoDialog() {
        this.mType = Type.DISS;
        if (isDialogShowing()) {
            mVideoDialog.dismiss();
        }
        disBackLight();
    }

    public void showVideo(Uri uri) {
        if (!isDialogShowing()) {
            mVideoDialog.show();
        }
        setVideoUri(uri);
    }

    public void showMusic2() {
        stopPlay();
        if (!isDialogShowing()) {
            mVideoDialog.show();
        }
        startMusicAnim();
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mType == Type.CONNECTING || mType == Type.CALL || mType == Type.UPDATE_APP) {
            //连接中、语音通话、更新apk
            play();
        } else if (mType == Type.SCAN) {
            //扫码提示-
            //CaptureActivity.startToCapture(mContext, false, false);
            dismissVideoDialog();
        } else if (mType == Type.HANDLE) {
            //处理中
            mUri = getUri(Type.CONNECTING);
            play();
        } else if (mType == Type.CONN_OK) {
            //连接成功
            mUri = getUri(Type.EXEPRESS);
            play();
        } else if (mType == Type.CONN_FAIL) {
            //连接失败
            dismissVideoDialog();
            //CameraMonitorHelper.closeMonitor(mContext);
        } else if (mType == Type.FEED) {
            //喂食
            if (SignDevice.getSign().isSingleCall()) {
                mUri = getUri(Type.CALL);
                play();
                LogUtil.i("NICK", "play call...");
            } else if (SignDevice.getSign().isPlayMusic()) {
                getUri(Type.MUSIC);
                selectUiWithType();
                showMusic2();
                LogUtil.i("NICK", "play music...");
            } else {
                dismissVideoDialog();
                LogUtil.i("NICK", "feed ..diss.");
            }
        } else if (mType == Type.EXEPRESS) {
            //表情
            dismissVideoDialog();

            ActivityUtils.startActivity(RecorderActivity.class);
            //CameraMonitorHelper.openMonitor(mContext);
        } else if (mType == Type.WIFI_DIS) {
            //wifi断开
            dismissVideoDialog();
            //CaptureActivity.startToCapture(mContext, false, false);
        } else {
            dismissVideoDialog();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogUtil.e("DialogControl", "Play onError called");
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                LogUtil.e("Play Error:::", "MEDIA_ERROR_SERVER_DIED");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                LogUtil.e("Play Error:::", "MEDIA_ERROR_UNKNOWN");
                break;
            default:
                break;
        }


        if (mType != Type.MUSIC) {
            mVideoView.stopPlayback();
            if (mType != Type.CALL) {
                dismissVideoDialog();
            }
        }
        return true;
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mType != Type.MUSIC && mType != Type.DISS) {
            mVideoView.start();
        }
    }


    public void setVideoUri(Uri uri) {
        mUri = uri;
        play();
    }


    // Play Video
    private void play() {
        try {
            mVideoView.setVideoURI(mUri);
            mVideoView.setOnPreparedListener(this);
        } catch (Exception e) {
            mVideoView = null;
            mVideoDialog.dismiss();
        }
    }

    private void stopPlay() {
        if (mVideoView != null && mVideoView.isPlaying()) {
            mVideoView.stopPlayback();
        }
    }

    private void showBackLight() {
       // ControlUtil.getInstance().backLight(1);
    }

    private void disBackLight() {
    }


    public void selectUiWithType() {
        if (mType != Type.MUSIC) {
            LogUtil.i("NICK", "selectUiWithType:  video");
            mVideoContainer.setVisibility(View.VISIBLE);
            mMusicContainer.setVisibility(View.GONE);
            stopMusicAnim();
        } else {
            LogUtil.i("NICK", "selectUiWithType:  music");
            mVideoContainer.setVisibility(View.GONE);
            mMusicContainer.setVisibility(View.VISIBLE);
            stopPlay();
        }

    }

    //-----------------------------------------

    private AnimatorSet mAnimatorSet;
    private ObjectAnimator mNeedleAnim, mRotateAnim;


    public void startMusicAnim() {
        mNeedleAnim = ObjectAnimator.ofFloat(mIndicator, "rotation", 18f, 24f);
        mIndicator.setPivotX(13);
        mIndicator.setPivotY(77);
        mNeedleAnim.setDuration(1000);
        mNeedleAnim.setRepeatMode(ValueAnimator.REVERSE);
        mNeedleAnim.setRepeatCount(INFINITE);
        mNeedleAnim.setInterpolator(new LinearInterpolator());

        mRotateAnim = ObjectAnimator.ofFloat(mDisk, "rotation", 0f, 360f);
        mRotateAnim.setDuration(5 * 1000);
        mRotateAnim.setRepeatMode(ValueAnimator.RESTART);
        mRotateAnim.setRepeatCount(ValueAnimator.INFINITE);
        mRotateAnim.setInterpolator(new LinearInterpolator());
        if (mAnimatorSet == null) {
            mAnimatorSet = new AnimatorSet();
            mAnimatorSet.play(mRotateAnim).with(mNeedleAnim);
            mAnimatorSet.start();
        }

    }

    private void stopMusicAnim() {
        if (mRotateAnim != null) {
            mRotateAnim.cancel();
            mRotateAnim = null;
        }
        if (mNeedleAnim != null) {
            mNeedleAnim.cancel();
            mNeedleAnim = null;
        }
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
            mAnimatorSet = null;
        }
    }


    public class Type {
        public static final int HINT_ADD = 1;
        public static final int HINT_DOWN = 2;
        public static final int UPDATE_APP = 3;
        public static final int SCAN = 4;
        public static final int CONNECTING = 5;
        public static final int HANDLE = 6;
        public static final int CONN_OK = 7;
        public static final int CONN_FAIL = 8;
        public static final int FEED = 9;
        public static final int EXEPRESS = 10;
        public static final int CALL = 11;
        public static final int WIFI_DIS = 12;
        public static final int MUSIC = 13;
        public static final int DISS = 14;
    }
}


