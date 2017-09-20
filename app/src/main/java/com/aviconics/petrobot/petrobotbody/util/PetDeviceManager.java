//package com.aviconics.petrobot.petrobotbody.util;
//
//import android.app.AlarmManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.os.PowerManager;
//import android.os.SystemClock;
//
//import com.accloud.clientservice.AC;
//import com.accloud.utils.LogUtil;
//import com.aviconics.petrobot.petrobotbody.activity.CaptureActivity;
//import com.aviconics.petrobot.petrobotbody.activity.SplashActivity;
//import com.aviconics.petrobot.petrobotbody.activity.VideoCallActivity;
//import com.aviconics.petrobot.petrobotbody.config.ConstantValue;
//import com.aviconics.petrobot.petrobotbody.index.RobotApp;
//import com.aviconics.petrobot.petrobotbody.net.ReportHelper;
//import com.aviconics.petrobot.petrobotbody.service.MusicPlayerService;
//
//import java.io.File;
//
//import cn.mindpush.petrobot.controlboardcom.ControlBoardUtils;
//
///**
// * 设备管理
// * 解绑-重启-复位
// */
//public class PetDeviceManager {
//
//    public static void updateWifi(Context context) {
//        CameraMonitorHelper.closeMonitor(context);
//        if (VideoCallActivity.getVideoCall() != null) {
//            VideoCallActivity.getVideoCall().finish();
//        }
//        if (SignDevice.getSign().isPlayMusic()) {
//            context.sendBroadcast(new Intent().setAction(MusicPlayerService.STOP_MUSIC_SERVICE));
//        }
//
//       /* if (AppManager.getAppManager().isServiceRunning(context, ProWifiService.proWifiServiceName)) {
//            context.stopService(new Intent(context, ProWifiService.class));
//        }*/
//
//        //断开当前连接wifi，并关闭wifi
//        RobotApp.getSpManger().setDevWifiState(ConstantValue.WIFI_FORGET);
//        RobotApp.getWifiAdmin().removeAllWifi();
//        SystemClock.sleep(1000);
//
//        CaptureActivity.startToCapture(context, true, false);
//    }
//
//
//    /**
//     * 重启
//     *
//     * @param context
//     */
//    public static void rebot(Context context) {
//        LogUtil.i("NICK", "--------重启--------");
//        ControlBoardUtils.getInstance().control_reset();//通知底层复位
//        SystemClock.sleep(500);
//
//        PowerManager pManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        pManager.reboot("");
//    }
//
//
//    /**
//     * 复位
//     *
//     * @param context
//     */
//    public static void reset(Context context) {
//        LogUtil.i("NICK", "--------复位--------");
//        if (SignDevice.getSign().isPlayMusic()) {
//            context.sendBroadcast(new Intent().setAction(MusicPlayerService.STOP_MUSIC_SERVICE));
//        }
//        ReportHelper.mediaVolume(5);
//        SystemClock.sleep(1000);
//        VolumeHelper.getInstance(context).setPetMusicVolume(5);
//
//        AC.deviceForceUnbind();//解绑app
//
//        RobotApp.getSpManger().setBindAppState(false);//标记复位
//        File orderFile = new File(ConstantValue.voice_sys_dir, ConstantValue.order_voice_name);
//        if (orderFile.exists())
//            orderFile.delete();//删除本地存储喂食录音
//
//        ControlBoardUtils.getInstance().control_reset();//通知底层复位
//        //删除sp 中数据
//        CleanDataManager.cleanApplicationData(context);
//        SharedPrefs.clearData(context);
//
//        //断开当前连接wifi，并关闭wifi
//        RobotApp.getWifiAdmin().removeAllWifi();
//        RobotApp.getSpManger().setDevWifiState(ConstantValue.WIFI_DIS);
//
//        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//        pm.reboot("");
//    }
//
//    private static void creatCrash() {
//        int[] arr = new int[]{1, 2};
//        int a = 0;
//        for (int i = 0; i <= arr.length; i++) {
//            a += arr[i];
//        }
//    }
//
//
//    public static void rebootApp(Context context) {
//        Intent intent = new Intent(context, SplashActivity.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);
//
//        android.os.Process.killProcess(android.os.Process.myPid());
//        System.exit(0);
//    }
//
//    /**
//     * 解绑
//     *
//     * @param context
//     */
//    public static void unbind(Context context) {
//        if (CaptureActivity.getCapture() != null) {
//            CaptureActivity.getCapture().finish();
//        }
//        if (VideoCallActivity.getVideoCall() != null) {
//            VideoCallActivity.getVideoCall().finish();
//        }
//        if (SignDevice.getSign().isPlayMusic()) {
//            context.sendBroadcast(new Intent().setAction(MusicPlayerService.STOP_MUSIC_SERVICE));
//        }
//
//        CameraMonitorHelper.closeMonitor(context);
//        CleanDataManager.cleanApplicationData(context);
//        SharedPrefs.clearData(context);
//        PetToService.sendUnbindMsg2Service();
//        SystemClock.sleep(500);
//
//        AC.deviceForceUnbind();
//        File orderFile = new File(ConstantValue.voice_sys_dir, ConstantValue.order_voice_name);
//        if (orderFile.exists())
//            orderFile.delete();//删除本地存储喂食录音
//
//        SystemClock.sleep(1000);
//        RobotApp.getWifiAdmin().removeAllWifi();
//        RobotApp.getSpManger().setDevWifiState(ConstantValue.WIFI_DIS);
//
//        ControlBoardUtils.getInstance().back_light(1);
//
//        //android.os.Process.killProcess(android.os.Process.myPid());
//        //System.exit(0);
//        restart(context);
//    }
//
//    private static void restart(Context context) {
//        Intent intent = new Intent(RobotApp.getContext(), SplashActivity.class);
//        PendingIntent restartIntent = PendingIntent.getActivity(
//                RobotApp.getContext(), 0, intent,
//                Intent.FLAG_ACTIVITY_NEW_TASK);
//        // 退出程序
//        AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, restartIntent); // 2秒钟后重启应用
//
//        AppManager.getAppManager().popAllActivityFromStack();
//        //杀死该应用进程
//        android.os.Process.killProcess(android.os.Process.myPid());
//    }
//
//}
