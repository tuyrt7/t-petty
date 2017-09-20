package com.aviconics.petrobot.petrobotbody.util;

import android.os.Handler;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.manager.SpManager;
import com.aviconics.petrobot.petrobotbody.module.service.SoundMonitorService;
import com.aviconics.petrobot.petrobotbody.module.ui.activity.MoveShowActivity;
import com.aviconics.petrobot.petrobotbody.module.ui.activity.RecorderActivity;
import com.aviconics.petrobot.petrobotbody.zxing.activity.CaptureActivity;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ServiceUtils;

/**
 * Created by win7 on 2016/9/13.
 */
public class CameraMonitorHelper {

    private static boolean flag = false;

    public static void openMonitor() {
        LogUtil.d("NICK", "----启动监听----");
        openMovement();
        openSoundMonitor();
    }

    public static boolean openMovement() {
        if (SignDevice.getSign().isCallByUser() || (!isBindApp()) || (!SignDevice.getSign().isConnectAc())) {
            return false;
        }
        if (SpManager.getInstance().getMovementState()) {
            if (
                   /* VideoCallActivity.getVideoCall() == null
                    && VideoPlayActivity.getVideoPlay() == null
                    &&*/ App.findActivity(RecorderActivity.class) == null
                    && App.findActivity(MoveShowActivity.class) == null
                    && App.findActivity(CaptureActivity.class) == null
                    && !SignDevice.getSign().isPlayMusic()
                    ) {
                ActivityUtils.startActivity(MoveShowActivity.class);
                return true;
            }
        }

        return false;
    }

    public static void openRecorder(Handler handler) {
        if (SignDevice.getSign().isCallByUser() || !isBindApp()) {
            return;
        }

        /*if (VideoCallActivity.getVideoCall() != null || CaptureActivity.getCapture() != null
                || VideoPlayActivity.getVideoPlay() != null) {
            return;
        }*/

        //停掉监控和录像
        if (ServiceUtils.isServiceRunning(SoundMonitorService.class.getName())) {
            ServiceUtils.stopService(SoundMonitorService.class);
        }

        if (App.findActivity(MoveShowActivity.class) != null) {
            App.finishActivity(MoveShowActivity.class);
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ActivityUtils.startActivity(RecorderActivity.class);
            }
        }, 3000);
    }

    public static boolean openSoundMonitor() {
        if (SignDevice.getSign().isCallByUser() || !isBindApp()) {
            return false;
        }

        if (SpManager.getInstance().getSoundState()) {
            if (/*VideoCallActivity.getVideoCall() == null
                    &&*/ App.findActivity(RecorderActivity.class) == null
                    && App.findActivity(CaptureActivity.class) == null
                    && !SignDevice.getSign().isPlayMusic()
                    ) {
                ServiceUtils.startService(SoundMonitorService.class);
                return true;
            }
        }
        return false;
    }

    /**
     * 停掉两个监控和录像
     */
    public static void closeMonitor() {
        closeMovement();
        closeSoundMonitor();
        if (App.findActivity(RecorderActivity.class) != null) {
            App.finishActivity(RecorderActivity.class);
            LogUtil.d("NICK", "----closeMonitor----mRecorder--");
        }
    }

    /**
     * 停掉运动检测
     */
    public static void closeMovement() {
        if (App.findActivity(MoveShowActivity.class) != null) {
            App.finishActivity(MoveShowActivity.class);
            LogUtil.d("NICK", "----closeMonitor----mMovement--");
        }
    }

    /**
     * 停掉声音检测
     */
    public static void closeSoundMonitor() {
        if (ServiceUtils.isServiceRunning(SoundMonitorService.class.getName())) {
            ServiceUtils.stopService(SoundMonitorService.class);
        }
    }


    public static boolean isBindApp() {
        return SpManager.getInstance().getBindState();
    }
}
