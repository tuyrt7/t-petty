package com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data;

/**
 * Created by futao on 2017/9/11.
 */

public class UIEvent {
    int type;

    public UIEvent(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "UIEvent{" +
                "type=" + type +
                '}';
    }
}
