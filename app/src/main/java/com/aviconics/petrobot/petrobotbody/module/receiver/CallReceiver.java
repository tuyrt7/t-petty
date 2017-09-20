/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aviconics.petrobot.petrobotbody.module.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.em.DemoHelper;
import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;
import com.hyphenate.chat.EMClient;


public class CallReceiver extends BroadcastReceiver {
    /**
     * 检查是否已经登录过
     *
     * @return
     */
    public boolean isLogined() {
        return EMClient.getInstance().isLoggedInBefore();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!isLogined())
            return;
        LogUtil.d("NICK", "接收到电话要求");

        //拨打方username
        String from = intent.getStringExtra("from");
        LogUtil.d("NICK", "tousername --------:" + from);
        SharedPrefs.putString(context.getApplicationContext(), "toUsername", from);//本地存入对方的环信号
        //call type
        String type = intent.getStringExtra("type");

        if ("video".equals(type)) { //视频通话
           LogUtil.d("NICK", "视频通话");
          /*   SignDevice.getSign().setCallByUser(true);

            if (VideoPlayActivity.getVideoPlay() != null) {
                context.sendBroadcast(new Intent().setAction(VideoPlayActivity.VIDEO_NORMAL_STOP_ACTION));
            }

            boolean flag = false;

            //停掉两个监控和录像
            if (AppManager.getAppManager().isServiceRunning(context, SoundMonitorService.soundMonitor)) {
                context.stopService(new Intent(context, SoundMonitorService.class));
            }

            if (MovementActivity.getMovement() != null) {
                EventBus.getDefault().post(new CallEvent(from));
                flag = true;
            }
            if (RecorderActivity.getRecorder() != null) {
                EventBus.getDefault().post(new RecordEvent(from));
                flag = true;
            }

            if (!flag) {
                startCall(context, from);
            }*/

            DemoHelper.getInstance().addCallStateListener();
        }
    }

    private void startCall(Context context, String from) {
        //进行视频通话
      /*  context.startActivity(new Intent(context, VideoCallActivity.class).
                putExtra("username", from).putExtra("isComingCall", true).
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));*/
    }




}
