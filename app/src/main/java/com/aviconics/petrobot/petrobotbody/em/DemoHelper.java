package com.aviconics.petrobot.petrobotbody.em;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.text.TextUtils;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.module.receiver.CallReceiver;
import com.aviconics.petrobot.petrobotbody.module.receiver.ReConnReceiver;
import com.aviconics.petrobot.petrobotbody.util.Pop;
import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;
import com.aviconics.petrobot.petrobotbody.util.ThreadUtil;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMConnectionListener;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMCallOptions;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMOptions;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import java.util.Iterator;
import java.util.List;

public class DemoHelper {

    private static DemoHelper instance = null;
    private static String TAG = "EM_DemoHelper";
    public boolean isVideoCalling;
    private static boolean isEmRegister;

    private Context appContext;
    private boolean isDisConnEM;
    private ReConnEaseThread mEaseThread;

    private boolean sdkInited = false;

    private int MIN_BIT_RATE = 1500;//最小比特率
    private int MAX_BIT_RATE = 10000;//最大比特率
    private int MAX_FRAME_RATE = -1;//帧率
    private int AUDIO_SAMPLE_RATE = -1;//
    private String BACK_RESOLUTION = "1280x720";
    private String FRONT_RESOLUTION = "640x480";
    private boolean ENABLE_FIX_SAMPLE_RATE = false;
    private boolean IS_SEND_PUSH_IF_OFFLINE = false;

    private DemoHelper() {
    }

    public synchronized static DemoHelper getInstance() {
        if (instance == null) {
            instance = new DemoHelper();
        }
        return instance;
    }

    /**
     * init helper
     *
     * @param context application context
     */
    public void init(Context context) {
        appContext = context;
        EMOptions options = initChatOptions();
        if (initEMSdk(context, options)) {
            //debug mode, you'd better set it to false, if you want release your App officially.
            EMClient.getInstance().setDebugMode(false);

            //set Call options
            setCallOptions();
        }
    }

    public boolean isEmRegister() {
        return isEmRegister;
    }
    public boolean isVideoCalling() {
        return isVideoCalling;
    }

