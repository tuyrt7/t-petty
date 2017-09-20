package com.aviconics.petrobot.petrobotbody.util;

import android.content.Context;
import android.widget.Toast;

import com.aviconics.petrobot.petrobotbody.app.App;

/**
 *
 */
public class Pop {
    private static Toast toast = null;

    public static void popToast(Context context, CharSequence title) {
        if (toast == null) {
            if (context == null)
                context = App.getContext();
            toast = Toast.makeText(context, title, Toast.LENGTH_SHORT);
        } else
            toast.setText(title);
        toast.show();
    }

    public static void showSafe(final CharSequence text) {
        ThreadUtil.runInUIThread(new Runnable() {
            @Override
            public void run() {
                popToast(null,text);
            }
        });
    }

    /**
     * 关闭Toast
     */
    public static void cancelToast() {
        if (toast != null) {
            toast.cancel();
        }
    }
}
