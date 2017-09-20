package com.aviconics.petrobot.petrobotbody.mvp.model.db.filter;

import java.io.File;
import java.io.FilenameFilter;

/**
 * 音乐文件 过滤器
 *  支持 MP3 WMA FLAC 格式
 */
public class MusicFilter implements FilenameFilter {

    @Override
    public boolean accept(File dir, String name) {
       /* 指定扩展名类型 .mp3，可以加其他的音乐格式*/
        return  name.endsWith(".mp3") || name.endsWith(".wma") ||name.endsWith(".flac");
    }
}
