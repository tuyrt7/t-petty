package com.aviconics.petrobot.petrobotbody.net;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;

import com.accloud.common.ACDeviceMsg;
import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.configs.ConstantValue;
import com.aviconics.petrobot.petrobotbody.em.DemoHelper;
import com.aviconics.petrobot.petrobotbody.em.EMConstant;
import com.aviconics.petrobot.petrobotbody.manager.SpManager;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile;
import com.aviconics.petrobot.petrobotbody.util.CameraMonitorHelper;
import com.aviconics.petrobot.petrobotbody.util.DeviceUtil;
import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;
import com.aviconics.petrobot.petrobotbody.util.VolumeHelper;
import com.blankj.utilcode.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 下发数据的解析
 */
public class IssuedHelper {

    private Activity iContext;
    private int code = 0;
    private String oldReqJson = "";
    private long pass = 0;//指令之间间隔时间
    private int feedCountWhenBoxEmpty = 0;

//    private String system_voice_path = ConstantValue.voice_sys_dir + File.separator + ConstantValue.system_voice_name;
//    private String cusdom_order_path = ConstantValue.voice_sys_dir + File.separator + ConstantValue.order_voice_name;
    public static final String PET_VOLUME = "pet_volume";

    private IssuedHelper() {
    }

    private static class SingletonInstance {
        private static final IssuedHelper INSTANCE = new IssuedHelper();
    }

    public static IssuedHelper getInstance() {
        return SingletonInstance.INSTANCE;
    }

    /**
     * 环信拍照的标记
     */
    private boolean isTakePhoto = false;
    /**
     * 记录时间戳 yyyyMMddHHmmss
     */
    private String timeStamp = "20160101000000";
    /**
     * 设备正在被使用中
     */
    private boolean isDeviceInUsing = false;

    public boolean isDeviceInUsing() {
        return isDeviceInUsing;
    }

