package com.aviconics.petrobot.petrobotbody.util;

import android.os.Handler;
import android.os.Looper;

/**
 *  线程切换
 */
public class ThreadUtil {
    private static Handler sHandler = null;

    static {
        sHandler = new Handler(Looper.getMainLooper());//初始化handle，用于切换到主线程
    }

    public static void runInThread(Runnable task) {
        new Thread(task).start();
    }

    public static void runInUIThread(Runnable task) {
        sHandler.post(task);
    }

    public static void runInUIThread(Runnable task, long delayMillis) {
        sHandler.postDelayed(task, delayMillis);
    }

}