    private void setCallOptions() {
        // min video kbps
        if (MIN_BIT_RATE != -1) {
            EMClient.getInstance().callManager().getCallOptions().setMinVideoKbps(MIN_BIT_RATE);
        }

        // max video kbps
        if (MAX_BIT_RATE != -1) {
            EMClient.getInstance().callManager().getCallOptions().setMaxVideoKbps(MIN_BIT_RATE);
        }

        // max frame rate
        if (MAX_FRAME_RATE != -1) {
            EMClient.getInstance().callManager().getCallOptions().setMaxVideoFrameRate(MAX_FRAME_RATE);
        }

        // audio sample rate
        if (AUDIO_SAMPLE_RATE != -1) {
            EMClient.getInstance().callManager().getCallOptions().setAudioSampleRate(AUDIO_SAMPLE_RATE);
        }

        // resolution
        String[] wh = BACK_RESOLUTION.split("x");
        if (wh.length == 2) {
            try {
                EMClient.getInstance().callManager().getCallOptions()
                        .setVideoResolution(new Integer(wh[0]).intValue(), new Integer(wh[1]).intValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //*** 解决声音像怪兽的bug
        EMClient.getInstance().callManager().getCallOptions().setAudioSampleRate(16000);
        //录像 mov
        EMClient.getInstance().callManager().getVideoCallHelper().setPreferMovFormatEnable(true);

        // enabled fixed sample rate
        EMClient.getInstance().callManager().getCallOptions().enableFixedVideoResolution(ENABLE_FIX_SAMPLE_RATE);

        // Offline call push
        EMClient.getInstance().callManager().getCallOptions().setIsSendPushIfOffline(IS_SEND_PUSH_IF_OFFLINE);

        setEMConnectListener(new EMConnListener());

        // register callreceicer
        IntentFilter callFilter = new IntentFilter(EMClient.getInstance().callManager().getIncomingCallBroadcastAction());
        CallReceiver callReceiver = new CallReceiver();
        appContext.registerReceiver(callReceiver, callFilter);

        // register 断线15s重连广播
        ReConnReceiver reConnReceiver = new ReConnReceiver();
        IntentFilter reConnFilter = new IntentFilter("com.aviconics.petrobot.reconn.em");
        appContext.registerReceiver(reConnReceiver, reConnFilter);
    }


    /**
     * boolean true if caller can continue to call SDK related APIs after calling onInit, otherwise false.
     *
     * @param context
     * @param options use default if options is null
     * @return
     */
    public synchronized boolean initEMSdk(Context context, EMOptions options) {
        if (sdkInited) {
            return true;
        }
        int pid = android.os.Process.myPid();
        String processAppName = getAppName(pid);

        EMLog.d("DemoHelper", "process app name : " + processAppName);

        // if there is application has remote service, application:onCreate() maybe called twice
        // this check is to make sure SDK will initialized only once
        // return if process name is not application's name since the package name is the default process name
        if (processAppName == null || !processAppName.equalsIgnoreCase(appContext.getPackageName())) {
            EMLog.e("DemoHelper", "enter the service process!");
            return false;
        }
        EMClient.getInstance().init(context, options);
        sdkInited = true;
        return true;
    }

    /**
     * check the application process name if process name is not qualified, then we think it is a service process and we will not init SDK
     *
     * @param pID
     * @return
     */
    private String getAppName(int pID) {
        String processName = null;
        ActivityManager am = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        List l = am.getRunningAppProcesses();
        Iterator i = l.iterator();
        PackageManager pm = appContext.getPackageManager();
        while (i.hasNext()) {
            ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (i.next());
            try {
                if (info.pid == pID) {
                    CharSequence c = pm.getApplicationLabel(pm.getApplicationInfo(info.processName, PackageManager.GET_META_DATA));
                    // Log.d("Process", "Id: "+ info.pid +" ProcessName: "+
                    // info.processName +"  Label: "+c.toString());
                    // processName = c.toString();
                    processName = info.processName;
                    return processName;
                }
            } catch (Exception e) {
                // Log.d("Process", "Error>> :"+ e.toString());
            }
        }
        return processName;
    }


    protected EMOptions initChatOptions() {
        EMLog.d("DemoHelper", "init HuanXin Options");
        EMOptions options = new EMOptions();
        // change to need confirm contact invitation
        options.setAcceptInvitationAlways(false);
        // set if need read ack
        options.setRequireAck(true);
        // set if need delivery ack
        options.setRequireDeliveryAck(false);

        return options;
    }

    public void setEMConnectListener(EMConnectionListener listener) {
        EMClient.getInstance().addConnectionListener(listener);
    }


    public boolean isLoginIn() {
        return EMClient.getInstance().isLoggedInBefore();
    }

    public boolean isConnEmServer() {
        return EMClient.getInstance().isConnected();
    }

    /**
     * 注册
     */
    public void registerEM(String macId) {
        final String macAddress = macId;
        if (TextUtils.isEmpty(macAddress)) {
            return;
        }

        ThreadUtil.runInThread(new RegisterRunnable(macId));
    }

    //登陆
    public void loginEM(final String macAddress) {
        if (TextUtils.isEmpty(macAddress)) {
            return;
        }

        ThreadUtil.runInUIThread(new LoginRunnable(macAddress));
    }


    private class EMConnListener implements EMConnectionListener {

        @Override
        public void onConnected() {
            isDisConnEM = false;
        }

        @Override
        public void onDisconnected(int error) {
            isDisConnEM = true;
            if (error == EMError.USER_REMOVED) {
                LogUtil.e("NICK","环信-----USER_REMOVED-----");
            } else if (error == EMError.USER_LOGIN_ANOTHER_DEVICE) {
                LogUtil.e("NICK","环信-----USER_LOGIN_ANOTHER_DEVICE-----");
            } else if (error == EMError.SERVER_SERVICE_RESTRICTED) {
                LogUtil.e("NICK","环信连不上服务器");
                Pop.popToast(appContext, "环信服务器断开连接...");

                if (mEaseThread == null) {
                    mEaseThread = new ReConnEaseThread();
                    mEaseThread.start();
                }
            }
        }
    }

     private static class RegisterRunnable implements Runnable {
        private String name;


        public RegisterRunnable(String mac) {
            this.name = mac;
        }

        public void run() {
            isEmRegister = true;
            try {
                // 调用sdk注册方法
                EMClient.getInstance().createAccount(name, name);
                LogUtil.d(TAG,"环信账号注册成功,正在登陆");
            } catch (final HyphenateException e) {
                int errorCode = e.getErrorCode();
                if (errorCode == EMError.NETWORK_ERROR
                        || errorCode == EMError.USER_AUTHENTICATION_FAILED
                        || errorCode == EMError.USER_ILLEGAL_ARGUMENT) {
                    //Log.d("NICK", "网络异常、注册失败：没有权限、用户名不合法");
                    isEmRegister = false;
                } else if (errorCode == EMError.USER_ALREADY_EXIST) {
                    //Log.d("NICK", "用户已经存在直接登陆");
                    LogUtil.d(TAG,"用户已经存在直接登陆");
                    //isEmRegister = true;
                } else {
                    //isEmRegister = true;
                }
            }
            if (isEmRegister) {
                ThreadUtil.runInUIThread(new LoginRunnable(name));
            }
        }
    }


    private static class LoginRunnable implements Runnable {

        private String name;

        public LoginRunnable(String mac) {
            this.name = mac;
        }

        @Override
        public void run() {
            // 调用sdk登陆方法登陆聊天服务器
            EMClient.getInstance().login(name, name, new EMCallBack() {

                @Override
                public void onSuccess() {
                    LogUtil.d(TAG,"环信登陆成功");
                    Pop.showSafe("环信登陆成功");
                }

                @Override
                public void onProgress(int progress, String status) {
                    LogUtil.d(TAG,status);
                }

                @Override
                public void onError(final int code, final String message) {
                    LogUtil.d(TAG,"环信登陆onError");
                    Pop.showSafe("环信登陆onError");
                }
            });
        }
    }




    /**
     * 设置环信视频通话分辨率
     *
     * @param quality
     */
    public void setVideocallQuality(String quality) {
        if (SignDevice.getSign().isCallByUser()) {
            return;
        }
        EMCallOptions callOptions = EMClient.getInstance().callManager().getCallOptions();
        switch (quality) {
            case "auto":
                // if (!SharedPrefs.getBoolean(context, EMConstant.VIDEO_CALL_RESOLUTION, false)) {
                callOptions.enableFixedVideoResolution(true);
                //SharedPrefs.putBoolean(context, EMConstant.VIDEO_CALL_RESOLUTION, true);
                //}
                break;
            case "480p":
                callOptions.setVideoResolution(640, 480);
                callOptions.enableFixedVideoResolution(true);
                EMClient.getInstance().callManager().getCallOptions().setMaxVideoKbps(800);
                EMClient.getInstance().callManager().getCallOptions().setMinVideoKbps(80);
                break;
            case "720p":
                callOptions.setVideoResolution(1280, 720);
                callOptions.enableFixedVideoResolution(true);
                EMClient.getInstance().callManager().getCallOptions().setMaxVideoKbps(10000);
                EMClient.getInstance().callManager().getCallOptions().setMinVideoKbps(1500);

                break;
            default:
                callOptions.enableFixedVideoResolution(true);
                SharedPrefs.putBoolean(appContext, EMConstant.VIDEO_CALL_RESOLUTION, true);
                break;
        }
    }


    private class ReConnEaseThread extends Thread {

        @Override
        public void run() {
            int count = 0;
            while (isDisConnEM) {
                if (count > 15 ) {
                    appContext.sendBroadcast(new Intent("com.aviconics.petrobot.reconn.em"));
                    break;
                }
                SystemClock.sleep(1000);
                count ++;
            }
            mEaseThread = null;
        }
    }


    private boolean isCallArrive;//标记通话 开始-断线 状态

    public boolean isCallArrive() {
        return isCallArrive;
    }


    private EMCallStateChangeListener callStateListener = new EMCallStateChangeListener() {
        @Override
        public void onCallStateChanged(CallState callState, final CallError error) {
            LogUtil.d("NICK:-----", callState.toString() + "-----");
            switch (callState) {
                case CONNECTING: // is connecting
                    LogUtil.i("NICK", "正在连接对方...");
                    break;

                case ANSWERING: // answering
                    LogUtil.i("NICK", "正在接听对方...");
               //     EventBus.getDefault().post(new CallEvent(VideoCallActivity.VIDEO_START_MONITOR));
                    break;
                case CONNECTED: // connected
                    LogUtil.i("NICK", "已经和对方建立连接，等待对方接受...");
                    break;
                case NETWORK_DISCONNECTED:
                    LogUtil.i("NICK", "网络连接不可用，请检查网络.");
                    break;
                case NETWORK_UNSTABLE:
                    if (error == CallError.ERROR_NO_DATA) {
                        LogUtil.i("NICK", "没有通话数据");
                    } else {
                        LogUtil.i("NICK", "网络不稳定");
                    }
                    break;
                case NETWORK_NORMAL:
                    LogUtil.i("NICK", "通话状态正常...");
                    break;
                case VIDEO_PAUSE:
                    LogUtil.i("NICK", "视频数据暂停...");
                    break;
                case VIDEO_RESUME:
                    LogUtil.i("NICK", "视频数据恢复...");
                    break;
                case VOICE_PAUSE:
                    LogUtil.i("NICK", "音频数据暂停...");
                    break;
                case VOICE_RESUME:
                    LogUtil.i("NICK", "音频数据恢复...");
                    break;
                case ACCEPTED: // call is accepted
                    LogUtil.i("NICK", "通话中...");//拨号被接通
                 //   EventBus.getDefault().post(new CallEvent(VideoCallActivity.VIDEO_ACCEPTED));
                    break;
                case DISCONNECTED: // call is disconnected
                    LogUtil.i("NICK", "环信连接已断开...");
                    isCallArrive = false;
                    EMClient.getInstance().callManager().removeCallStateChangeListener(this);
                  //  EventBus.getDefault().post(new CallEvent(VideoCallActivity.VIDEO_CALL_MIS_CONNECT));
                    break;
                default:
                    break;
            }
        }
    };
    /**
     * set call state listener
     */
    public void addCallStateListener() {
        isCallArrive = true;
        EMClient.getInstance().callManager().addCallStateChangeListener(callStateListener);
    }

    public void removeCallStateListener() {
        EMClient.getInstance().callManager().removeCallStateChangeListener(callStateListener);
    }

}
