package com.aviconics.petrobot.petrobotbody.mvp.model.db;

import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.gen.DaoMaster;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.gen.DaoSession;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.gen.MediaFileDao;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.Event;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventBusUtil;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventCode;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.MediaEvent;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;

import org.greenrobot.greendao.identityscope.IdentityScopeType;

import java.util.List;


public class GreenDaoManager {

    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private static GreenDaoManager mInstance; //单例

    private GreenDaoManager(){
        if (mInstance == null) {
            DaoMaster.DevOpenHelper devOpenHelper = new
                    DaoMaster.DevOpenHelper(App.getContext(), "media-db", null);//此处为自己需要处理的表
            mDaoMaster = new DaoMaster(devOpenHelper.getWritableDatabase());
            mDaoSession = mDaoMaster.newSession(IdentityScopeType.None);
        }
    }

    public static GreenDaoManager getInstance() {
        if (mInstance == null) {
            synchronized (GreenDaoManager.class) {//保证异步处理安全操作
                if (mInstance == null) {
                    mInstance = new GreenDaoManager();
                }
            }
        }
        return mInstance;
    }

    public DaoMaster getMaster() {
        return mDaoMaster;
    }
    public DaoSession getSession() {
        return mDaoSession;
    }
    public DaoSession getNewSession() {
        mDaoSession = mDaoMaster.newSession();
        return mDaoSession;
    }

    public void initDbData() {
        GreenDaoManager.getInstance().addAll(App.getUsbHelper().getAllMediaList());//所有media 插入数据库
        EventBusUtil.sendEvent(new Event(EventCode.MEDIA_LIST,new MediaEvent()));//发送media同步云标记
    }

    public List<MediaFile> getAllMedia() {
        return mDaoSession.getMediaFileDao().queryBuilder()
                .orderAsc().list();
    }

    public List<MediaFile> getMusicSd() {
        return mDaoSession.getMediaFileDao().queryBuilder()
                .where(MediaFileDao.Properties.Type.eq(MediaFile.TYPE_MUSIC_SD)).orderAsc().list();
    }

    public List<MediaFile> getMusicUsb() {
        return mDaoSession.getMediaFileDao().queryBuilder()
                .where(MediaFileDao.Properties.Type.eq(MediaFile.TYPE_MUSIC_USB)).orderAsc().list();
    }

    public List<MediaFile> getVideoSd() {
        return mDaoSession.getMediaFileDao().queryBuilder()
                .where(MediaFileDao.Properties.Type.eq(MediaFile.TYPE_VIDEO_SD)).orderAsc().list();
    }

    public List<MediaFile> getVideoUsb() {
        return mDaoSession.getMediaFileDao().queryBuilder()
                .where(MediaFileDao.Properties.Type.eq(MediaFile.TYPE_MUSIC_USB)).orderAsc().list();
    }
    /**
     * 添加数据，如果有重复则覆盖
     *
     * @param file
     */
    public void insertMedia(MediaFile file) {
         mDaoSession.getMediaFileDao().insertOrReplace(file);
    }

    public void addAll(List<MediaFile> list) {
        SignDevice.getSign().setCompleteMediaData(false);
        deleteAllMedia();

        if (list != null && list.size() > 0) {
            for (MediaFile f : list) {
                insertMedia(f);
            }
        }
        SignDevice.getSign().setCompleteMediaData(true);
    }

    /**
     * 删除全部
     */
    public void deleteAllMedia() {
        mDaoSession.getMediaFileDao().deleteAll();
    }
}
