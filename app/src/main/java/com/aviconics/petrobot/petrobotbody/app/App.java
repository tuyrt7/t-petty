package com.aviconics.petrobot.petrobotbody.app;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.BuildConfig;
import com.aviconics.petrobot.petrobotbody.em.DemoHelper;
import com.aviconics.petrobot.petrobotbody.util.CrashHandler;
import com.aviconics.petrobot.petrobotbody.util.Pop;
import com.aviconics.petrobot.petrobotbody.util.UsbHelper;
import com.blankj.utilcode.util.Utils;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by futao on 2017/9/7.
 */

public class App extends SuperApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        init();

        setDebug();
    }

    @Override
    protected void onAppExit() {
        Pop.showSafe("----应用退出----");
    }

    private void init() {
        CrashHandler.getInstance().init(this);
        Utils.init(this);
        UsbHelper.instance().init(this);
        DemoHelper.getInstance().init(this);
        initManager();
    }

    private void initManager() {

    }

    private void setDebug() {
        if (BuildConfig.ENV_DEBUG) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                return;
            }
            mWatcher = LeakCanary.install(this);
        }
        LogUtil.setDebug(BuildConfig.ENV_DEBUG);
        CrashReport.initCrashReport(getContext(), "15af651176", true);
    }

    private static RefWatcher mWatcher;


    public static RefWatcher getRefWatcher() {
        return mWatcher;
    }

    public static UsbHelper getUsbHelper() {
        return UsbHelper.instance();
    }

}
