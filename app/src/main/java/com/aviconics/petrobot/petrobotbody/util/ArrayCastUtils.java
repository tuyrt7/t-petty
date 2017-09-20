package com.aviconics.petrobot.petrobotbody.util;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by win7 on 2016/6/24.
 */
public class ArrayCastUtils {

    public static List<String> intArrayToString(JSONArray jsonArray) throws JSONException {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            int a = (int) jsonArray.get(i);
            switch (a) {
                case 1:
                    list.add("星期一");
                    break;
                case 2:
                    list.add("星期二");
                    break;
                case 3:
                    list.add("星期三");
                    break;
                case 4:
                    list.add("星期四");
                    break;
                case 5:
                    list.add("星期五");
                    break;
                case 6:
                    list.add("星期六");
                    break;
                case 7:
                    list.add("星期日");
                    break;
                default:
                    break;
            }
        }
        return list;
    }
}
