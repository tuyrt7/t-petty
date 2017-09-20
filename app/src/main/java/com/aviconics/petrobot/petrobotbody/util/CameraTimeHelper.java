package com.aviconics.petrobot.petrobotbody.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.accloud.utils.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 说明：
 定时开     监控开     定时时间段内触发监控条件录制，其他时间段内不录制。
 定时关     监控开     触发监控条件进行录制

 定时开     监控关     不录制
 定时关     监控关     不录制
 */


/**
 * 摄像头开启时间的工具
 */
public class CameraTimeHelper {

    private long mEndpoint;//单次定时的时间终结点 时间戳（只有当定时的重复周期repeat 为空数组才有效）
    private long l = 24 * 60 * 60 * 1000; //每天的毫秒数

    public CameraTimeHelper(Context context) {
        this.context = context;
    }

    private Context context;
    private String start, end;
    private JSONArray repeat;
//    private String repeat;

    /**
     * 判断date 是否在sp中给定的时间域里面
     *
     * @return
     */
    private boolean isBetweenStartAndEnd(Date date) throws ParseException, JSONException {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        Date parseStart = sdf.parse(start);
        Date parseEnd = sdf.parse(end);
        Date now = sdf.parse(sdf.format(date));//获取传入的时间点  时分 格式的date
        Log.d("NICK", "now:" + now.getTime() + "+++++++start:" + parseStart.getTime() + "+++++end:" + parseEnd.getTime());
//        if (now.after(parseStart) && now.before(parseEnd)) {//不包括开始时间点
//            return true;
//        }
        if (now.getTime() >= parseStart.getTime() && now.getTime() <= parseEnd.getTime()) {
            //把对应的开始时间点和结束时间点也包括
            return true;
        }

        return false;
    }


