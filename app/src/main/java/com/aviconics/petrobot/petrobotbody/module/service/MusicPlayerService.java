package com.aviconics.petrobot.petrobotbody.module.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.GreenDaoManager;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile;
import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;
import com.aviconics.petrobot.petrobotbody.util.SignDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MusicPlayerService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener {

    public MediaPlayer mMediaPlayer;/* 定于一个多媒体对象 */
    private String mPath;//音乐路径

    public int mCurrentItem = -1; //定义在播放当前选择项
    private int mType;//音乐类型 1外置 2内置
    private List<MediaFile> mUsbMusicList; //内置歌曲列表
    private List<MediaFile> mSdMusicList; //外置歌曲列表
    private List<MediaFile> mCurMusicList = new ArrayList<>(); //当前歌曲列表

    private int MSG_COM; // 用户操作
    public static final int PLAY = 1;//开始播放
    public static final int PAUSE = 2;//暂停播放
    public static final int STOP = 3;//停止播放
    public static final int NEXT = 4;//上一曲播放
    public static final int LAST = 5;//下一曲播放

    private boolean isPause;//喂食口令使音乐暂停的标记
    private Handler mHandler;

    //播放模式
    public static final int CYCLE_PLAY = 11;//循环播放
    public static final int ONCE_PLAY = 12;//单曲播放
    public static final int RANDOM_PLAY = 13;//随机播放

    private Random mRandom;
    private boolean isPrepared;

    private ControlMusicReceiver musicReceiver = new ControlMusicReceiver();
    /* 广播action */
    public static final String PAUSE_MUSIC_ACTION = "com.aviconics.petrobot.petrobotbody.pause_music";
    public static final String RESUME_MUSIC_ACTION = "com.aviconics.petrobot.petrobotbody.resume_music";
    public static final String STOP_MUSIC_ACTION = "com.aviconics.petrobot.petrobotbody.stop_music";
    public static final String UPDATE_MUSIC_LIST_ACTION = "com.aviconics.petrobot.petrobotbody.update_music_list";
    public static final String STOP_MUSIC_SERVICE = "com.aviconics.petrobot.petrobotbody.stop_music_service";

    class ControlMusicReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case PAUSE_MUSIC_ACTION:
                    LogUtil.d("NICK", "-----PAUSE_MUSIC_ACTION---------");
                    pausePlay();
                    break;
                case RESUME_MUSIC_ACTION:
                    LogUtil.d("NICK", "-----RESUME_MUSIC_ACTION---------");
                    resumePlay();
                    break;
                case STOP_MUSIC_ACTION:
                    LogUtil.d("NICK", "-----STOP_MUSIC_ACTION---------");
                    if (SignDevice.getSign().isPlayMusic()) {// 正在播放
                        stopPlay();
                        hideMusicUI();
                    }
                    break;
                case STOP_MUSIC_SERVICE:
                    LogUtil.d("NICK", "-----STOP_MUSIC_ACTION---------");
                    if (SignDevice.getSign().isPlayMusic()) {// 正在播放
                        stopPlay();
                        reportMusicStop();
                    }
                    stopSelf();
                    break;
                case UPDATE_MUSIC_LIST_ACTION:
                    initData();
                    break;
                default:
                    break;
            }
        }
    }

    private void registerMusicReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(PAUSE_MUSIC_ACTION);
        filter.addAction(RESUME_MUSIC_ACTION);
        filter.addAction(STOP_MUSIC_ACTION);
        filter.addAction(UPDATE_MUSIC_LIST_ACTION);
        filter.addAction(STOP_MUSIC_SERVICE);
        registerReceiver(musicReceiver, filter);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mMediaPlayer = new MediaPlayer();
        mRandom = new Random();
        mHandler = new Handler(getMainLooper());
        registerMusicReceiver();
        initData();
    }

    private void initData() {
        mSdMusicList = GreenDaoManager.getInstance().getMusicSd();
        mUsbMusicList = GreenDaoManager.getInstance().getMusicUsb();
    }


    /*启动service时执行的方法*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*得到从startService传来的动作，后是默认参数，这里是我自定义的常量*/
        if (intent != null) {
            MSG_COM = intent.getIntExtra("MSG_COM", PLAY);  //动作指令
            switch (MSG_COM) {
                case PLAY:
                    mType = intent.getIntExtra("TYPE_COM", MediaFile.TYPE_MUSIC_USB);  //播放列表类型
                    mPath = intent.getStringExtra("URL_COM");  //音乐路径
                    if (mMediaPlayer != null && mMediaPlayer.isPlaying()
                            && mPath.equals(getCurMusicUrl())) {
                        LogUtil.e("NICK","收到播放正在playing不响应");
                        break;
                    }
                    mCurrentItem = getUrlPositionInList();
                    if (mCurrentItem >= 0) {

                        playMusic();
                    } else {
                        LogUtil.e("NICK", "不应该发生这种情况"); //url未找到
                        stopSelf();
                    }
                    break;

                case PAUSE:
                    pausePlay();
                    break;

                case STOP:
                    if (SignDevice.getSign().isPlayMusic()) {// 正在播放
                        keepMusicToSp();
                        stopPlay();

                        reportMusicStop();
                        showPressUI();
                    }
                    break;
                case NEXT:
                    int newType = intent.getIntExtra("TYPE_COM", MediaFile.TYPE_MUSIC_USB);
                    if (mType != newType) {
                        //切换列表下一曲，播放此列表第0首
                        mCurrentItem = -1;
                        mType = newType;
                    }
                    LogUtil.i("NICK", "next=" + newType);
                    next();
                    break;
                case LAST:
                    newType = intent.getIntExtra("TYPE_COM", MediaFile.TYPE_MUSIC_USB);
                    if (mType != newType) {
                        //切换列表上一曲，播放此列表第0首
                        mCurrentItem = 1;
                        mType = newType;
                    }
                    last();
                    break;
                default:
                    break;
            }
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        closeSpeaker();

        releaseMediaPlayer();

        hideMusicUI();

        unregisterReceiver(musicReceiver);
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            if (SignDevice.getSign().isPlayMusic()) {
                try {
                    mMediaPlayer.stop();
                } catch (IllegalStateException e) {
                }
                SignDevice.getSign().setPlayMusic(false);
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        start();
        SignDevice.getSign().setPlayMusic(true);


        openSpeaker();
        reportMusicPlay();
        showMusicUI();
    }

    @Override
    public boolean onError(MediaPlayer mp, final int what, int extra) {
        stopSelf();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (getPlayMode() == ONCE_PLAY) {
            //单曲循环的，重新定位到0位置,并继续播放
            mMediaPlayer.seekTo(0);
            mMediaPlayer.start();
        } else {
            //歌曲播放完成自动播放下一曲
            next();
        }
    }

    private void start() {
        if (mMediaPlayer != null && isPrepared) {
            if (getLastMusicUrlFromSp().equals(mCurMusicList.get(mCurrentItem).getUrl())) {
                int lastPos = getLastMusicPositionFromSp();
                if (lastPos < 0) {
                    lastPos = 0;
                } else if (lastPos > mMediaPlayer.getDuration()) {
                    lastPos = mMediaPlayer.getDuration();
                }
                mMediaPlayer.seekTo(lastPos);
            } else {
                removeStopMusicFromSp();
            }

            mMediaPlayer.start();
        }
    }

    public void playMusic() {
        try {
            mMediaPlayer.reset();  /* 重置多媒体 */
            isPrepared = false;
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(this, Uri.parse(mCurMusicList.get(mCurrentItem).getUrl()));/* 读取mp3文件 */
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.prepareAsync();/* 准备播放 */
        } catch (Exception e) {
            stopSelf();
        }

    }

    /**
     * 下一曲
     */
    public void next() {
        if (mType == MediaFile.TYPE_MUSIC_USB) {
            mCurMusicList = mUsbMusicList;
        } else {
            mCurMusicList = mSdMusicList;
        }
        //更具播放模式确定播放列表下一首 item
        switch (getPlayMode()) {
            case CYCLE_PLAY:
            case ONCE_PLAY:
                //对于列表循环和单曲循环下一曲 +1
                mCurrentItem++;
                if (mCurrentItem > mCurMusicList.size() - 1) {
                    //越界检测
                    mCurrentItem = 0;
                }
                break;
            case RANDOM_PLAY:
                //随机下一曲
                if (mCurMusicList.size() > 0) {
                    mCurrentItem = mRandom.nextInt(mCurMusicList.size());
                }
                break;
        }
        playMusic();
    }

    /**
     * 上一曲
     */
    public void last() {
        if (mType == MediaFile.TYPE_MUSIC_USB) {
            mCurMusicList = mUsbMusicList;
        } else {
            mCurMusicList = mSdMusicList;
        }

        //只有从列表中进来的才能播放下一首，网络资源只能播放一次
        switch (getPlayMode()) {
            case CYCLE_PLAY:
            case ONCE_PLAY:
                //对于列表循环和单曲循环上一曲 +1
                mCurrentItem--;
                if (mCurrentItem < 0) {
                    //越界检测
                    mCurrentItem = mCurMusicList.size() - 1;
                }
                break;
            case RANDOM_PLAY:
                //随机下一曲
                mCurrentItem = mRandom.nextInt(mCurMusicList.size());
                break;
        }
        playMusic();
    }


    private void pausePlay() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {//正在播放
            mMediaPlayer.pause();
            isPause = true;
        }
    }

    private void resumePlay() {
        if (isPause) {
            mMediaPlayer.start();
            isPause = false;
        }
    }


    private void stopPlay() {
        closeSpeaker();
        try {
            mMediaPlayer.stop();
        }catch (Exception e){}
        isPause = false;
        SignDevice.getSign().setPlayMusic(false);
    }


    private void showMusicUI() {
       // if (VideoCallActivity.getVideoCall() == null && CaptureActivity.getCapture() == null)
        //    EventBus.getDefault().post(new UIEvent(MainActivity.UI_MUSIC_START));
    }

    private void reportMusicPlay() {
        //EventBus.getDefault().post(new StateReportEvent(MainActivity.MUSIC_PLAY_TYPE, mCurMusicList.get(mCurrentItem).getUrl()));
    }

    private void reportMusicStop() {
       // EventBus.getDefault().post(new StateReportEvent(MainActivity.MUSIC_STOP_TYPE, ""));
    }

    private void showPressUI() {
        //if (VideoCallActivity.getVideoCall() == null && CaptureActivity.getCapture() == null)
           // EventBus.getDefault().post(new UIEvent(MainActivity.UI_EXPRESS_SHOW));
    }

    private void hideMusicUI() {
        //EventBus.getDefault().post(new UIEvent(MainActivity.UI_MUSIC_STOP));
    }

    private void openSpeaker() {
       // ControlUtil.getInstance().playMusic();//调用底层打开音乐播放端口
    }

    private void closeSpeaker() {
        if (!SignDevice.getSign().isCallByUser()) {
            //ControlUtil.getInstance().stop_music();//调用底层关闭音乐播放端口
        }
    }


    public int getPlayMode() {
        String mode = SharedPrefs.getString(App.getContext(), "music_play_mode", "cycle");
        if (mode.equals("once")) {
            return ONCE_PLAY;
        } else if (mode.equals("random")) {
            return RANDOM_PLAY;
        } else {
            return CYCLE_PLAY;
        }
    }


    /**
     * 获取url在当前列表中播放的位置 position
     *
     * @return
     */
    public int getUrlPositionInList() {
        if (mType == MediaFile.TYPE_MUSIC_USB) {
            mCurMusicList = mUsbMusicList;
        } else {
            mCurMusicList = mSdMusicList;
        }
        if (mCurMusicList != null && mCurMusicList.size() > 0) {
            for (int i = 0; i < mCurMusicList.size(); i++) {
                MediaFile mediaFile = mCurMusicList.get(i);
                if (mPath.equals(mediaFile.getUrl())) {
                    return i;
                }
            }
        }
        LogUtil.e("NICK", "--------音乐不在当前列表中---------type=" + mType);
        return -1;
    }

    private static final String MUSIC_LAST_URL = "music_last_url";
    private static final String MUSIC_LAST_POSITION = "music_last_position";
    private void keepMusicToSp() {
        try {
            int curPosition = mMediaPlayer.getCurrentPosition();
            String curMusicUrl = mCurMusicList.get(mCurrentItem).getUrl();
            SharedPrefs.putInt(App.getContext(),MUSIC_LAST_POSITION, curPosition);
            SharedPrefs.putString(App.getContext(), MUSIC_LAST_URL, curMusicUrl);
        } catch (Exception e) {
        }
    }

    private String getLastMusicUrlFromSp() {
       return SharedPrefs.getString(App.getContext(), MUSIC_LAST_URL, "");
    }

    private int getLastMusicPositionFromSp() {
        return SharedPrefs.getInt(App.getContext(), MUSIC_LAST_POSITION, 0);
    }

    private void removeStopMusicFromSp() {
         SharedPrefs.remove(App.getContext(), MUSIC_LAST_URL);
         SharedPrefs.remove(App.getContext(), MUSIC_LAST_POSITION);
    }

    private String getCurMusicUrl() {
        String url;
        try {
            url=  mCurMusicList.get(mCurrentItem).getUrl();
        } catch (Exception e) {
            return  "";
        }
        return url;
    }

}