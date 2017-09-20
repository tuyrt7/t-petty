package com.aviconics.petrobot.petrobotbody.util;

import android.content.Context;

import java.util.Map;
import java.util.Set;

/**
 * Created by lyt on 2016/4/17.
 */
// 不能在多个进程中同时调用
public class SharedPrefs {

    private static final String name = "pet_sp";
    private static int mode = Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS;

    public static int getInt(Context context, String key, int defValue) {
        return context.getApplicationContext().getSharedPreferences(name, mode).getInt(key, defValue);
}

    public static boolean putInt(Context context, String key, int value) {
        return context.getApplicationContext().getSharedPreferences(name, mode).edit().putInt(key, value).commit();
    }

    public static String getString(Context context, String key, String defValue) {
        return context.getApplicationContext().getSharedPreferences(name, mode).getString(key, defValue);
    }

    public static boolean putString(Context context, String key, String value) {
        return context.getApplicationContext().getSharedPreferences(name, mode).edit().putString(key, value).commit();
    }

    public static long getLong(Context context, String key, long defValue) {
        return context.getApplicationContext().getSharedPreferences(name, mode).getLong(key, defValue);
    }

    public static boolean putLong(Context context, String key, long value) {
        return context.getApplicationContext().getSharedPreferences(name, mode).edit().putLong(key, value).commit();
    }

    public static float getFloat(Context context, String key, float defValue) {
        return context.getApplicationContext().getSharedPreferences(name, mode).getFloat(key, defValue);
    }

    public static boolean putFloat(Context context, String key, float value) {
        return context.getApplicationContext().getSharedPreferences(name, mode).edit().putFloat(key, value).commit();
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        return context.getApplicationContext().getSharedPreferences(name, mode).getBoolean(key, defValue);
    }

    public static boolean putBoolean(Context context, String key, boolean value) {
        return context.getApplicationContext().getSharedPreferences(name, mode).edit().putBoolean(key, value).commit();
    }

    public static Set<String> getStringSet(Context context, String key, Set<String> defValue) {
        return context.getApplicationContext().getSharedPreferences(name, mode).getStringSet(key, defValue);
    }

    public static boolean putStringSet(Context context, String key, Set<String> values) {
        return context.getApplicationContext().getSharedPreferences(name, mode).edit().putStringSet(key, values).commit();
    }

    public static Map<String, ?> getAll(Context context, String key, boolean defValue) {
        return context.getApplicationContext().getSharedPreferences(name, mode).getAll();
    }

    public static boolean contains(Context context, String key) {
        return context.getApplicationContext().getSharedPreferences(name, mode).contains(key);
    }


    /**
     * 移除某个key值已经对应的值
     * @param context
     * @param key
     */
    public static boolean remove(Context context, String key){
        return  context.getApplicationContext().getSharedPreferences(name, mode).edit() .remove(key).commit();
    }

    /**
     *  清除sp的数据
     * @param context
     */
    public static boolean clearData(Context context) {
       return context.getApplicationContext().getSharedPreferences(name, mode).edit().clear().commit();
    }

}
