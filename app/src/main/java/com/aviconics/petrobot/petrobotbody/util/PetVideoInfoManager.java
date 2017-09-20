package com.aviconics.petrobot.petrobotbody.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;

import com.accloud.clientservice.AC;
import com.accloud.clientservice.PayloadCallback;
import com.accloud.common.ACException;
import com.accloud.common.ACMsg;
import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.configs.ConstantValue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Created by win7 on 2016/8/11.
 */
public class PetVideoInfoManager {

    private static String TAG = "SendToService:";
    private static String allVideoInfo;


    /**
     * 发送本地所有视频数据 易发生ANR（当本地没有的视频列表时，调用ok）
     *
     * @param ctx
     */
    public static void sendVideoAllInfoWhenNoList(Context ctx) {
        sendACMsg2Service(getAllVideoInfo(ctx));
    }

    /**
     * 发送单个本地视频数据
     *
     * @param videoName
     * @param thumbName
     * @param coverFiles
     * @param duration
     * @param size
     * @param doWhat
     */
    public static void sendVideoSingleInfo(String videoName, String thumbName, List<String> coverFiles, long duration, long size, String doWhat) {
        sendACMsg2Service(getSingleVideoMsg(videoName, thumbName, coverFiles, duration, size, doWhat));
    }

    /**
     * main thread 执行
     *
     * @param json
     */
    public static void sendACMsg2Service(String json) {
        String domain = "petbot";
        ACMsg req = null;
        try {
            String packACMsg = getPackACMsg(json);
            LogUtil.i("NICK", "sent to AC, video  msg :" + packACMsg);
            req = new ACMsg(packACMsg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        req.setName("service");
        int version = 1;
        AC.sendToService(domain, version, req, new PayloadCallback<ACMsg>() {
            @Override
            public void success(ACMsg acMsg) {
                LogUtil.i(TAG, "send videoInfo success!");
            }

            @Override
            public void error(ACException e) {
                LogUtil.i(TAG, "error,msg=" + e.getMessage() + ",des :" + e.getDescription());
            }
        });

//        ACMsg req = getACMsgJson(ctx);
//        req.setName(name);
//        InputStream in = new ByteArrayInputStream(LocalRecordHelper.getListJson(ctx).getBytes());
//        req.setStreamPayload(in,1);
//        AC.sendToService(subDomain, serviceName, req, new PayloadCallback<ACMsg>() {
//            @Override
//            public void success(ACMsg resp) {
//                //发送成功并接收服务的响应消息
//            }
//
//            @Override
//            public void error(ACException e) {
//                //网络错误或其他，根据e.getErrorCode()做不同的提示或处理，此处一般为传递的参数或UDS云端问题，可到AbleCloud平台查看log日志
//            }
//        });
    }


    private static String getPackACMsg(String jsonStr) {
        JSONObject json1 = new JSONObject();
        JSONObject json2 = new JSONObject();
        try {
//            json1.put("name","service"); //名称在 外面, req.setName(name);
            json1.put("action", "actiondatas");
            json1.put("method", "report");
            json1.put("ver", "v1");

            JSONObject json3;
            if (!TextUtils.isEmpty(jsonStr)) {
                json3 = new JSONObject(jsonStr);
                json2.put("action_data", json3);
            } else json2.put("action_data", new JSONObject().toString());
            json1.put("params", json2);
        } catch (JSONException e) {
            LogUtil.d("NICK", "json 解析 error:" + e.getMessage());
            return new JSONObject().toString();
        }
        return json1.toString();
    }


    /**
     * 最好在子线程执行（文件较多时，创建缩略图耗时）
     *
     * @param context
     * @return
     */
    public static String getAllVideoInfo(Context context) {
        List<String> recordList = LocalRecordHelper.getRecordList(App.getUsbHelper().getPetVideoDir());
        JSONObject jsonObject = new JSONObject();
        JSONObject bodyObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        try {
            jsonObject.put("physicalDeviceId", DeviceUtil.getMacAddress());
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
//                    Log.d("NICK", "string---:" + string);
                    long videoFileSize = 0;
                    long videoTimeSize = 0;
                    String thumbName = "";
                    if (split.length != 3) {
                        if (videoFile.exists() && videoName.endsWith(".mp4")) {
//                        LogUtil.d("NICK", "如果没有获取sp中的值，再次创建缩略图，存sp");
                            //防止手动改了文件名，不合规范的直接删除文件
                            try {
                                thumbName = videoName.substring(0, 14) + ".png";
                            } catch (RuntimeException ex) {
                                videoFile.delete();
                                continue;
                            }

                            videoTimeSize = getVideoDuration(videoFile.getAbsolutePath());
                            //以 视频文件名为key，以 缩略图，文件大小，时长 为value  本地存视频文件信息
                            SharedPrefs.putString(context, videoName, thumbName + "," + videoFileSize + "," + videoTimeSize);
                           /* if (!new File(RobotApp.getUsbHelper().getPetThumbDir() + "/" + thumbName).exists() && videoFileSize > 1024) {
                                ThumbUtils.bitmapWriteAnd2Bytes(context, videoFile.getAbsolutePath(), thumbName);
                            }*/
                        }
                    } else {
                        thumbName = split[0];
                        videoFileSize = Long.parseLong(split[1]);
                        videoTimeSize = Long.parseLong(split[2]);
                    }

                    videoFileSize = videoFile.length() / 1024;//视频文件 小于 1 M,清除，不上报
                    if (videoFileSize < 1024 || !checkMp4CanPlay(videoFile.getAbsolutePath())) { //文件小于 1 M 或者 已损坏
                        videoFile.delete();
                        File thumbFile = new File(App.getUsbHelper().getPetThumbDir() + "/" + thumbName);
                        if (thumbFile.exists()) {
                            thumbFile.delete();
                        }
                        SharedPrefs.remove(context, videoName);
                        continue;
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
            LogUtil.d("NICK", "解析失败," + ex.getMessage());
            return "";
        }
//        LogUtil.d("NICK",jsonObject.toString());
        return jsonObject.toString();
    }

    public static String getSingleVideoMsg(String videoName, String thumbName, List<String> coverFiles, long duration, long size, String doWhat) {
        JSONObject jsonObject = new JSONObject();
        JSONObject bodyObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        try {
            jsonObject.put("physicalDeviceId", DeviceUtil.getMacAddress());
            jsonObject.put("module", "monitor");
            jsonObject.put("type", "res");
            jsonObject.put("action", "file");

            bodyObject.put("doWhat", doWhat);
            bodyObject.put("addFile", videoName);
            bodyObject.put("file", videoName);
            bodyObject.put("thumb", thumbName);
            bodyObject.put("bucket", ConstantValue.bucket);
            bodyObject.put("size", size);
            bodyObject.put("duration", duration);

            if (coverFiles != null && coverFiles.size() > 0) {
                for (String coverFile : coverFiles) {
                    jsonArray.put(coverFile);
                }
            }
            bodyObject.put("coverFiles", jsonArray);

            jsonObject.put("values", bodyObject);
        } catch (JSONException ex) {
            LogUtil.d("NICK", "解析失败" + ex.getMessage());
            return null;
        }
        return jsonObject.toString();
    }


    /**
     * 移除SP 中的视频文件信息
     *
     * @param context
     */
    public static void removeVideoInfoInSP(Context context) {
        if (App.getUsbHelper().isUsbEnable()) {
            List<String> recordList = LocalRecordHelper.getRecordList(App.getUsbHelper().getPetVideoDir());
            if (recordList != null && recordList.size() > 0) {
                for (String videoName : recordList) {
                    if (SharedPrefs.contains(context, videoName))
                        SharedPrefs.remove(context, videoName);
                }
            }
        }
    }


    /**
     * 检查视频文件是否损坏 （在子线程执行）
     *
     * @param path 视频文件路径
     * @return
     */
    public static boolean checkMp4CanPlay(String path) {
        MediaMetadataRetriever retr = new MediaMetadataRetriever();
        try {
            retr.setDataSource(path); //耗时操作
            String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (TextUtils.isEmpty(height)) {
                return false;
            }
        } catch (IllegalArgumentException ex) {
            // Assume this is a corrupt video file
            return false;
        } catch (RuntimeException ex) {
            // Assume this is a corrupt video file.
            return false;
        } finally {
            try {
                retr.release();
            } catch (RuntimeException ex) {
                // Ignore failures while cleaning up.
                return false;
            }
        }
        return true;
    }


    /**
     * 获取视频的时间长度
     *
     * @param path
     * @return
     */
    public static long getVideoDuration(String path) {
       if(new File(path).exists()) {
           return 0;
       }
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();

            if (Build.VERSION.SDK_INT >= 14)
                mmr.setDataSource(path, new HashMap<String, String>());
            else
                mmr.setDataSource(path);
            String s = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (s != null) {
                return Long.parseLong(s);
            }
        } catch (IllegalStateException e) {
            return 0;
        } finally {
            if (mmr != null) {
                mmr.release();
            }
        }
        return 0;
    }
}
