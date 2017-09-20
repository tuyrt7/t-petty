package com.aviconics.petrobot.petrobotbody.mvp.model.db.gen;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile;

import com.aviconics.petrobot.petrobotbody.mvp.model.db.gen.MediaFileDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig mediaFileDaoConfig;

    private final MediaFileDao mediaFileDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        mediaFileDaoConfig = daoConfigMap.get(MediaFileDao.class).clone();
        mediaFileDaoConfig.initIdentityScope(type);

        mediaFileDao = new MediaFileDao(mediaFileDaoConfig, this);

        registerDao(MediaFile.class, mediaFileDao);
    }
    
    public void clear() {
        mediaFileDaoConfig.clearIdentityScope();
    }

    public MediaFileDao getMediaFileDao() {
        return mediaFileDao;
    }

}
