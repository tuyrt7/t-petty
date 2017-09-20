package com.aviconics.petrobot.petrobotbody.util;

import android.content.Context;
import android.media.AudioManager;

import com.accloud.utils.LogUtil;

/**
 * Created by win7 on 2016/6/28.
 */
public class VolumeHelper {

    private static VolumeHelper volumeHelper ;
    private VolumeHelper(Context context){
        mContext = context;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public static VolumeHelper getInstance(Context context) {
        if (volumeHelper == null) {
            synchronized (VolumeHelper.class) {
                if (volumeHelper == null) {
                    volumeHelper = new VolumeHelper(context);
                }
            }
        }
        return volumeHelper;
    }
    private AudioManager mAudioManager;
    private Context mContext;
    private int curVolume = 0;


    /**
     *  当前音量和设置值超过2之后 每次加2
     * @param volume
     */
    public void setPetMusicVolume(int volume) {
        int count = 0 ;
        curVolume = getMusicVolume();
        if (volume > curVolume){
            while (Math.abs(volume - curVolume) > 2) {
                if (count>=5)   break;
                count ++;
                curVolume = curVolume + 1;
                if (curVolume <= 10) {
                    setMusicVolume(curVolume);
                }
            }
            setMusicVolume(volume);
        } else {
            while (Math.abs(volume - curVolume) > 2) {
                if (count>=5)   break;
                count ++;
                curVolume = curVolume - 2;
                setMusicVolume(curVolume);
            }
            setMusicVolume(volume);
        }
    }

    public void setMusicVolume(int volume) {
        int musicMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (volume > 10) {
            return;
        }

        double setVolume = Arith.mul(musicMaxVolume,Arith.div(volume,10,2));
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) setVolume, 0);
        LogUtil.d("NICK", "MUSIC当前的音量（1-10）:max= "+ musicMaxVolume + ",set=" + setVolume +",get="+ mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    public void setCallVolume(int volume) {
        int callMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
        double setVolume = Arith.mul(callMaxVolume,Arith.div(volume,10,2));
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, (int) setVolume, 0);
        LogUtil.d("NICK", "VOICE_CALL当前的音量（1-10）:max= "+callMaxVolume+",set=" + volume +",get="+ mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
    }

    public  void setSystemVolume(int volume) {
        int systemMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        double setVolume = Arith.mul(systemMaxVolume,Arith.div(volume,10,2));
        mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, (int) setVolume, 0);
        LogUtil.d("NICK", "SYSTEM当前的音量（1-10）:max= "+systemMaxVolume+",set=" + setVolume +",get="+ mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
    }

    public  void setMaxSystemVolume() {
        int systemMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM);
        mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, systemMaxVolume, 0);
    }


    public  int getMusicVolume() {
        int musicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int musicMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        double i = Arith.mul(10, Arith.div(musicVolume, musicMaxVolume, 2));
        LogUtil.d("NICK", "get MUSIC当前的音量（1-10）：get="+ i+",max="+musicMaxVolume);
        return (int) i;
    }

    public void setRingVolume(int volume) {
        int musicMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, musicMaxVolume * volume / 10, 0);
        LogUtil.d("NICK", "RING当前的音量（1-10）：" + volume);
    }

    //打开扬声器
    public void OpenSpeaker(int currVolume) {
        try {
            mAudioManager.setMode(AudioManager.ROUTE_SPEAKER);

            //获取当前通话音量
//             currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);

            if (!mAudioManager.isSpeakerphoneOn()) {
                mAudioManager.setSpeakerphoneOn(true);

                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                        AudioManager.STREAM_VOICE_CALL);

                LogUtil.d("NICK","---------扬声器打开了--------------");
            } else {
                mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume * currVolume/10,
                        AudioManager.STREAM_VOICE_CALL);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //关闭扬声器
    public void CloseSpeaker(int currVolume) {
        try {
            //获取当前通话音量
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            if (mAudioManager != null) {
                if (mAudioManager.isSpeakerphoneOn()) {
                    mAudioManager.setSpeakerphoneOn(false);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVolume * currVolume/10,
                            AudioManager.STREAM_VOICE_CALL);
                    LogUtil.d("NICK","---------扬声器关闭了--------------");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRotALARM(int volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
    }

    public void setRotNotifi(int volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, volume, 0);
    }

    public void setRotRing(int volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
    }


    public void setRotMusic(int volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    public void setRotCall(int volume) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, volume, 0);
    }
}
