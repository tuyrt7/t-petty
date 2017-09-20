package com.aviconics.petrobot.petrobotbody.util;

import android.app.Activity;
import android.app.Service;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.text.TextUtils;

import com.accloud.utils.ACUtils;
import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.manager.SpManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * 原来MAC地址是直接从"/sys/class/net/" + name + "/address"文件中读取的！(开发板mac Name mth0 )
 */
public class DeviceUtil {

    /**
     * 手机获取wifi　mac 地址
     *
     * @return
     */
    public static String getPhysicalDeviceId() {
        String ret = "0000" + ACUtils.getMacAddress(App.getContext()).replace(":", "");
        String s = ret.toUpperCase();
        LogUtil.i("DeviceUtil", s);
        return s;

    }

    public static final String DEVICE_MAC = "device_mac";

    /**
     * 获取 Ethernet 的MAC地址
     * Linux  eth0--- Android  wlan0
     * <p>
     * 旧板子用eth0  新板子 wlan0
     *
     * @return
     */
    public static String getMacAddress() {
        String mac = SpManager.getInstance().getDeviceMac();
        if (!TextUtils.isEmpty(mac))
            return mac;

        try {
            String s = loadFileAsString("/sys/class/net/wlan0/address").toUpperCase(Locale.ENGLISH).substring(0, 17);
            String s1 = "0000" + s.toUpperCase().replace(":", "");
            LogUtil.i("DeviceUtil", s1);
            SpManager.getInstance().setDeviceMac(s1);
            return s1;
        } catch (IOException e) {
            LogUtil.d("NICK", "获取 mac 失败");
            return null;
        }
    }

    public static String loadFileAsString(String filePath) throws java.io.IOException {
        StringBuffer fileData = new StringBuffer(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead = 0;
        while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }

    /**
     * 获取设备的mac地址
     *
     * @param ac
     * @param callback 成功获取到mac地址之后会回调此方法
     */
    public static void getSafeMacAddress(final Activity ac, final SimpleCallback callback) {
        final WifiManager wm = (WifiManager) ac.getApplicationContext().getSystemService(Service.WIFI_SERVICE);

        // 如果本次开机后打开过WIFI，则能够直接获取到mac信息。立刻返回数据。
        WifiInfo info = wm.getConnectionInfo();
        if (info != null && info.getMacAddress() != null) {
            if (callback != null) {
                callback.onComplete(info.getMacAddress());
            }
            return;
        }

        // 尝试打开WIFI，并获取mac地址
        if (!wm.isWifiEnabled()) {
            wm.setWifiEnabled(true);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                int tryCount = 0;
                final int MAX_COUNT = 10;

                while (tryCount < MAX_COUNT) {
                    final WifiInfo info = wm.getConnectionInfo();
                    if (info != null && info.getMacAddress() != null) {
                        if (callback != null) {
                            ac.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onComplete(info.getMacAddress());
                                }
                            });
                        }
                        return;
                    }

                    SystemClock.sleep(300);
                    tryCount++;
                }

                // 未获取到mac地址
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        }).start();
    }


    public interface SimpleCallback {
        void onComplete(String result);
    }
}
