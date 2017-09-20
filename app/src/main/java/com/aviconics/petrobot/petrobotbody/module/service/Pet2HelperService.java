package com.aviconics.petrobot.petrobotbody.module.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import com.accloud.utils.LogUtil;

/**
 * 启动helper与 helper 应用通过间隔30s发一次广播，来确定petrobot 是否正常运行
 */
public class Pet2HelperService extends Service {

    public static final String pet2helperServiceName = "com.aviconics.petrobot.petrobotbody.service.Pet2HelperService";
    /**
     * helper广播过滤action
     */
    private final String MY_PET_ACTION = "cn.mindpush.helper.my_pet_action";

    private boolean flag;
    private SendBcThread sendBcThread;
    private Intent intent;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        flag = true;
        intent = new Intent(MY_PET_ACTION).addCategory(Intent.CATEGORY_DEFAULT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startHelper();
        sendBc2Helper();
        return START_STICKY;
    }

    private void sendBc2Helper() {
        if (sendBcThread == null) {
            sendBcThread = new SendBcThread();
            sendBcThread.start();
        }
    }

    private class SendBcThread extends Thread {

        SendBcThread() {
            setName("SendBc2Helper");
        }

        @Override
        public void run() {
            while (flag) {
                SystemClock.sleep(30 * 1000);
                sendBc();

                showUiWhenDisNet();

            }
        }
    }

    private void showUiWhenDisNet() {
     /*   if ((!SignDevice.getSign().isConnectAc()) && (!NetWorkUtils.ping())) {
            if (CaptureActivity.getCapture() == null && (!SignDevice.getSign().isCallByUser())) {
                EventBus.getDefault().post(new UIEvent(MainActivity.UI_CONN_SHOW));
            }
            SignDevice.getSign().setShowConnInMain(true);
        }*/
    }

    /**
     * 每隔30s发广播给helper确认是否petrobot运行
     */
    private void sendBc() {
        sendBroadcast(intent);
        LogUtil.d("NICK", "----sendBroadcast---to---helper-");
    }


    private void startHelper() {
        LogUtil.d("NICK", "-----------startHelper-----------------");
        Intent mIntent = new Intent("cn.mindpush.helper.helper_service");//Service能够匹配的Action
        mIntent.setPackage("cn.mindpush.helper");//应用的包名
        try {
            startService(mIntent);
        } catch (Exception e) {
            LogUtil.d("NICK", "------helper service is not start-------Exception----------");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        flag = false;
        sendBcThread = null;
        intent = null;
    }
}
