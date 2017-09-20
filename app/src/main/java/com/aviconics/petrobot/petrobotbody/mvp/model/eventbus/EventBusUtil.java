package com.aviconics.petrobot.petrobotbody.mvp.model.eventbus;


import org.greenrobot.eventbus.EventBus;

/** EventBus
 *  在Base Activity/Fragment 的子Activity/Fragment中,如果有需要接收,复写isRegisterEventBus() 并返回true 和receiveEvent(Event event),
 *  不用每个地方都去订阅和取消订阅。
 *  根据Event给定code和泛型能够很好的区分不同的事件来源和数据类型
 */

public class EventBusUtil {

    public static void register(Object subscriber) {
        EventBus.getDefault().register(subscriber);
    }

    public static void unregister(Object subscriber) {
        EventBus.getDefault().unregister(subscriber);
    }

    public static void sendEvent(Event event) {
        EventBus.getDefault().post(event);
    }

    public static void sendStickyEvent(Event event) {
        EventBus.getDefault().postSticky(event);
    }

}
