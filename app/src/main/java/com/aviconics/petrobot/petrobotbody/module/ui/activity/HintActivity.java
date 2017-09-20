package com.aviconics.petrobot.petrobotbody.module.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.util.DialogControl;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;

import org.greenrobot.eventbus.EventBus;

/**
 * 提示页
 * U盘未插入，食盖打开
 */
public class HintActivity extends Activity {

    private static HintActivity mHintAct = null;

    public static HintActivity getHintAct() {
        return mHintAct;
    }

    private String info;
    private HintThread hintThread;
    private RelativeLayout rlRoot;
    private boolean mNoSleep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_hint);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        mHintAct = this;/*
        AppManager.getAppManager().pushActivity2Stack(this);
        EventBus.getDefault().registerSticky(this);*/
        initView();
        initDate();
    }

    private void initDate() {
        //CameraMonitorHelper.closeMonitor(App.getContext());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Intent i = getIntent();
        info = i.getStringExtra("info");
        mNoSleep = false;
        if ("no_usb".equals(info)) {
            rlRoot.setBackgroundResource(R.mipmap.usb_not_insert);
        } else if ("open_foodbox".equals(info)) {
            rlRoot.setBackgroundResource(R.mipmap.open_box);
        } else if ("reStartMovement".equals(info)) {//运动检测恢复
            LogUtil.i("NICK", "运动检测唤醒");
            mNoSleep = true;
            finish();
        } else {//意外打开
            mNoSleep = true;
            finish();
        }

        if (!mNoSleep && hintThread == null) {
            hintThread = new HintThread();
            hintThread.start();
        }
    }

    @Override
    protected void onResume() {
        SignDevice.getSign().setHintOn(true);
        super.onResume();
        resumeVideoCall();
        if (SignDevice.getSign().isUiDialogShow()) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    DialogControl.getInstance().dismissVideoDialog();
                }
            }, 500);
        } else {
        }
    }

    private void initView() {
        rlRoot = (RelativeLayout) findViewById(R.id.hint_rl_root);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SignDevice.getSign().setHintOn(false);
    /*    if (SignDevice.getSign().isCallByUser() && SignDevice.getSign().isSingleCall()) {
            EventBus.getDefault().post(new UIEvent(MainActivity.UI_CALL_SHOW));
        } else if (SignDevice.getSign().isPlayMusic()) {
            EventBus.getDefault().post(new UIEvent(MainActivity.UI_MUSIC_START));
        } else if ((!SignDevice.getSign().isConnectAc()) || (!NetWorkUtils.ping())) {
            EventBus.getDefault().post(new UIEvent(MainActivity.UI_CONN_SHOW));
            SignDevice.getSign().setShowConnInMain(true);//
        } else {
            CameraMonitorHelper.openMonitor(this);
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        info = null;

        mHintAct = null;

        EventBus.getDefault().unregister(this);
    }

   /* public void onEventMainThread(HintEvent hintEvent) {
        String info = hintEvent.getInfo();
        LogUtil.d("NICK", "提示页面收到消息:" + info);
        if ("connect".equals(info)) {
            if (HintActivity.this != null) {
                HintActivity.this.finish();
            }
        }
    }*/

    private class HintThread extends Thread {
        @Override
        public void run() {
            SystemClock.sleep(5 * 1000);
            if (info != null && info.equals("no_usb")) {
                SystemClock.sleep(10 * 1000);
            }
            hintThread = null;
            HintActivity.this.finish();
        }
    }

    private void resumeVideoCall() {
        //环信视频通话状态恢复
        if (SignDevice.getSign().isCallByUser()) {
            //EventBus.getDefault().post(new VideoCallEvent(VideoCallActivity.VIDEO_PLAY_RESUME_CALL));
        }
    }

}
