package com.aviconics.petrobot.petrobotbody.util;

import com.accloud.clientservice.AC;
import com.accloud.clientservice.PayloadCallback;
import com.accloud.common.ACException;
import com.accloud.common.ACMsg;
import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by futao on 2017/5/4.
 */

public class MediaUtil {

    public static void sendMediaToCloud(List<MediaFile> mediaFiles) {

        String domain = "petbot";
        ACMsg req = null;
        try {
            String packACMsg = getMediaParams(mediaFiles);
            LogUtil.i("NICK", "sent media msg to cloud:" + packACMsg);
            req = new ACMsg(packACMsg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        req.setName("service");
        int version = 1;
        AC.sendToService(domain, version, req, new PayloadCallback<ACMsg>() {
            @Override
            public void success(ACMsg acMsg) {
                LogUtil.i("NICK", "send media success!");
            }

            @Override
            public void error(ACException e) {
                LogUtil.i("NICK", "error,msg=" + e.getMessage() + ",des :" + e.getDescription());
            }
        });
    }

/** ----------------------------------------------------------------
    {
           "action": "actiondatas",
            "method": "reportMedia",
            "params": {
                "action_data": {
                    "action": "medias",
                    "module": "music",
                    "physicalDeviceId": "10061C5AADC2925B",
                    "type": "res",
                    "values": {
                        "medias": [
                            {
                            "duration": 120,
                            "name": "20160814144548.mp4",
                            "type": 1,
                            "url": "/sdcard/20160814144548.mp4"
                            }
                        ]
                }
            }
        },
        "ver": "v1"
    }
-------------------------------------------------------------------------*/
    private static String getMediaParams(List<MediaFile> mediaFiles) {
        JSONObject json1 = new JSONObject();
        JSONObject json2 = new JSONObject();
        JSONObject json3 = new JSONObject();
        JSONObject json4 = new JSONObject();

        Gson gson = new Gson();
        try {
            json1.put("action", "actiondatas");
            json1.put("method", "reportMedia");
            json1.put("ver", "v1");

            json3.put("action","medias");
            json3.put("module","music");
            json3.put("type","res");
            json3.put("physicalDeviceId", DeviceUtil.getMacAddress());
            if (mediaFiles != null && mediaFiles.size() > 0) {

                String medias = gson.toJson(mediaFiles);
                JSONArray jsonArray = new JSONArray(medias);
                json4.put("medias",jsonArray);
            } else json4.put("medias", new JSONArray());

            json3.put("values",json4);
            json2.put("action_data", json3);
            json1.put("params", json2);
        } catch (JSONException e) {
            LogUtil.d("NICK", "json 解析 error:" + e.getMessage());
            return new JSONObject().toString();
        }
        return json1.toString();
    }

}
