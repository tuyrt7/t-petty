package com.aviconics.petrobot.petrobotbody.module.ui.activity;

import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.accloud.clientservice.AC;
import com.accloud.clientservice.ACNetworkChangeReceiver;
import com.accloud.clientservice.PayloadCallback;
import com.accloud.common.ACDeviceMsg;
import com.accloud.common.ACException;
import com.accloud.service.ACConnectChangeListener;
import com.accloud.service.ACMsgHandler;
import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.configs.ConstantValue;
import com.aviconics.petrobot.petrobotbody.em.DemoHelper;
import com.aviconics.petrobot.petrobotbody.manager.SpManager;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.Event;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventBusUtil;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventCode;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.BindEvent;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.UIEvent;
import com.aviconics.petrobot.petrobotbody.net.IssuedHelper;
import com.aviconics.petrobot.petrobotbody.net.ReportHelper;
import com.aviconics.petrobot.petrobotbody.util.DeviceUtil;
import com.aviconics.petrobot.petrobotbody.util.DialogControl;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;
import com.badoo.mobile.util.WeakHandler;

/**
 * Created by futao on 2017/9/7.
 */

public abstract class BaseACActivity extends BaseActivity implements ACConnectChangeListener, ACMsgHandler {

    private static final String TAG = "BaseACActivity";
    private ACNetworkChangeReceiver receiver;
    private boolean isInitAC = false;
    public SignDevice mSign = SignDevice.getSign();
    public SpManager mSpManager = SpManager.getInstance();
    private WeakHandler mHandler;

    public static final int NO_BIND = 201;
    public static final int MEDIA_SYNC = 202;
    private DelayThread delayThread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String macAddress = DeviceUtil.getMacAddress();
        while (macAddress == null) { //保证获取到mac
            SystemClock.sleep(100);
            macAddress = DeviceUtil.getMacAddress();
        }

        if (!isInitAC) {
            AC.init(App.getContext(), macAddress);
            LogUtil.d("NICK", "------AC init----------mac:" + macAddress);
            isInitAC = true;
            //OtaUpdateHelper.getInstance(App.getContext()).startCheckUpadte(); //检查更新
        }
        AC.setConnectListener(this);
        AC.handleMsg(this);

        //registerACRceiver();//AC SDK 配置广播，动态注册
        mHandler = new WeakHandler();

        if (!mSign.isConnectAc()) {
            EventBusUtil.sendEvent(new Event(EventCode.UI,new UIEvent(DialogControl.Type.CONNECTING)));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //EMChatManager.getInstance().logout();
        //AC.DeviceSleep();
       //unregisterReceiver(receiver);
        LogUtil.d("NICK", "调用了BaseActivity 的 AC、环信 主动下线--的接口--------");
    }

    @Override
    public void connect() {
        LogUtil.i(TAG, "mainactivity,connect");
        mSign.setConnectAc(true);
        doSthConn();
    }

    @Override
    public void disconnect() {
        LogUtil.i(TAG, "mainactivity,disconnect");
        mSign.setConnectAc(false);
        doSthDisConn();
    }


    @Override
    public void handleMsg(ACDeviceMsg acDeviceMsg, ACDeviceMsg acDeviceMsg1) {

    }

    private void registerACReceiver() {
        receiver = new ACNetworkChangeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(receiver, filter);//AC状态广播
    }