    public void setDeviceInUsing(boolean deviceInUsing) {
        isDeviceInUsing = deviceInUsing;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp() {
        timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    public boolean isTakePhoto() {
        return isTakePhoto;

    }

    public void setTakePhoto(boolean takePhoto) {
        isTakePhoto = takePhoto;
    }


    //JSON格式的json不需要设置msgCode
    public void reSolverMsg(final Activity context, final ACDeviceMsg reqMsg,
                            final ACDeviceMsg respMsg, Handler mainHandler) throws JSONException {
        iContext = context;
        final String reqJson = reqMsg.getJsonPayload();
        long cur = new Date().getTime();
        //code，云端控制或者直连控制
        LogUtil.i("NICK", "reqmsgcode: " + reqMsg.getMsgCode() + ",reqmsg: " + reqMsg.getJsonPayload());
        if (checkOrderWhenSame(reqJson, cur))
            return;//在1s内相同的指令抛弃

        //获取到下发数据的json对象
        JSONObject json = new JSONObject(reqJson);
        String jsonModule = json.getString("module");
        String jsonAction = json.getString("action");

        //设备转向、喂食时，其他指令不响应
        if (isDeviceInUsing && !"head".equals(jsonModule)) {
            handleJson(respMsg, -3, context.getString(R.string.device_isInUsing));
            return;
        }

        switch (jsonModule) {
            case "system"://系统设置
                if ("wifi".equals(jsonAction)) {
                    //                     <-- {70: {"module":"system", "type":"cmd", "action":"wifi"}}
                    //重置wifi,，重新扫码(保证指令的状态码能返回手机app)
                    handleJson(respMsg, 0);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                          //  PetDeviceManager.updateWifi(App.getContext());
                        }
                    }).start();

                } else if ("bindSuccess".equals(jsonAction)) { //废弃
                    //{70: {"module":"system", "type":"cmd", "action":"bindSuccess"}}
                    //已经绑定ac，上报食盒，注册网络状态变化广播，启动检测
                    // EventBus.getDefault().postSticky(new SyncDevStaEvent("bind"));
                    handleJson(respMsg, 0);
                } else if ("volume".equals(jsonAction)) {
                    // {70: {"module":"system", "type":"set", "action":"volume"}, "values":{"volume":3}}
                    int volume = (Integer) ((JSONObject) json.get("values")).get("volume");
                    SharedPrefs.putInt(context, PET_VOLUME, volume);
                    //给机器人设置音量 (0~10，表示百分比)
                    VolumeHelper.getInstance(context).setPetMusicVolume(volume);
                    if (SignDevice.getSign().isCallByUser())
                        VolumeHelper.getInstance(context).setCallVolume(volume);

                    //上报
                    ReportHelper.mediaVolume(volume);
                    handleJson(respMsg, 0);
                }   else if ("reboot".equals(jsonAction)) {
                    //<-- {70: {"module":"system", "type":"cmd", "action":"reboot"}}
                    //PetDeviceManager.rebot(context);
                    handleJson(respMsg, 0);

                }else if ("unbind".equals(jsonAction)) {
                    //{200: {"module":"system", "type":"cmd", "action":"unbind"}
                    new Thread() {
                        @Override
                        public void run() {
                         //   PetDeviceManager.unbind(context);
                        }
                    }.start();
                    //收到解绑指令
                    handleJson(respMsg, 0);//先返回0，再解绑

                } else if ("retryEasemob".equals(jsonAction)) {
                    //{70: {"module":"system", "type":"cmd", "action":"ryEasemob"}
                    //重新注册环信,注册成功就上报，并且登陆
                    if (!SpManager.getInstance().getRegisterEmNumber()) {
                        DemoHelper.getInstance().registerEM(DeviceUtil.getMacAddress().toLowerCase());
                    } else {
                        if ((!DemoHelper.getInstance().isLoginIn())) {
                            DemoHelper.getInstance().loginEM(DeviceUtil.getMacAddress().toLowerCase());
                        }
                    }
                    handleJson(respMsg, 0);
                }

                break;
            case "media"://media 相关
                if ("play".equals(jsonAction)) {
                    //<-- {70: {"module":"music", "type":"cmd", "action":"play", "values": {"url": "/sdcard/music.mp3", "type": 2}}}
                    //音乐播放 返回 {"code":0}
                    JSONObject musicJson = (JSONObject) json.get("values");
                    String url = (String) musicJson.get("url");
                    int type = (int) musicJson.get("type");

                    if (FileUtils.isFileExists(url)) {
                        if (type < 3) { //开音乐
                            handleJson(respMsg, 0);
                            SignDevice.getSign().setMediaState(1);

                            //TODO ？？ start

                           /* //关视频
                            if (SignDevice.getSign().isVideoPlay()) {
                                context.sendBroadcast(new Intent().setAction(VideoPlayActivity.VIDEO_STOP_TO_MUSIC_ACTION));
                            }

                            context.startService(new Intent(context, MusicPlayerService.class)
                                    .putExtra("MSG_COM", MusicPlayerService.PLAY)
                                    .putExtra("TYPE_COM", type)
                                    .putExtra("URL_COM", url)
                            );*/
                        } else { //看视频
                            if (SignDevice.getSign().isCallByUser() && !SignDevice.getSign().isSingleCall()) {
                                handleJson(respMsg, -1,context.getResources().getString(R.string.video_not_play_when_call));
                                break;//双向视频不响应
                            }
                          /*  handleJson(respMsg, 0);
                            SignDevice.getSign().setMediaState(2);

                            context.startActivity(new Intent(context, VideoPlayActivity.class)
                                    .putExtra(VideoPlayActivity.VIDEO_MSG, url));*/
                        }
                        //CameraMonitorHelper.closeMonitor(context);
                    } else {
                        String message = "";
                        if (App.getUsbHelper().isUsbEnable()) {
                            message = context.getResources().getString(R.string.no_url_music);
                            LogUtil.e("NICK", type == MediaFile.TYPE_MUSIC_USB ? "----外置音乐出错-----" : "----内置音乐出错-----");
                        } else {
                          /*  if (HintActivity.getHintAct() == null) {
                                context.startActivity(new Intent(context, HintActivity.class).putExtra("info", "no_usb"));
                            }*/
                            message = context.getResources().getString(R.string.usb_not_insert);
                        }
                        handleJson(respMsg, -1, message);
                    }

                } else if ("stop".equals(jsonAction)) {
                    //<-- {70: {"module":"music", "type":"cmd", "action":"stop"}}
                    SignDevice.getSign().setMediaState(0);

                    if (SignDevice.getSign().isPlayMusic()) {
                        //音乐停止 返回 {"code":0}
                       // context.startService(new Intent(context, MusicPlayerService.class).putExtra("MSG_COM", MusicPlayerService.STOP));
                    }

                    if (SignDevice.getSign().isVideoPlay()) {
                      //  context.sendBroadcast(new Intent().setAction(VideoPlayActivity.VIDEO_STOP_AND_KEEP_POS_ACTION));
                    }
                    handleJson(respMsg, 0);
                }
                break;
            case "music"://音乐相关
                //           {70: {"module":"music", "type":"cmd", "action":"play", "values": {"url": "/sdcard/music.mp3", "mode":"random","type": 2}}}
                if ("musicVolume".endsWith(jsonAction)) {
                    //查询音乐音量 {"module":"music", "type":"get", "action":"musicVolume"
                    int musicVolume = VolumeHelper.getInstance(context).getMusicVolume();//实际音量
                    int petVolume = SharedPrefs.getInt(context, PET_VOLUME, 5);//app设置
                    if (musicVolume != petVolume) {
                        musicVolume = petVolume;
                        //环信通话时，实际音量和app设置会不一致，手动调整
                        VolumeHelper.getInstance(context).setPetMusicVolume(petVolume);
                    }
                    ReportHelper.mediaVolume(musicVolume);
                    handleJson(respMsg, 0);
                } else if ("modes".equals(jsonAction)) {
                    //--> {200: {"module":"music", "action":"modes", "type":"set, "values": {"mode":"once"}}}
                    String mode = (String) ((JSONObject) json.get("values")).get("mode");
                    //SharedPrefs.putString(RobotApp.getContext(), "music_play_mode", mode);//音乐播放模式
                   // EventBus.getDefault().post(new StateReportEvent(MainActivity.MUSIC_MODE_TYPE, mode));
                    handleJson(respMsg, 0);

                } else if ("next".equals(jsonAction)) {
                    //<-- {70: {"module":"music", "type":"cmd", "action":"next", "values": {"type": 2}}}
                    JSONObject musicJson = (JSONObject) json.get("values");
                    int type = (int) musicJson.get("type");
                   /* context.startService(new Intent(context, MusicPlayerService.class)
                            .putExtra("MSG_COM", MusicPlayerService.NEXT)
                            .putExtra("TYPE_COM", type));*/
                } else if ("last".equals(jsonAction)) {
                    JSONObject musicJson = (JSONObject) json.get("values");
                    int type = (int) musicJson.get("type");
                    /*context.startService(new Intent(context, MusicPlayerService.class)
                            .putExtra("MSG_COM", MusicPlayerService.LAST)
                            .putExtra("TYPE_COM", type));*/
                }
                break;

            case "vedio"://视频设置
                if ("mode".equals(jsonAction)) {
                    // 70: {"module":"vedio", "type":"set", "action":"mode", "values":{"mode":1}}}
                    int mode = (Integer) ((JSONObject) json.get("values")).get("mode");
                 //   int videoModeEvent = mode == 2 ? VideoCallActivity.VIDEO_MODE_DOUBLE : VideoCallActivity.VIDEO_MODE_SINGLE;
                   // EventBus.getDefault().post(new VideoCallEvent(videoModeEvent));
                    handleJson(respMsg, 0);

                } else if ("quality".equals(jsonAction)) {
                    if ("set".equals(json.get("type"))) {
                        // {70: {"module":"vedio", "type":"set", "action":"quality", "values":{"quality":"auto|480p|720p"}}}
                        String quality = (String) ((JSONObject) json.get("values")).get("quality");
                        if (SignDevice.getSign().isCallByUser()) {
                            code = -1;
                        } else{
                            SharedPrefs.putString(context, EMConstant.VIDEO_CALL_QUALITY, quality);
                            DemoHelper.getInstance().setVideocallQuality(quality);
                            code = 0;
                        }
                        handleJson(respMsg, code);

                    } else {
                        // {70: {"module":"vedio", "type":"get", "action":"quality"}}
                        String oldQuality = SharedPrefs.getString(context, EMConstant.VIDEO_CALL_QUALITY, "720p");
                        handleJson(respMsg, 0, oldQuality);
                    }
                }
                break;

            case "monitor"://监控设置
                if ("movement".equals(jsonAction)) {//运动监控开关
                    //{70: {"module":"monitor", "type":"set", "action":"movement"||"sound", "values":{"status":"on"||"off"}}}
                    JSONObject jsonObject = (JSONObject) json.get("values");
                    String status = (String) jsonObject.get("status");

                    boolean isMovementOpen = "on".equals(status) ? true : false;
                    SpManager.getInstance().setMovementState(isMovementOpen);

                    if (isMovementOpen)
                        CameraMonitorHelper.openMovement();
                    else
                        CameraMonitorHelper.closeMovement();

                    handleJson(respMsg, 0);

                } else if ("sound".equals(jsonAction)) { //声音监控开关
                    //{70: {"module":"monitor", "type":"set", "action":"movement"||"sound", "values":{"status":"on"||"off"}}}
                    JSONObject jsonObject = (JSONObject) json.get("values");
                    String status = (String) jsonObject.get("status");

                    boolean isSoundOpen = "on".equals(status) ? true : false;
                    SpManager.getInstance().setSoundState(isSoundOpen);
                    if (isSoundOpen)
                        CameraMonitorHelper.openSoundMonitor();
                    else
                        CameraMonitorHelper.closeSoundMonitor();

                    handleJson(respMsg, 0);

                } else if ("quality".equals(jsonAction)) {//接口取消
                    // {70: {"module":"monitor", "type":"set", "action":"quality", "values":{"type":"movement", "quality":1}}}

                    // Object values = json.get("values");
                    // JSONObject jsonObject = (JSONObject) values;
                    // int quality = (Integer) jsonObject.get("quality");
                    // String type = (String) jsonObject.get("type");
                    // // type ('movement','sound' 运动，声音)
                    // // quality (1,2,3 高,中,低) 运动/声音 识别质量 返回 {"code":0}
                    // if ("sound".equals(type)) {// 声音检测
                    //     SharedPrefs.putInt(context, "sound_quality", quality);
                    //     EventBus.getDefault().post(new AudioEvent(quality));
                    //
                    // } else if ("movement".equals(type)) { // 运动检测
                    //     SharedPrefs.putInt(context, "movement_quality", quality);
                    //     EventBus.getDefault().post(new MovementEvent(quality));
                    // }
                    handleJson(respMsg, 0);

                } else if ("timer".equals(jsonAction)) {
                    //      {200: {"module":"monitor", "type":"set", "action":"timer","values": {
                    //          "secret":"32位UUID", status": "on", start:"08:00", end:"20:00", "label":标签, repeat:[1,2,3]}}}
                    String type = json.getString("type");
                  /*  CameraTimeHelper helper = new CameraTimeHelper(context);
                    if ("set".equals(type)) {//设置、增加、编辑 定时
                        JSONObject jsonObject = (JSONObject) json.get("values");
                        helper.setCameraMonitorTime(jsonObject);
                        handleJson(respMsg, 0);

                    } else if ("get".equals(type)) {//获取 定时
                        helper.isInMultiCameraMonitorTime(new Date());//解析过时的定时删除
                        String cameraTime = SharedPrefs.getString(context, "camera_time", "");
                        JSONArray jsonArray = TextUtils.isEmpty(cameraTime) ? new JSONArray() : new JSONArray(cameraTime);
                        handleJson(respMsg, 0, jsonArray);

                    } else if ("cmd".equals(type)) {//删除 某个定时
                        String secret = (String) ((JSONObject) json.get("values")).get("secret");
                        helper.delCameraMonitorTime(secret);
                        handleJson(respMsg, 0);

                    }*/

                } else if ("files".equals(jsonAction)) { // 服务器获取本地视频列表     -取消
                    // {70: {"module":"monitor", "type":"get", "action":"files"}}
                    // List<String> recordList = LocalRecordHelper.getRecordList();
                    // ReportHelper.files(recordList, context);
                    handleJson(respMsg, 0);

                } else if ("file".equals(jsonAction)) {
                    //                   {70: {"module":"monitor", "type":"get", "action":"file", "values":{"file":"文件名", "easemob":"环信ID"}}}
                    JSONObject jsonObject = (JSONObject) json.get("values");

                    if ("get".equals(json.get("type"))) {
                        String fileName = (String) jsonObject.get("file");
                        String easemob = (String) jsonObject.get("easemob");
                        //获取单个本地视频文件名 返回 {"code":0}
                       /* File sendVideo = new File(RobotApp.getUsbHelper().getPetVideoDir() + File.separator + fileName);
                        if (sendVideo.exists() && sendVideo.length() < 10 * 1024 * 1024) {
                            //文件存在,并且小于 10M
                            HXSendVideoFileUtils hxUtils = new HXSendVideoFileUtils(context);
                            hxUtils.sendVideoFile2App(fileName, easemob);
                            code = 0;
                        } else
                            code = -1;*/

                    } else if ("cmd".equals(json.get("type"))) {
                     /*   String fileName = (String) jsonObject.get("file");
                        File delVideoFile = new File(RobotApp.getUsbHelper().getPetVideoDir() + File.separator + fileName);
                        code = -1;
                        if (delVideoFile.exists() && delVideoFile.delete()) {
                            LogUtil.d("pet_file", "video is deleted");
                            //删除文件成功--删了sp的数据，删缩略图，刷新ac的数据
                            SharedPrefs.remove(context, fileName);
                            File thumbFile = new File(RobotApp.getUsbHelper().getPetThumbDir() + File.separator + fileName.substring(0, 14) + ".png");
                            if (thumbFile.exists() && thumbFile.delete()) {
                                LogUtil.d("pet_file", "thumb is deleted");
                            }
//                            syncLocalVideo();
                            code = 0;
                        }*/
                    }
                    handleJson(respMsg, code);

                } else if ("pause".equals(jsonAction)) {
                    // 视频通话暂停   返回 {"code":0}
                   // EventBus.getDefault().post(new VideoCallEvent(VideoCallActivity.VIDEO_CALL_PAUSE));
                    handleJson(respMsg, 0);

                } else if ("resume".equals(jsonAction)) {
                   // EventBus.getDefault().post(new VideoCallEvent(VideoCallActivity.VIDEO_CALL_RESUME));
                    handleJson(respMsg, 0);
                } else if ("pause_voice".equals(jsonAction)) {
                    // 视频通话暂停   返回 {"code":0}
                  //  EventBus.getDefault().post(new VideoCallEvent(VideoCallActivity.VIDEO_CALL_PAUSE_VOICE));
                    handleJson(respMsg, 0);

                } else if ("resume_voice".equals(jsonAction)) {
                  //  EventBus.getDefault().post(new VideoCallEvent(VideoCallActivity.VIDEO_CALL_RESUME_VOICE));
                    handleJson(respMsg, 0);
                }
                break;

            case "usb":
                if ("out".equals(jsonAction)) { //取消此接口
                    // u盘弹出   返回 {"code":0}
                    LogUtil.d("NICK", "弹出 u 盘");
                    handleJson(respMsg, 0);

                } else if ("size".equals(jsonAction)) {
                    //查询U盘信息 "module":"usb", "type":"get", "action":"size",
                    String message = "";
                   /* if (RobotApp.getUsbHelper().isUsbEnable()) {
                        String freeMemorySize = RobotApp.getUsbHelper().getFreeMemorySize(RobotApp.getUsbHelper().getUsbCardPath());
                        String totalMemorySize = RobotApp.getUsbHelper().getTotalMemorySize(RobotApp.getUsbHelper().getUsbCardPath());
                        code = 0;
                        message = "U盘" + freeMemorySize + "可用,共" + totalMemorySize;
                    } else {
                        code = -1;
                        message = "U盘未插入";
                    }*/
                    handleJson(respMsg, code, message);

                } else if ("format".equals(jsonAction)) {
                    // u盘格式化(暂时删除所有文件)   返回 {"code":0}

                    /*if (RobotApp.getUsbHelper().isUsbEnable()) {// usb文件是否存在
                        //当前正在录制，停止录制
                        if (RecorderActivity.getRecorder() != null)
                            RecorderActivity.getRecorder().finish();
                        //线程去删文件
                        new DelUSBFileThread(context).start();

                        handleJson(respMsg, 0);
                    } else {
                        context.startActivity(new Intent(context, HintActivity.class)
                                .putExtra("info", "no_usb"));
                        LogUtil.d("NICK", "usb 未插入!!!");
                        handleJson(respMsg, -1);
                    }*/
                }
                break;

            case "feed":
                if ("food".equals(jsonAction)) {
                    // {70: {"module":"feed", "type":"cmd", "action":"food"}}
                    // 喂食通知   返回 {"code":0}
                    // 调喂食接口（同时查看上一次播放的order），上报状态

                    // 检查食盒是否打开()
                /*    if (SignDevice.getSign().isLidOpen()) {
                        handleJson(respMsg, -10, context.getResources().getString(R.string.foodbox_reply_msg));
                        break;
                    }*/

                 /*   if (BOX_STATUS_EMPTY == SharedPrefs.getInt(context, "boxStatus", -1)) {
                        if (VideoCallActivity.getVideoCall() != null && SignDevice.getSign().isSingleCall())
                            EventBus.getDefault().post(new UIEvent(MainActivity.UI_FEED_SHOW));

                        //记录零食盒空时，喂食次数
                        ++feedCountWhenBoxEmpty;
                        LogUtil.d("NICK", "空次数：" + feedCountWhenBoxEmpty);
                        if (feedCountWhenBoxEmpty > 2) {//超过2次,喂食返回 code -2
                            code = -2;
                            feedCountWhenBoxEmpty = 3;//防止超过int范围
                            LogUtil.i("NICK", "空次数超过2次，返回-2，不允许再弹");
                            handleJson(respMsg, code);
                            break;
                        }
                    } else {
                        feedCountWhenBoxEmpty = 0;//次数归0
                    }*/

                  /*  if (SignDevice.getSign().isPlayMusic()) {
                        //当音乐播放,先暂停，直到口令播玩之后，继续播放音乐
                        context.sendBroadcast(new Intent().setAction(MusicPlayerService.PAUSE_MUSIC_ACTION));
                        new Thread() {
                            @Override
                            public void run() {
                                SystemClock.sleep(6000);//
                                context.sendBroadcast(new Intent().setAction(MusicPlayerService.RESUME_MUSIC_ACTION));
                            }
                        }.start();
                    } else
                        ControlBoardUtils.getInstance().play_music();//调用底层打开音乐播放喇叭

                    File cusdomOrderFile = new File(cusdom_order_path);
                    File systemOrderFile = new File(system_voice_path);
                    if (cusdomOrderFile.exists() && !cusdomOrderFile.isDirectory() && cusdomOrderFile.getName().endsWith(".mp3")) {
                        FeedOrderHelper.getInstance(context).playCusdomOrder();
                    } else {
                        if (systemOrderFile.exists() && !systemOrderFile.isDirectory() && systemOrderFile.getName().endsWith(".mp3")) {
                            FeedOrderHelper.getInstance(context).playSystemOrder();
                        }
                    }
                    //喂食
                    context.sendBroadcast(new Intent(QueryKeyStateService.FEED_ACTION));*/

                    handleJson(respMsg, 0);

                } else if ("record".equals(jsonAction)) {
                    String type = (String) json.get("type");
                    if ("set".equals(type)) {
                        //                      <-- {70: {"module":"feed", "type":"set", "action":"record", "values":{"file":"system.mp3"}}}
                        // 设置喂食录音 file文件名 md5文件名哈希值  返回 {"code":0}
                        String fileName = (String) ((JSONObject) json.get("values")).get("file");
                        //                        String md5 = (String) jsonObject.get("md5");
                    /*    if ("system.mp3".equals(fileName)) {
                            File cusdomOrderFile = new File(cusdom_order_path);
                            if (cusdomOrderFile.exists() && cusdomOrderFile.delete()) {
                                FeedOrderHelper.getInstance(context).deleteCusdomOrder();
                            }
                        } else {
                            FeedOrderHelper.getInstance(context).getFeedOrder(fileName);
                        }*/
                        handleJson(respMsg, 0);
                    } else if ("get".equals(type)) {
                        //获取本体 录音口令（用户自定义 -1    系统 0）
                      //  File cusdomOrderFile = new File(cusdom_order_path);
                     //   code = cusdomOrderFile.exists() ? -1 : 0;
                        handleJson(respMsg, code);
                    }
                }
                break;

            case "head":
                // 云台转向   返回 {"code":0}
                if ("moveLeft".equals(jsonAction) || "moveRight".equals(jsonAction)) {
                    if (SignDevice.getSign().isLidOpen()) {
                        handleJson(respMsg, -10, context.getResources().getString(R.string.turn_reply_msg));
                        break;
                    }
                }

                if ("moveLeft".equals(jsonAction)) {
                    isDeviceInUsing = true;
                    deviceStateInUseTimeout();
                    //左转
                   // ControlBoardUtils.getInstance().left_turn();

                } else if ("moveRight".equals(jsonAction)) {
                    isDeviceInUsing = true;
                    deviceStateInUseTimeout();
                    //右转
                  //  ControlBoardUtils.getInstance().right_turn();
                } else if ("stop".equals(jsonAction)) {
                    isDeviceInUsing = false;
                    for (int i = 0; i < 3; i++) {
                        SystemClock.sleep(100);
                        // 停止
                     //   ControlBoardUtils.getInstance().stop_turn();
                    }
                }

                handleJson(respMsg, 0);

                break;
            case "easemob":
                if ("endCall".equals(jsonAction)) {
                 //   EventBus.getDefault().post(new VideoCallEvent(VideoCallActivity.VIDEO_CALL_END));
                    handleJson(respMsg, 0);

                } else if ("login".equals(jsonAction)) {
                    //{200: {"module":"easemob", "type":"cmd", "action":"login"}}
                    DemoHelper.getInstance().loginEM(DeviceUtil.getMacAddress().toLowerCase());
                    handleJson(respMsg, 0);

                } else if ("photo".endsWith(jsonAction)) {
                    //{70: { "easemob":"monitor", type: "cmd", "action": "photo"}
                  /*  if (VideoCallActivity.getVideoCall() != null) {
                        setTimeStamp();
                        setTakePhoto(true);

                      //  EventBus.getDefault().post(new VideoCallEvent(VideoCallActivity.VIDEO_TAKE_PHOTO));
                    }*/

                    // {"code":0, "file":{"name: '文件名.jpg',"bucket": '文件存储bucket',"size": 123029 }}   //文件大小

                    JSONObject jsonPhoto = new JSONObject();
                    jsonPhoto.put("name", getTimeStamp() + ".jpg");
                    jsonPhoto.put("bucket", ConstantValue.bucket);
                    jsonPhoto.put("size", (long) 530209);
                    handleJson(respMsg, 0, jsonPhoto);
                }
                break;
            default:
                break;
        }
    }

    private void syncLocalVideo() {
       // EventBus.getDefault().post(new SyncDevStaEvent(MainActivity.SEND_RECORD_VIDEO_LIST));
    }

    private void deviceStateInUseTimeout() {
        if (timeoutThread == null) {
            timeoutThread = new TimeoutThread();
            timeoutThread.start();
        }
    }

    private TimeoutThread timeoutThread;

    class TimeoutThread extends Thread {
        //设备使用中状态的超时时间 20s
        int deviceTimeout = 20;

        @Override
        public void run() {
            int count = 0;
            while (isDeviceInUsing) {
                SystemClock.sleep(1000);
                if (count > deviceTimeout) {//设备使用超时时间
                    isDeviceInUsing = false;
                }
                count++;
            }
            timeoutThread = null;
        }

    }

    private boolean checkOrderWhenSame(String reqJson, long cur) {
        boolean flag = false;
        if (reqJson.equals(oldReqJson) && Math.abs(pass - cur) <= 1000) {
            flag = true;
        }
        oldReqJson = reqJson;
        pass = cur;
        return flag;
    }

    /**
     * 响应返回值 需要code
     */
    public static void handleJson(ACDeviceMsg respMsg, int code) throws JSONException {
        //响应消息体
        JSONObject resp = new JSONObject();
        resp.put("code", code);
        respMsg.setJsonPayload(resp.toString());
        LogUtil.i("NICK", "返回msg：" + resp.toString());
    }

    /**
     * 响应返回值 需要code
     */
    public static void handleJson(ACDeviceMsg respMsg, int code, String msg) throws JSONException {
        //响应消息体
        JSONObject resp = new JSONObject();
        resp.put("code", code);
        resp.put("message", msg);
        respMsg.setJsonPayload(resp.toString());
        LogUtil.i("NICK", "返回msg：" + resp.toString());
    }

    /**
     * 响应返回值 需要code
     *
     * @param respMsg,code,jsonObject
     */
    public static void handleJson(ACDeviceMsg respMsg, int code, JSONObject jsonObject) throws JSONException {
        //响应消息体
        JSONObject resp = new JSONObject();
        resp.put("code", code);
        resp.put("file", jsonObject);
        respMsg.setJsonPayload(resp.toString());
        LogUtil.i("NICK", "返回msg：" + resp.toString());
    }


    /**
     * 响应返回值
     *
     * @param respMsg,code,jsonArray
     */
    public static void handleJson(ACDeviceMsg respMsg, int code, JSONArray jsonArray) throws JSONException {
        //响应消息体
        JSONObject resp = new JSONObject();
        resp.put("code", code);
        resp.put("values", jsonArray);
        respMsg.setJsonPayload(resp.toString());
        LogUtil.d("NICK", "返回定时msg：" + resp.toString());
    }

    private int getRespCode(boolean flag) {
        if (flag) {
            return 0;
        }
        return -1;
    }
}
