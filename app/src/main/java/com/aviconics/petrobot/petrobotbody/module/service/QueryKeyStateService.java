//package com.aviconics.petrobot.petrobotbody.module.service;
//
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Message;
//import android.os.SystemClock;
//
//import com.accloud.utils.LogUtil;
//import com.aviconics.petrobot.petrobotbody.net.ReportHelper;
//import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;
//import com.aviconics.petrobot.petrobotbody.util.SignDevice;
//
//import org.greenrobot.eventbus.EventBus;
//
//import cn.mindpush.petrobot.controlboardcom.controlboardcom;
//
///**
// * Created by win7 on 2016/9/14.
// */
//public class QueryKeyStateService extends Service {
//
//    private static final int LIMIT = 11;
//    public static String queryServiceName = "com.aviconics.petrobot.petrobotbody.service.QueryKeyStateService";
//    public static boolean envirLightState = false; //环境补光灯的状态
//    public static final String FEED_ACTION = "com.aviconics.petrobot.petrobotbody.service.QueryKeyStateService.feed"; //喂食指令广播
//
//    private Handler handler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            super.handleMessage(msg);
//            if (msg.what == LIMIT) {
//                ReportHelper.reachLimit("normal");
//            }
//        }
//    };
//    private boolean query = true;
//    private QueryThread queryThread;
//    private boolean isPress;
//    private boolean isFeeding = false;
//    private int count;
//    private FeedReceiver feedReceiver;
//    private int curBoxStatus;
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        LogUtil.d("NICK", "开始检测按键状态...");
//
//        regFeedReceiver();
//        EventBus.getDefault().register(this);
//    }
//
//    private void regFeedReceiver() {
//        feedReceiver = new FeedReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(FEED_ACTION);
//        registerReceiver(feedReceiver, filter);
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (queryThread == null) {
//            isPress = false;
//            queryThread = new QueryThread();
//            queryThread.start();
//        }
//
//        return START_REDELIVER_INTENT;
//    }
//
//
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        query = false;
//
//        unregisterReceiver(feedReceiver);
//        EventBus.getDefault().unregister(this);
//    }
//
//    private class QueryThread extends Thread {
//        private long startPressTime;
//        private long endPressTime;
//        private int lastBoxStatus = -1;
//
//        private int mKeyAndLightState;
//        private int conBoardState;
//        private long countPressTimes;
//        private boolean isReset;
//
//        @Override
//        public void run() {
////            //轮询3次下发查询指令
////            for (int i = 0; i < 3; i++) {
////                ControlBoardUtils.getInstance().lunch_box();
////            }
//
//            while (query) {
//                SystemClock.sleep(100);
//
//                //每100ms 查下环境灯和按键状态
//                mKeyAndLightState = ControlBoardUtils.getInstance().que_key_and_envlight();
//                //复位键状态
//                if ((mKeyAndLightState & 0x01) == 0x01) { //无按键
//                    countPressTimes = 0;
//                    if (isPress) { //按下 -》未按
//                        isPress = false;
//                        endPressTime = System.currentTimeMillis();
//                        LogUtil.d("NICK", "抬起时间：" + endPressTime);
//                        long time = endPressTime - startPressTime;
//                        LogUtil.d("NICK", "按键时间：" + time);
//                        if (time <= 3000 && time > 300) { //短按 0.3 ~ 3 秒
//                        } else if (time >= 10 * 1000) { //长按  > 10秒
//                            if (!isReset) {
//                                PetDeviceManager.reset(QueryKeyStateService.this);
//                                isReset = true;
//                            }
//                        } else if (time > 3 * 1000 && time < 10 * 1000) { //3 ~ 10秒
//                            PetDeviceManager.updateWifi(QueryKeyStateService.this);
//                            LogUtil.i("NICK", "---------接收到更新wifi的信号--------");
//                        }
//                    }
//                } else { //按键
//                    if (!isPress) { //未按 -》 按下
//                        isPress = true;
//                        startPressTime = System.currentTimeMillis();
//                        LogUtil.i("NICK", "按下时间：" + startPressTime);
//                    } else {//一直都是按下的状态
//                        countPressTimes++;
//                        if (countPressTimes > 100 && !isReset) {
//                            //按下状态超过10秒
//                            isReset = true;
//                            LogUtil.i("NICK", "---------按下10s，开始复位--------");
//                            PetDeviceManager.reset(QueryKeyStateService.this);
//                        }
//                    }
//                }
//
//                //环境灯状态
//                if ((mKeyAndLightState & 0x100) == 0x100) {
//                    if (!envirLightState) {
//                        LogUtil.d("NICK", "补光灯-关--- -暗--> 亮");
//                        ControlBoardUtils.getInstance().envlight_seting(controlboardcom.EXTERN_ENVLIGHT_DIS);
//                    }
//                    envirLightState = true;
//                } else {
//                    if (envirLightState) {
//                        LogUtil.d("NICK", "补光灯-开-- 亮----> 暗");
//                        ControlBoardUtils.getInstance().envlight_seting(controlboardcom.EXTERN_ENVLIGHT_EN);
//                    }
//                    envirLightState = false;
//                }
//
//
//                //查询电机限位 \食盒状态 \喂食成功失败状态 \ 单片机重启状态调控呼吸灯
//                conBoardState = ControlBoardUtils.getInstance().que_control_and_boardstatus();
//
//                //食盒状态
//                curBoxStatus = (conBoardState & 0x0010) == controlboardcom.LUNCH_BOX_STATUS ? 3 : 1;
//                if (lastBoxStatus != curBoxStatus) {
//                    //食盒状态改变
//                    if (isConnCloud()) { //连接状态同步上报
//                        ReportHelper.food(curBoxStatus);
//                        SharedPrefs.putInt(getApplicationContext(), "boxStatus", curBoxStatus);
//                    }
//                }
//                lastBoxStatus = curBoxStatus;
//
//
//                if (isConnCloud()) {//连上云端---绑定 才更新 云端的状态
//                    // 限位
//                    if ((conBoardState & 0x0f00) == controlboardcom.MOTOR_LEFT_LIMIT) {
//                        ReportHelper.reachLimit("leftLimit");
//                        handler.sendEmptyMessageDelayed(LIMIT, 5000);
//                    } else if ((conBoardState & 0x0f00) == controlboardcom.MOTOR_RIGHT_LIMIT) {
//                        ReportHelper.reachLimit("rightLimit");
//                        handler.sendEmptyMessageDelayed(LIMIT, 5000);
//                    }
//
//                    //喂食状态 feeding 不等于0 为成功
//                    if (isFeeding) {
//                        if ((conBoardState & 0x0080) == controlboardcom.FEEDING_STATUS) {
//                            ReportHelper.hasFeeded("success");
//                            isFeeding = false;
//                        } else {
//                            count++;
//                            if (count > 80) { //从收到喂食广播8s内，未获取到喂食成功的状态上报喂食失败
//                                ReportHelper.hasFeeded("failed");
//                                isFeeding = false;
//                            }
//                        }
//                    }
//
//                    // //食盒盖是否在动
//                    // if ((conBoardState &  0x0040) == controlboardcom.LID_IS_LOOSE)
//                    //     EventBus.getDefault().post(new StateReportEvent(MainActivity.LID_LOOSE_TYPE,""));
//
//                }
//
//
//                //单片机重启呼吸灯
//                if ((conBoardState & 0x0008) == controlboardcom.BOARD_RST_STATUS) {
//                    LogUtil.e("demo", "board resting");
//                    ControlBoardUtils.getInstance().led_seting(SignDevice.getSign().isConnectAc() ?
//                            controlboardcom.LED_BLUE_BREATHE : controlboardcom.LED_BLUE_FLICKER);
//
//                } else {
//                    //LogUtil.e("demo", "board runing");
//                }
//
//                //食盒盖 开关状态
//                if ((conBoardState & 0x0020) == controlboardcom.LID_LOOSE_STAUS) {
//                    if (!SignDevice.getSign().isLidOpen()) {
//                        EventBus.getDefault().post(new UsbAndLidEvent(LID_OPEN));
//                        SignDevice.getSign().setLidOpen(true);
//                    }
//                } else
//                    SignDevice.getSign().setLidOpen(false);
//            }
//            queryThread = null;
//        }
//    }
//
//
//    private class FeedReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (!isFeeding) {
//                LogUtil.i("NICK", "收到广播,下发喂食指令");
//                ControlBoardUtils.getInstance().feeding();//调用底层弹食
//                count = 0;//计数归0
//                isFeeding = true;
//            }
//        }
//    }
//
//    private boolean isConnCloud() {
//        return RobotApp.getSpManger().getBindAppState() && SignDevice.getSign().isConnectAc();
//    }
//
//    //显示 uri资源
//    public void onEventMainThread(SyncBoxEvent event) {
//        boolean sync = event.isSync();
//        LogUtil.d("NICK", "main接收到SyncBoxEvent消息" + sync);
//
//        if (sync) {
//            LogUtil.d("NICK","--start-------lunch_box--------");
//            int foodStatus = ControlBoardUtils.getInstance().lunch_box();
//            LogUtil.d("NICK", "--end-------lunch_box--------" + foodStatus);
//            ReportHelper.food(foodStatus);
//            SharedPrefs.putInt(RobotApp.getContext(), "boxStatus", foodStatus);
//
////            if (curBoxStatus == 3 || curBoxStatus ==1) {
////                ReportHelper.food(curBoxStatus);
////                SharedPrefs.putInt(RobotApp.getContext(), "boxStatus", curBoxStatus);
////            }
//        }
//    }
//
//}
