package com.aviconics.petrobot.petrobotbody.net;

import android.content.Context;

import com.accloud.clientservice.AC;
import com.accloud.common.ACDeviceMsg;
import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.configs.AcConfig;
import com.aviconics.petrobot.petrobotbody.configs.ConstantValue;
import com.aviconics.petrobot.petrobotbody.util.DeviceUtil;
import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by lyt on 2016/5/22.
 */
public class ReportHelper {

    private static void reportData(JSONObject jsonData) {
        ACDeviceMsg req = new ACDeviceMsg();
        req.setMsgCode(AcConfig.CODE_REPORT);
        req.setJsonPayload(jsonData.toString());
        AC.reportDeviceMsg(req);
    }

    private static JSONObject getJsonHead(String module, String type, String action, Object values) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        if (module != null) {
            jsonObject.put("module", module);
        }
        if (type != null) {
            jsonObject.put("type", type);
        }
        if (action != null) {
            jsonObject.put("action", action);
        }

        if (values != null) {
            jsonObject.put("values", values);
        }
        return jsonObject;
    }

    /**
     * API-3.1.1
     * 上报sn和环信账号
     * {200: {"module":"system", "type":"res", "action":"sn", "values":{"sn":"abcdefg", "easemob":"环信帐号"}}}
     */
    public static void reportAccount() {
        JSONObject values = new JSONObject();
        try {
            values.put("sn", "sn123");
            values.put("easemob", DeviceUtil.getMacAddress().toLowerCase());
            JSONObject jsonHead = getJsonHead("system", "res", "sn", values);
            LogUtil.i("NICK", "reportAccount: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }

    /**
     * API 1.1.4
     * <-- {200: {"module":"system", "type":"cmd", "action":"push",
     * "values":
     * {"pushMsg":"推送消息的内容","title":"movement|sound |openLid" }}
     *
     * @param context
     * @param type    title
     */
    public static void pushMsg(Context context, String type) {
        JSONObject values = new JSONObject();
        try {
            values.put("title", type);
            values.put("pushMsg", context.getResources().getString(R.string.push_msg));
            JSONObject jsonHead = getJsonHead("system", "cmd", "push", values);
            LogUtil.i("NICK", "pushMsg: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }

    }

    /**
     * API-1.1.5
     * 设备日志上报
     * {200: {"module":"logs", "type":"res", "action":"log", {"cpu":"error", "flash":"ok", "mcu":"ok", "kernel":"ok", "usb":"ok"}}
     */
    public static void deviceState(/*Context context ,String cpuState,String isFlash,String mcu,String kernel,String usbState*/) {
        JSONObject values = new JSONObject();
        try {
            //values.put("flash","无");
            //values.put("kernel",kernel);
            values.put("cpu", "正常运行");
            values.put("mcu", "ok");
            values.put("usb", App.getUsbHelper().isUsbEnable() ? "ok" : "no");
            JSONObject jsonHead = getJsonHead("logs", "res", "log", values);
            LogUtil.i("NICK", "deviceState: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }

    /**
     * API-2.1.3
     * 媒体文件播放状态上报
     * --> {200: {"module":"music", "action":"stoped", "type":"res", "musicStatus":"stoped","values":{"url":""}}}
     */
    public static void musicState(String state, String url) {
        JSONObject values = new JSONObject();
        try {
            values.put("url", url);
            JSONObject jsonHead = getJsonHead("media", "res", "stoped", values);
            jsonHead.put("musicStatus", state);
            LogUtil.i("NICK", "musicState: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }

    }

    /**
     * API-2.1.5
     * 音乐播放的模式上报
     * {200: {"module":"music", "action":"syncMode", "type":"res","musicMode":"cycle"}}
     */
    public static void musicModeState(String mode) {
        try {
            JSONObject jsonHead = getJsonHead("music", "res", "syncMode", null);
            jsonHead.put("musicMode", mode);
            LogUtil.i("NICK", "musicModeState: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }

    /**
     * 上报视频缩略图
     * {200: {"module":"vedio", "type":"res", "action":"upload", "values":{"file":'fileneme.mp4', "thumb":"thumb.png", "bucket": "a/file/bucket"}}}
     */
    public static void upload(Context context, String videoName) {
        JSONObject values = new JSONObject();
        try {
            values.put("file", videoName);
            String s = SharedPrefs.getString(context, videoName, "");////value - 缩略图名,文件大小,时长
            String[] split = s.split(",");
            if (split.length != 3) {
                return;
            }
            String thumbName = split[0];
            String videoFileSize = split[1];
            String videoTimeSize = split[2];
            values.put("thumb", thumbName);
            values.put("size", videoFileSize);
            values.put("duration", videoTimeSize);
            values.put("bucket", ConstantValue.bucket);

            JSONObject jsonHead = getJsonHead("vedio", "res", "upload", values);
            LogUtil.i("NICK", "upload: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }

/*    *//**
     * 上传本地视频列表
     * {200: {"module":"monitor", "type":"get", "action":"files", "values":
     * {"total":10, "files":[{"file":{"file":'fileneme.mp4', "thumb":"thumb.png", "bucket": "a/file/bucket","size":1000, duration:60}}…]}}}
     *//*

    public static void files(List<String> recordList, Context context) {
        JSONObject values = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            if (recordList == null || recordList.size() == 0) {
                values.put("total", 0);
                values.put("files", jsonArray);
            } else {
                values.put("total", recordList.size());
                for (int i = 0; i < recordList.size(); i++) {

                    String videoName = recordList.get(i);
                    File videoFile = new File(App.getUsbHelper().getPetVideoDir() + File.separator + videoName);
                    String string = SharedPrefs.getString(context, videoName, "");
                    String[] split = null;
                    try {
                        split = string.split(",");
                    } catch (RuntimeException ex) {
                        videoFile.delete();
                        continue;
                    }
                    long videoFileSize = 0;
                    long videoTimeSize = 0;
                    String thumbName = "";
                    if (split.length >= 3 && videoFile.exists() && videoName.endsWith(".mp4")) {
                        //LogUtil.d("NICK","如果没有获取sp中的值，再次创建缩略图，存sp");
                        //防止手动改了文件名，不合规范的直接删除文件
                        try {
                            thumbName = videoName.substring(0, 14) + ".png";
                        } catch (RuntimeException ex) {
                            videoFile.delete();
                            continue;
                        }
                        videoFileSize = videoFile.length() / 1024;//视频文件大小 kb
                        videoTimeSize = PetVideoInfoManager.getVideoDuration(videoFile.getAbsolutePath());
                        //以 视频文件名为key，以 缩略图，文件大小，时长 为value  本地存视频文件信息
                        SharedPrefs.putString(context, videoName, thumbName + "," + videoFileSize + "," + videoTimeSize);
                        ThumbUtils.bitmapWriteAnd2Bytes(context, videoFile.getAbsolutePath(), videoName);
                    } else {
                        thumbName = split[0];
                        videoFileSize = Long.parseLong(split[1]);
                        videoTimeSize = Long.parseLong(split[2]);
                    }
                    String bucket = ConstantValue.bucket;
                    JSONObject json1 = new JSONObject();
                    JSONObject json2 = new JSONObject();

                    json1.put("file", videoName);
                    json1.put("bucket", bucket);
                    json1.put("thumb", thumbName);
                    json1.put("size", videoFileSize);
                    json1.put("duration", videoTimeSize);

                    json2.put("file", json1);
                    jsonArray.put(json2);
                }
                values.put("files", jsonArray);
            }

            JSONObject jsonHead = getJsonHead("monitor", "res", "files", values);
            LogUtil.i("NICK", "files: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }*/

    /**
     * API-3.2.1
     * U盘事件上报 注意没有action
     * {200: {"module":"usb", "type":"res", "usbStatus":"out"}}
     */
    public static void inUSB(String usbState) {
        try {
            JSONObject jsonHead = getJsonHead("usb", "res", null, null);
            jsonHead.put("usbStatus", usbState);
            LogUtil.i("NICK", "inUSB: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }

    /**
     * API-3.3.2
     * 上报食物状态   food = 3｜2｜1 高,中,低
     * {200: {"module":"feed", "type":"res", "action":"food", "foodStatus":2, "values":{"food":1}}}
     */
    public static void food(int foodStatus) {
        JSONObject values = new JSONObject();
        try {
            values.put("food", foodStatus);
            JSONObject jsonHead = getJsonHead("feed", "res", "food", values);
            jsonHead.put("foodStatus", foodStatus);
            LogUtil.i("NICK", "food: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }

    /**
     * API-3.3.4
     * 录音设置成功
     * {200: {"module":"feed", "type":"res", "action":"record", "values":{"status":"success", "message":"ok"}}}
     */
    public static void record(String status, String message) {
        JSONObject values = new JSONObject();
        try {
            values.put("status", status);
            values.put("message", message);
            JSONObject jsonHead = getJsonHead("feed", "res", "record", values);
            LogUtil.i("NICK", "record: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }

    }


    /**
     * API-3.3.5
     * 喂食 是否成功
     * {200: {"module":"feed", "type":"res", "action":"hasfeed", "feedingResult":"success"|"failure"}}
     */
    public static void hasFeeded(String result) {
        try {
            JSONObject jsonHead = getJsonHead("feed", "res", "hasfeed", null);
            jsonHead.put("feedingResult", result);
            LogUtil.i("NICK", "hasFeeded: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }

    /**
     * API-3.3.5
     * 喂食状态上报  注意没有action
     * {200: {"module":"feed", "type":"res", "feedStatus":"success||failed "}}
     */
    public static void isFeedPet(String result) {
        try {
            JSONObject jsonHead = getJsonHead("feed", "res",/*"isfeed"*/ null, null);
            jsonHead.put("feedStatus", result);
            LogUtil.i("NICK", "isFeedPet: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }

    }

    /**
     * 传视频文件失败，通知ac
     *
     * @param state
     */
    public static void uploadVideoFileResult(String state) {
        try {
            JSONObject jsonHead = getJsonHead("monitor", "res","file", null);
            jsonHead.put("fileStatus", state);
            LogUtil.i("NICK", "uploadVideoFileResult: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }

    }

    /**
     * API-3.4.2
     * 云台转向限位 上报
     * <-- {70: {"module":"head", "type":"res","action":"limit", "headStatus":"rightLimit"}}
     */
    public static void reachLimit(String text) {
        try {
            JSONObject jsonHead = getJsonHead("head", "res","limit", null);
            jsonHead.put("headStatus", text);
            LogUtil.i("NICK", "reachLimit: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }


    /**
     * usb 空间大小信息上报
     * {"module":"usb", "type":"res", "action":"size", "values":{"sizeValue":"U盘XXGB可用，共XXGB"}}
     */
    public static void usbSize(String sizeValue) {
        JSONObject values = new JSONObject();
        try {
            values.put("sizeValue", sizeValue);
            JSONObject jsonHead = getJsonHead("usb", "res","size", values);
            LogUtil.i("NICK", "usbSize: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }
    }

    /**
     * API-2.1.7
     * 媒体（音乐）音量上报
     * <-- {70: "module":"music", "type":"res", "action":"musicVolume"}, "values":{"volume":3}}}
     */
    public static void mediaVolume(int musicVolume) {
        JSONObject values = new JSONObject();
        try {
            values.put("volume", musicVolume);
            JSONObject jsonHead = getJsonHead("music", "res", "musicVolume", values);
            LogUtil.i("NICK", "mediaVolume: " + jsonHead.toString());
            reportData(jsonHead);
        } catch (JSONException e) {
        }

    }
}
