package com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data;


/** 视频通话的事件
 * Created by win7 on 2016/6/28.
 */
public class MotionEvent {
    int number;

    public MotionEvent(int mode) {
        this.number = mode;
    }

    public int getFrom() {
        return number;
    }


}
