package com.aviconics.petrobot.petrobotbody.module.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.configs.AcConfig;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.Event;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventBusUtil;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.EventCode;
import com.aviconics.petrobot.petrobotbody.mvp.model.eventbus.data.ReportEvent;
import com.aviconics.petrobot.petrobotbody.util.CameraMonitorHelper;
import com.aviconics.petrobot.petrobotbody.util.CameraTimeHelper;
import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;

import java.util.ArrayList;
import java.util.Date;

/**
 * 声音监控 1\2\3 高中低
 */
public class SoundMonitorService extends Service {

    private static final String TAG = "SoundMonitor";

    private static final int SAMPLE_RATE_IN_HZ = 8000;
    private  final int DEFAULT_DB = 5;//分贝差（可设置成三种模式即:高、中、低）
    private  final int HIGH_DB = 8;//高
    private  final int LOW_DB = 3;//低
    private  int db_diff = DEFAULT_DB;

    private double TRIGGER_DB = 70.0;//触发分贝
    private double oldAverageDB = 0.0;
    private int number = 30;

    private volatile boolean isMonitoring;
    private AudioRecord mAudioRecord;
    private AudioRecordThread mAudioRecordThread;
    private Handler handler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //从sp 中获取声音监控的质量
        //getSwitchDB(SharedPrefs.getInt(getApplicationContext(), "sound_quality", 2));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isMonitoring = true;
        number = 30;
        if (mAudioRecordThread == null) {
            mAudioRecordThread = new AudioRecordThread();
            mAudioRecordThread.start();
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        LogUtil.d("NICK", "--close--sound-monitor--");
        isMonitoring = false;
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
        super.onDestroy();
    }


    private void getSwitchDB(int quality) {
        switch (quality) {
            case 1:
                db_diff = LOW_DB;
                break;
            case 2:
                db_diff = DEFAULT_DB;
                break;
            case 3:
                db_diff = HIGH_DB;
                break;
            default:
                break;
        }
    }

    private class AudioRecordThread extends Thread {

        AudioRecordThread() {
            setName("AudioRecordThread");
        }

        @Override
        public void run() {
            super.run();
            try {
                int mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
                        AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
                short[] audioData = new short[mBufferSize];
                ArrayList<Double> dbList = new ArrayList<>();

                while (isMonitoring) {
                    if (mAudioRecord == null || mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                                AudioFormat.ENCODING_PCM_16BIT, mBufferSize);
                    }
                    if (mAudioRecord != null && mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                        // 大概一秒十次
                        if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
                            mAudioRecord.startRecording();
                        }
                        SystemClock.sleep(100);
                        //r是实际读取的数据长度，一般而言r会小于buffersize
                        int r = mAudioRecord.read(audioData, 0, mBufferSize);
                        long v = 0;
                        // 将 buffer 内容取出，进行平方和运算
                        for (int i = 0; i < audioData.length; i++) {
                            v += audioData[i] * audioData[i];
                        }
                        // 平方和除以数据总长度，得到音量大小。
                        double mean = v / (double) r;
                        double volume = 10 * Math.log10(mean);

                        if (number == 20) {
//                            LogUtil.d("NICK", "当前环境音量：" + volume);
                        }
                        dbList.add(volume);

                        if (dbList.size() == number) {
                            if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                                mAudioRecord.stop();
                            }
                            SystemClock.sleep(1000);

                            if (oldAverageDB == 0.0) {
                                LogUtil.d("NICK","-------getAverageDB-------------");
                                oldAverageDB = getAverageDB(dbList);
                                if (oldAverageDB != 0.0) number = 20;
                            }
                        }

                        //播放音乐和视频不计算监控触发
                        /*if (SignDevice.getSign().isPlayMusic() || App.findActivity(VideoPlayActivity.class) != null) {
                            oldAverageDB = 0.0;
                            number = 30;
                            continue;
                        }*/

                        //2s 取样20次，有2次超过60算触发 (抛弃第一次检测结果)
                        if (oldAverageDB != 0.0  && isWarning(dbList)) {
                            //达到预警值，判断时间域内是否录制（所有的定时都关或者有定时的定时区域内）
                            CameraTimeHelper helper = new CameraTimeHelper(SoundMonitorService.this.getApplicationContext());
                            if (helper.isInMultiCameraMonitorTime(new Date())) {
                                isMonitoring = false;
                                LogUtil.d(TAG, "报警，停止监控");
                                ReportEvent reportEvent = new ReportEvent(AcConfig.Type.SOUND_TYPE, "");
                                EventBusUtil.sendEvent(new Event(EventCode.REPORT,reportEvent));
                                CameraMonitorHelper.openRecorder(handler);
                            } else
                                LogUtil.d("NICK", "当前时间，不允许录制视频");

                        }
                    }

                }
            } catch (Exception e) {
               stopSelf();
            }/* finally {
                isMonitoring = false;
                if (mAudioRecord != null) {
                    //   mAudioRecord.stop(); release方法中已经包含stop方法了
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
                mAudioRecordThread = null;
            }*/
        }

        /**
         * 判断是否发出警告
         *
         * @param dbList
         * @param volume
         * @return
         */
        private boolean isWarning(ArrayList<Double> dbList, double volume) {
            if (dbList != null) {
                if (dbList.size() == 20) {
                    double l = 0;
                    double r = 0;
                    for (int i = 0; i < 20; i++) {
                        if (i <= 9) {
                            l += dbList.get(i);
                        } else {
                            r += dbList.get(i);
                        }
                    }
                    dbList.clear();
                    double diff = Math.abs(l / 10 - r / 10);
                    int now_q = SharedPrefs.getInt(getApplicationContext(), "sound_quality", 2);
                    if (diff > db_diff) {
                        LogUtil.i("NICK", "当前声音检测识别度：" + now_q + ",触发分贝差值:" + diff);
                        return true;
                    }
                } else if (dbList.size() > 20) {
                    //不应该进入此条件
                    dbList.clear();
                }
            }
            return false;
        }


        /**
         * 判断是否发出警告
         *
         * @param dbList
         * @return
         */
        private boolean isWarning(ArrayList<Double> dbList) {
            if (dbList != null) {
                if (dbList.size() == 20) {
                    int l = 0;
                    int e = 0;
                    double r = 0;
                    double r20 = 0;//统计20次分贝和
                    for (int i = 0; i < 20; i++) {
                        r =  dbList.get(i);
                        r20 += r;
                        if (r - oldAverageDB > 5) {
                            l ++;
                        }

                        if (r > 75) {
                            e ++;
                        }
                    }
                    oldAverageDB = Math.abs( r20/20);
                    dbList.clear();

                    //LogUtil.d("NICK", "均值差" + l + "次,高分贝" + e + "次");
                    if (l >= 3 && e >= 2) {
                        return true;
                    }
                    if (l >= 8) {
                        return true;
                    }
                } else if (dbList.size() > 20) {
                    //不应该进入此条件
                    dbList.clear();
                }
            }
            return false;
        }

    }


    /**
     * 前3s 声音平均值
     *
     * @param dbList
     * @return
     */
    private Double getAverageDB(ArrayList<Double> dbList) {
        if (dbList != null) {
            if (dbList.size() == 30) {
                double r = 0;
                for (int i = 0; i < 30; i++) {
                    r += dbList.get(i);
                }
                dbList.clear();
                Double averageDB = Math.abs(r / 30);
                LogUtil.d("NICK", "前3s分贝均值：" + averageDB);
                return averageDB;
            } else if (dbList.size() > 30) {
                //不应该进入此条件
                dbList.clear();
            }
        }
        return 0.0;
    }


}
