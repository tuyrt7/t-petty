package com.aviconics.petrobot.petrobotbody.module.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.em.DemoHelper;
import com.aviconics.petrobot.petrobotbody.util.DeviceUtil;
import com.hyphenate.chat.EMClient;

/**
 * Created by futao on 2017/7/5.
 */

public class ReConnReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.aviconics.petrobot.reconn.em".equals(intent.getAction())) {
            LogUtil.d("NICK","收到重登陆环信的消息");
            EMClient.getInstance().logout(true);
            DemoHelper.getInstance().loginEM(DeviceUtil.getMacAddress().toLowerCase());
        }
    }
}
