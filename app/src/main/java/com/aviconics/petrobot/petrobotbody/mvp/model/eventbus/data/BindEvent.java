package com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data;

/**
 * Created by futao on 2017/7/14.
 */

public class BindEvent {
    /**
     *  NO_BIND = 201;
     */
    int bindState;

    public BindEvent(int bindState) {
        this.bindState = bindState;
    }

    public int getBindState() {
        return bindState;
    }

    public void setBindState(int bindState) {
        this.bindState = bindState;
    }
}
