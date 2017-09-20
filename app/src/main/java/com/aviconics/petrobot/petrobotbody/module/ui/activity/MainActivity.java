package com.aviconics.petrobot.petrobotbody.module.ui.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.WindowManager;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.module.service.MusicPlayerService;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.GreenDaoManager;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.Event;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventCode;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.ReportEvent;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.UIEvent;
import com.aviconics.petrobot.petrobotbody.net.ReportHelper;
import com.aviconics.petrobot.petrobotbody.util.DialogControl;
import com.aviconics.petrobot.petrobotbody.util.MediaUtil;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;
import com.aviconics.petrobot.petrobotbody.util.ThreadUtil;

import java.util.List;

import static com.aviconics.petrobot.petrobotbody.configs.AcConfig.Type.LID_LOOSE_TYPE;
import static com.aviconics.petrobot.petrobotbody.configs.AcConfig.Type.MOVEMENT_TYPE;
import static com.aviconics.petrobot.petrobotbody.configs.AcConfig.Type.MUSIC_MODE_TYPE;
import static com.aviconics.petrobot.petrobotbody.configs.AcConfig.Type.MUSIC_PLAY_TYPE;
import static com.aviconics.petrobot.petrobotbody.configs.AcConfig.Type.MUSIC_STOP_TYPE;
import static com.aviconics.petrobot.petrobotbody.configs.AcConfig.Type.SOUND_TYPE;
import static com.aviconics.petrobot.petrobotbody.configs.AcConfig.Type.VIDEO_PLAY_TYPE;
import static com.aviconics.petrobot.petrobotbody.configs.AcConfig.Type.VIDEO_STOP_TYPE;


public class MainActivity extends BaseACActivity {

    private ContentResolver mContentResolver;
    private MonitorThread mMonitorThread;
    private int m;

    @Override
    protected void beforeInit() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    protected boolean isRegisterEventBus() {
        return true;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        DialogControl.getInstance().initDialog(MainActivity.this);
        //关闭休眠
        if (checkSysPermiss()) {
            unLock();
        }
    }

    @Override
    protected void initData() {

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

    }

    public boolean checkSysPermiss() {
        PackageManager pm = getPackageManager();
        boolean permission = (PackageManager.PERMISSION_GRANTED ==
                pm.checkPermission("android.permission.WRITE_SECURE_SETTINGS", getPackageName()));
        if (permission) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 休眠 --需要签名权限
     */
    public void unLock() {
        mContentResolver = getContentResolver();
        setLockPatternEnabled(android.provider.Settings.Secure.LOCK_PATTERN_ENABLED, false);
    }

    private void setLockPatternEnabled(String systemSettingKey, boolean enabled) {
        android.provider.Settings.Secure.putInt(mContentResolver, systemSettingKey, enabled ? 1 : 0);
    }

    public void closeBackLightAndStartMonitor() {

        m = 60;//启动监控计数
        if (mMonitorThread == null) {
            mMonitorThread = new MonitorThread();
            mMonitorThread.start();
        }
    }

    private void stopThread() {


        if (mMonitorThread != null) {
            try {
                mMonitorThread.interrupt();
            } catch (SecurityException e) {
            }
            mMonitorThread = null;
        }
    }


    /**
     * 1分后关背光
     */
    private class MonitorThread extends Thread {
        @Override
        public void run() {
            boolean isOpened = false;
            while (m >= 0) {
                SystemClock.sleep(5 * 1000);
                m = m - 5;

                //最后5s 0s 分别在连网的时候启动检测
                if (m <= 5 && (!isOpened) && SignDevice.getSign().isConnectAc()) {
                    //根据开关,运动检测 + 声音检测
                    //CameraMonitorHelper.openMonitor(MainActivity.this);
                    isOpened = true;
                }
            }
            mMonitorThread = null;
        }
    }


    @Override
    protected void receiveEvent(Event event) {
        LogUtil.d("NICK", "main接收到消息:" + event.getCode());
        // 接受到Event后的相关逻辑
        switch (event.getCode()) {
            case EventCode.UI:
                UIEvent ui = (UIEvent) event.getData();
                DialogControl.getInstance().showDevDialog(ui.getType());
                break;
            case EventCode.BIND:
                ThreadUtil.runInUIThread(new Runnable() {
                    @Override
                    public void run() {
                        //设备未绑定
                        mSpManager.setBindState(false);
                        //PetDeviceManager.rebootApp(MainActivity.this);
                    }
                }, 2500);
                break;
            case EventCode.MEDIA_LIST:
                if (SignDevice.getSign().isCompleteMediaData() && SignDevice.getSign().isConnectAc()) {
                    //全量统计同步本地media 到云
                    ThreadUtil.runInThread(new Runnable() {
                        @Override
                        public void run() {
                            final List<MediaFile> allMedia = GreenDaoManager.getInstance().getAllMedia();
                            // final List<MediaFile> allMedia = RobotApp.getUsbHelper().getAllMediaList();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    MediaUtil.sendMediaToCloud(allMedia);
                                    sendBroadcast(new Intent().setAction(MusicPlayerService.UPDATE_MUSIC_LIST_ACTION));
                                }
                            });
                        }
                    });
                }
                break;
            case EventCode.REPORT:
                ReportEvent report = (ReportEvent) event.getData();
                reportCloud(report);
                break;
            default:
                break;
        }
    }

    private void  reportCloud(ReportEvent report) {
        int type = report.getType();
        switch (type) {
            case SOUND_TYPE:
                ReportHelper.pushMsg(getApplicationContext(), "sound");
                break;
            case MOVEMENT_TYPE:
                ReportHelper.pushMsg(getApplicationContext(), "movement");
                break;
            case LID_LOOSE_TYPE:
                ReportHelper.pushMsg(getApplicationContext(), "openLid");
                break;
            case MUSIC_MODE_TYPE:
                ReportHelper.musicModeState(report.getMsg());
                break;
            case MUSIC_PLAY_TYPE:
                ReportHelper.musicState("play", report.getMsg());
                break;
            case MUSIC_STOP_TYPE:
                if (SignDevice.getSign().getMediaState() == 2)
                    break;//播视频 stop状态不上报
                ReportHelper.musicState("stoped", "");
                break;
            case VIDEO_PLAY_TYPE:
                ReportHelper.musicState("play", report.getMsg());
                break;
            case VIDEO_STOP_TYPE:
                if (SignDevice.getSign().getMediaState() == 1)
                    break; //播音乐 stop状态不上报
                ReportHelper.musicState("stoped", "");
                break;
            default:
                break;
        }
    }
}
