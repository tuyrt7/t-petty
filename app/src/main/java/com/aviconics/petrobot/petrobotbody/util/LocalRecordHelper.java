package com.aviconics.petrobot.petrobotbody.util;

import android.content.Context;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.configs.ConstantValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地视频信息类
 */
public class LocalRecordHelper {

    /**
     *  获取视频文件清单
     * @return
     */
    public static List<String> getRecordList(String dirPath) {
        List<String> videoList = new ArrayList<>();
        File file = new File(dirPath);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null||files.length == 0 ) {
                return null;
            }
            for (File f : files) {
                if (f.exists() && !f.isDirectory() && f.getName().endsWith(".mp4"/*".3gp"*/)) {
                    String fileName = f.getName();
                    videoList.add(fileName);
                }
            }
            return videoList;
        }
        return null;
    }


    public static String getListJson(Context context) {
        List<String> recordList = LocalRecordHelper.getRecordList(App.getUsbHelper().getPetVideoDir());

        JSONObject jsonObject = new JSONObject();
        JSONObject bodyObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            jsonObject.put("module", "monitor");
            jsonObject.put("type", "res");
            jsonObject.put("action", "files");
            if (recordList == null || recordList.size() == 0) {
                bodyObject.put("total", 0);
                bodyObject.put("files", jsonArray);
            } else {
                bodyObject.put("total", recordList.size());
                for (int i = 0; i < recordList.size(); i++) {
                    JSONObject json1 = new JSONObject();
                    JSONObject json2 = new JSONObject();

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
                    if (split.length != 3 && videoFile.exists() && videoName.endsWith(".mp4")) {
//                        LogUtil.d("NICK", "如果没有获取sp中的值，再次创建缩略图，存sp");
                        try {
                            thumbName = videoName.substring(0, 14) + ".png";
                        } catch (RuntimeException ex) {
                            videoFile.delete();//防止手动改了文件名，不合规范的直接删除文件
                            continue;
                        }
                        videoFileSize = videoFile.length() / 1024;//视频文件大小
                        if (videoFileSize < 1024) { //文件小于1M
                            videoFile.delete();
                            continue;
                        }
                        videoTimeSize  = (int) (PetVideoInfoManager.getVideoDuration(videoFile.getAbsolutePath())/1000);
                        //以 视频文件名为key，以 缩略图，文件大小，时长 为value  本地存视频文件信息
                        SharedPrefs.putString(context, videoName, thumbName + "," + videoFileSize + "," + videoTimeSize);

                    } else {
                        thumbName = split[0];
                        videoFileSize = Long.parseLong(split[1]);
                        videoTimeSize = Long.parseLong(split[2]);
                    }
                    String bucket = ConstantValue.bucket;

                    json1.put("file", videoName);
                    json1.put("bucket", bucket);
                    json1.put("thumb", thumbName);
                    json1.put("size", videoFileSize);
                    json1.put("duration", videoTimeSize);

                    json2.put("file", json1);
                    jsonArray.put(json2);
                }
                bodyObject.put("files", jsonArray);
            }
            jsonObject.put("values", bodyObject);
        } catch (JSONException ex) {
            LogUtil.d("NICK","解析失败");
            return "";
        }
        return jsonObject.toString();
    }
}
