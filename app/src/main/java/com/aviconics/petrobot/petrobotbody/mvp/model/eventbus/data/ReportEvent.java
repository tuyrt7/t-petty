package com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data;

/**
 * 提示事件
 *   state = in  out  err
 */
public class ReportEvent {

    private String msg;
    private int  type;

    public ReportEvent(int  type , String msg) {
        this.type = type;
        this.msg = msg;
    }

    public int getType() {
        return type;
    }
    
    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "StateReportEvent{" +
                "msg='" + msg + '\'' +
                ", type=" + type +
                '}';
    }
}
