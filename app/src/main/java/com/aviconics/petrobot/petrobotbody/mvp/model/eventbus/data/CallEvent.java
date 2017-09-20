package com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data;

/** 视频通话的事件
 * Created by win7 on 2016/6/28.
 */
public class CallEvent {
    int mode;

    public CallEvent(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }
}
