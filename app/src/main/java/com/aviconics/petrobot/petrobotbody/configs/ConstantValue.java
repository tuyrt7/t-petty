package com.aviconics.petrobot.petrobotbody.configs;

import android.os.Environment;

import com.aviconics.petrobot.petrobotbody.util.DeviceUtil;

/**
 * Created by futao on 2017/9/11.
 */

public class ConstantValue {

    /* 云存储 目录 */
    public static final String bucket = "<"+ DeviceUtil.getMacAddress()+">";

    //系统默认喂食录音路径
    public static final String voice_sys_dir =  Environment.getExternalStorageDirectory().getAbsolutePath()+"/PetRobot/voice";
    //系统喂食录音
    public static final String system_voice_name = "yaho.mp3";
    //app发送的喂食录音名称
    public static final String order_voice_name ="order_voice.mp3";
    /**
     *  设备wifi状态
     */
    public static final int WIFI_CONN = 31;
    public static final int WIFI_FORGET = 32;
    public static final int WIFI_DIS = 33;
}