    private void doSthConn() {
        //连云之后，注册、登陆环信
        if (!SpManager.getInstance().getRegisterEmNumber()) {
            DemoHelper.getInstance().registerEM(DeviceUtil.getMacAddress().toLowerCase());
        } else {
            if ((!DemoHelper.getInstance().isLoginIn())) {
                DemoHelper.getInstance().loginEM(DeviceUtil.getMacAddress().toLowerCase());
            }
        }

        reportState();
        //ControlUtil.getInstance().ledSeting(controlboardcom.LED_BLUE_BREATHE);//待机，蓝灯呼吸
        //startService(new Intent(this, ProWifiService.class));//wifi保护
//        if (mSign.isShowConnInMain()) {
//            mSign.setShowConnInMain(false);
            EventBusUtil.sendEvent(new Event(EventCode.UI, new UIEvent(DialogControl.Type.CONN_OK)));
//        }

        String appPhoneNum = SpManager.getInstance().getAppNumber();
        SpManager.getInstance().setDevWifiState(ConstantValue.WIFI_CONN);
        if (!TextUtils.isEmpty(appPhoneNum)) {
            if (!SpManager.getInstance().getBindState()) {
                LogUtil.i("NICK", "发送广播：" + appPhoneNum);
                AC.DeviceStartBc(DeviceUtil.getMacAddress() + "$" + appPhoneNum);

                confirmBindState();
            } else { //已绑定状态，扫码之后，发送广播
                if (SignDevice.getSign().isSendBc()) {
                    LogUtil.i("NICK", "发送广播：" + appPhoneNum);
                    AC.DeviceStartBc(DeviceUtil.getMacAddress() + "$" + appPhoneNum);
                    SignDevice.getSign().setSendBc(false);
                }
            }
        }
    }

    private void reportState() {
        if (mSign.isShowConnInMain()) {
            ReportHelper.reportAccount();
            ReportHelper.deviceState();
        }

        IssuedHelper.getInstance().setDeviceInUsing(false);//置回使用中标记

       /* if (SignDevice.getSign().isPlayMusic())
            sendBroadcast(new Intent().setAction(MusicPlayerService.STOP_MUSIC_ACTION));//关闭播放
        if (VideoPlayActivity.getVideoPlay() != null) {
            sendBroadcast(new Intent().setAction(VideoPlayActivity.VIDEO_NORMAL_STOP_ACTION));
        }
        SignDevice.getSign().setMediaState(0);
        EventBus.getDefault().post(new StateReportEvent(MUSIC_STOP_TYPE, ""));//初始化音乐播放状态


        EventBus.getDefault().post(new SyncBoxEvent(true));//同步食盒状态

        ReportHelper.reachLimit("normal");//初始化pet限位状态
        ReportHelper.musicModeState(SharedPrefs.getString(RobotApp.getContext(), "music_play_mode", "cycle"));//音乐播放模式);//初始化pet限位状态

        //EventBus.getDefault().post(new SyncDevStaEvent(SEND_RECORD_VIDEO_LIST)); //全量统计本地视频列表
        EventBus.getDefault().post(new SyncDevStaEvent(SEND_MEDIA_LIST)); //同步云、本地 media列表

        FeedOrderHelper.getInstance(MainActivity.this);//初始化系统喂食口令*/
    }

    private void confirmBindState() {
        mSpManager.setBindState(true);
        //加确认 1分钟之后去查云端绑定是否ok
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AC.bindMgr().isDeviceBound(new PayloadCallback<Boolean>() {
                    @Override
                    public void success(Boolean b) {
                        LogUtil.e("NICK", "绑定状态：" + b);
                        if (b) {
                            //设备已被绑定
                            mSpManager.setBindState(true);
                        } else {
                            //设备未绑定
                            mSpManager.setBindState(false);
                            //PetDeviceManager.rebootApp(this);
                        }
                    }

                    @Override
                    public void error(ACException e) {
                        //一般情况下为网络问题，如请求超时。
                        LogUtil.e("NICK", "绑定手机失败(网络请求超时)");

                        EventBusUtil.sendEvent(new Event(EventCode.UI, new UIEvent(DialogControl.Type.WIFI_DIS)));
                        EventBusUtil.sendEvent(new Event(EventCode.BIND, new BindEvent(NO_BIND)));
                    }
                });
            }
        }, 60 * 1000);
    }


    private void doSthDisConn() {
        //检查ac 是否重连重连
        if (delayThread == null) {
            delayThread = new DelayThread();
            delayThread.start();
        }
    }


    class DelayThread extends Thread {
        @Override
        public void run() {
            int count = 0;
            while (!mSign.isConnectAc()) {
                if (mSign.isCallByUser() && count > 5) {
                   /* EventBusUtil.sendEvent(new Event(EventCode.CALL,
                            new CallEvent(VideoCallActivity.VIDEO_CALL_END)));*/
                    break;
                }
                SystemClock.sleep(1 * 1000);
                count++;
            }
            delayThread = null;
        }
    }


}
