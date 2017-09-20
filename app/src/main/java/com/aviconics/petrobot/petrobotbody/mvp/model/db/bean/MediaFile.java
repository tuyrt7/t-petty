package com.aviconics.petrobot.petrobotbody.mvp.model.db.bean;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Unique;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by futao on 2017/9/8.
 */

@Entity
public class MediaFile {

    public static final int TYPE_MUSIC_USB = 1;//表示外置u盘音乐
    public static final int TYPE_MUSIC_SD = 2;//表示内置音乐
    public static final int TYPE_VIDEO_USB = 3;//外置视频
    public static final int TYPE_VIDEO_SD = 4;//内置视频

    @Id(autoincrement = true)
    private Long id;

    @Unique
    private String url;//对应路径

    private long duration;//时长 单位：s

    private String name;//媒体文件名称

    private int type;//媒体文件类型

    @Generated(hash = 497368446)
    public MediaFile(Long id, String url, long duration, String name, int type) {
        this.id = id;
        this.url = url;
        this.duration = duration;
        this.name = name;
        this.type = type;
    }

    @Generated(hash = 835756724)
    public MediaFile() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

}