    //
    /**
     * 根据定时，是否允许触发监控进行视频录制
     * 解析values:  [{ "label":"标签","secret":"32位UUID","status": "on",
     *          start:"08:00", end:"20:00", repeat:[1,2,3],"day":"today|tomorrow|空串","endPoint":"12321213"}]
     * @param date
     * @return
     * @throws JSONException
     * @throws ParseException
     */
    public boolean isInMultiCameraMonitorTime(Date date) {
        String camera_time = SharedPrefs.getString(context, "camera_time", "");
        LogUtil.i("NICK", "检测recorder 时间：" + camera_time);
        if (TextUtils.isEmpty(camera_time)) {
            //未设置时间 默认为一直开启
            return true;
        }
        JSONArray jsonArray = null;
        List<String> delSecrets = new ArrayList<>();
        boolean result = false;
        try {
            jsonArray = new JSONArray(camera_time);
            if (jsonArray.length() == 0) {
                return true;//未设置时间 默认为一直开启
            }
            JSONObject jsonObject = null;
            int timeCount = 0;
            long endPoint;//记录
            String secret;//记录
            for (int i = 0; ; i++) {
                if (jsonArray.isNull(i)) {
                    break;
                }
                jsonObject = (JSONObject) jsonArray.get(i);
                String status = (String) jsonObject.get("status");
                boolean switchStatus = getSwitchStatus(status);
                if (switchStatus) {
                    start = (String) jsonObject.get("start");
                    end = (String) jsonObject.get("end");
                    repeat = (JSONArray) jsonObject.get("repeat");
                    secret = (String) jsonObject.get("secret");
                    endPoint = Long.parseLong((String) jsonObject.get("endPoint"));
                    if (repeat.length() == 0) { //无重复周期，单次定时
                        if (date.getTime() > endPoint-l && date.getTime() <= endPoint) {
                            LogUtil.i("NICK","单次定时生效");
                            if (isBetweenStartAndEnd(date)) result = true;
                        } else if (date.getTime() > endPoint) {
                            LogUtil.i("NICK","删除 secret="+secret);
                            delSecrets.add(secret);//把超出end 时间点的定时加入删除的列表中
                        }
                    } else { // 有重复周期
                        List<String> listRepeat = ArrayCastUtils.intArrayToString(repeat);
                        String weekDay = String.format("%tA", date);//获取 星期几
                        if (listRepeat.contains(weekDay)) {
                            if (isBetweenStartAndEnd(date)) result = true;
                        }
                    }
                } else {
                    timeCount++;
                }
            }
            //根据标记判断是否所有定时都为关闭
            if (timeCount == jsonArray.length()) {
                result = true;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        LogUtil.i("NICK","-------delSecrets-------");
        if (delSecrets.size() != 0) {
            for (String SecretKeyEntry : delSecrets) {
                LogUtil.i("NICK","-------delSecrets-------:"+SecretKeyEntry);
                delCameraMonitorTime(SecretKeyEntry);
            }
        }
        return result;
    }

    private boolean getSwitchStatus(String status) {
        if ("on".equals(status)) {
            return true;
        } else if ("off".equals(status)) {
            return false;
        }
        return true;
    }


    /**
     * 是否在单个定时监控时间域内
     *
     * @param date
     * @return
     */
    public boolean isInSingleCameraMonitorTime(Date date) {

        String camera_time = SharedPrefs.getString(context, "camera_time", "");
        if (TextUtils.isEmpty(camera_time)) {
            //未设置时间 默认为一直开启
            return true;
        }

        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(camera_time);
            String status = (String) jsonObject.get("status");
            boolean switchStatus = getSwitchStatus(status);
            if (switchStatus) {
                start = (String) jsonObject.get("start");
                end = (String) jsonObject.get("end");
//                repeat = (JSONArray) jsonObject.get("repeat");
                repeat = (JSONArray) jsonObject.get("repeat");

                boolean isMonitoringTime = isBetweenStartAndEnd(date);
                if (isMonitoringTime) {
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *  根据主键删除 定时对象
     * @param secret
     */
    public void delCameraMonitorTime(String secret) {
        String camera_time = SharedPrefs.getString(context, "camera_time", "");
        if (TextUtils.isEmpty(camera_time)) {
            //本地未有定时
            LogUtil.d("NICK", "设备未设定时");
            return;
        }
        JSONArray jsonArray = null;
        JSONArray newJsonArray = null;
        try {
            jsonArray = new JSONArray(camera_time);
            newJsonArray = new JSONArray();
            JSONObject jsonObject = null;
            int j = 0;
            for (int i = 0; ; i++) {
                if (jsonArray.isNull(i)) {
                    break;
                }
                jsonObject = (JSONObject) jsonArray.get(i);
                if (!jsonObject.get("secret").equals(secret)) {
                    newJsonArray.put( j++, jsonObject);
                } else {
                    LogUtil.d("NICK", "删除了定时：" + jsonObject.toString());
                }
            }
            jsonArray = null;
            SharedPrefs.putString(context, "camera_time", newJsonArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            LogUtil.d("NICK", "json 解析 error");
        }

    }

    /**
     *  当天0点的 毫秒数
     * @return
     */
    public long getTodayZero() {
        Date date = new Date();
        //date.getTime()是现在的毫秒数，它 减去 当天零点到现在的毫秒数（ 现在的毫秒数%一天总的毫秒数，取余。），理论上等于零点的毫秒数，不过这个毫秒数是UTC+0时区的。
        //减8个小时的毫秒值是为了解决时区的问题。
        return (date.getTime() - (date.getTime()%l) - 8* 60 * 60 *1000);
    }

    /**
     * 设置定时(增加、编辑)
     *
     * @param jsonObject
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void setCameraMonitorTime(JSONObject jsonObject) {
       // "values": { "day":"today|tomorrow|"" ","secret":"32位UUID", status": "on", start:"08:00", end:"20:00", "label":标签, repeat:[1,2,3]}
        JSONArray jsonArray = null;
        try {
            //根据定时信息，加入endPoint（单次定时 终结点 long类型） 字段信息
            JSONArray repeats = (JSONArray) jsonObject.get("repeat");
            if (repeats.isNull(0)) {
                //没有重复周期
                String day = (String) jsonObject.get("day");
                String end = (String) jsonObject.get("end");

                SimpleDateFormat hmSdf = new SimpleDateFormat("HH:mm", Locale.CHINA);
                long t = hmSdf.parse(end).getTime() + 8 * 60 * 60 * 1000;//一天中end 结点的毫秒数(从0点起)
                long l = 24 * 60 * 60 * 1000; //每天的毫秒数
                if ("tomorrow".equals(day)) {
                    mEndpoint =  getTodayZero() + l + t;
                } else if ("today".equals(day)) {
                    mEndpoint = getTodayZero() + t;
                } else {
                    mEndpoint = 0;
                }
            } else {
                //有重复周期
                mEndpoint = 0;
            }
            jsonObject.put("endPoint", String.valueOf(mEndpoint));


            //加入定时，本地持久化
            String camera_time = SharedPrefs.getString(context, "camera_time", "");
            if (TextUtils.isEmpty(camera_time)) {
                //本地未有定时
                jsonArray = new JSONArray();
                jsonArray.put(0, jsonObject);
            } else {    //本地有定时
                jsonArray  = new JSONArray(camera_time);
                String secret = (String) jsonObject.get("secret");//传入的定时主键
                JSONObject json = null;
                boolean flag = false;//标记  判断本地是否存有传入的定时对象
                for (int i = 0; ; i++) {
                    if (jsonArray.isNull(i)) {
                        if (!flag) {
                            jsonArray.put(i, jsonObject);
                        }
                        break;
                    }
                    json = (JSONObject) jsonArray.get(i);
                    if (json.get("secret").equals(secret)) {
//                        System.out.println("--------前:"+jsonArray.toString());
                        jsonArray.remove(i);
//                        System.out.println("---------正："+jsonArray.toString());
                        jsonArray.put(jsonArray.length(), jsonObject);
//                        System.out.println("--------后:"+jsonArray.toString());
                        //json = jsonObject
                        flag = true;
                        LogUtil.d("NICK", "编辑定时对象");
                    }
                }
            }
            SharedPrefs.putString(context, "camera_time", jsonArray.toString());//本地存储 定时数组对象
        } catch (JSONException e) {
            e.printStackTrace();
            LogUtil.d("NICK", "json 解析 error");
        } catch (ParseException e) {
            e.printStackTrace();
            LogUtil.d("NICK", "json 解析 error");
        }
    }
}
