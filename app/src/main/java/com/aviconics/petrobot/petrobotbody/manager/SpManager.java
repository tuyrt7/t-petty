package com.aviconics.petrobot.petrobotbody.manager;

import android.content.Context;

import com.aviconics.petrobot.petrobotbody.configs.ConstantValue;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;


/**
 * Created by futao on 2017/9/8.
 */

public class SpManager {

    private Context context;

    private SpManager() {
        context = App.getContext();
    }

    public static SpManager getInstance() {
        return InnerSingle.ourInstance;
    }

    private static class InnerSingle {
        private static final SpManager ourInstance = new SpManager();
    }

    static class Name {
        static final String PET_BIND_APP = "pet_bind_app"; //绑定app
        static final String USB_PATH = "usb_path"; //usb 路径
        static final String WIFI_NAME = "wifi_name"; //wifi 名称
        static final String WIFI_PWD = "wifi_pwd"; //wifi 密码
        static final String BIND_APP_PHONENUMBER = "bind_app_phonenumber";
        static final String DEVICE_MAC = "device_mac";
        static final String REGISTER_EM_NUMBER = "register_em_number";
        static final String DEVICE_WIFI = "device_wifi";
        static final String MOVEMENT_STATE = "movement_state";
        static final String SOUND_STATE = "sound_state";
    }

    public boolean getBindState() {
        return SharedPrefs.getBoolean(context, Name.PET_BIND_APP, false);
    }

    public boolean setBindState(boolean isBind) {
        return SharedPrefs.putBoolean(context, Name.PET_BIND_APP, isBind);
    }

    public String getUsbpath() {
        return SharedPrefs.getString(context, Name.USB_PATH, "");
    }

    public boolean setUsbpath(String path) {
        return SharedPrefs.putString(context, Name.USB_PATH, path);
    }

    public boolean setWifiName(String wifiName) {
        return SharedPrefs.putString(context, Name.WIFI_NAME, wifiName);
    }

    public String getWifiName() {
        return SharedPrefs.getString(context, Name.WIFI_NAME, "");
    }

    public boolean setWifiPwd(String wifiName) {
        return SharedPrefs.putString(context, Name.WIFI_PWD, wifiName);
    }

    public String getWifiPwd() {
        return SharedPrefs.getString(context, Name.WIFI_PWD, "");
    }

    public boolean setAppNumber(String appNumber) {
        return SharedPrefs.putString(context, Name.BIND_APP_PHONENUMBER, appNumber);
    }

    public String getAppNumber() {
        return SharedPrefs.getString(context, Name.BIND_APP_PHONENUMBER, "");
    }

    public boolean setDeviceMac(String mac) {
        return SharedPrefs.putString(context, Name.DEVICE_MAC, mac);
    }

    public String getDeviceMac() {
        return SharedPrefs.getString(context, Name.DEVICE_MAC, "");
    }

    public boolean setRegisterEmNumber(boolean isregister) {
        return SharedPrefs.putBoolean(context, Name.REGISTER_EM_NUMBER, isregister);
    }

    public boolean getRegisterEmNumber() {
        return SharedPrefs.getBoolean(context, Name.REGISTER_EM_NUMBER, false);
    }

    public boolean setDevWifiState(int devWifi) {
      return   SharedPrefs.putInt(context,Name.DEVICE_WIFI, devWifi);
    }

    public int getDevWifiState() {
        return  SharedPrefs.getInt(context,Name.DEVICE_WIFI, ConstantValue.WIFI_DIS);
    }


    public boolean setMovementState(boolean state) {
        return SharedPrefs.putBoolean(context, Name.MOVEMENT_STATE, state);
    }

    public boolean getMovementState() {
        return SharedPrefs.getBoolean(context, Name.MOVEMENT_STATE, false);
    }

    public boolean setSoundState(boolean state) {
        return SharedPrefs.putBoolean(context, Name.SOUND_STATE, state);
    }

    public boolean getSoundState() {
        return SharedPrefs.getBoolean(context, Name.SOUND_STATE, false);
    }
}
