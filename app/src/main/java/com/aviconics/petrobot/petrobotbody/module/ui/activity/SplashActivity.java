package com.aviconics.petrobot.petrobotbody.module.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.R;
import com.aviconics.petrobot.petrobotbody.app.App;
import com.aviconics.petrobot.petrobotbody.configs.ConstantValue;
import com.aviconics.petrobot.petrobotbody.manager.SpManager;
import com.aviconics.petrobot.petrobotbody.module.service.MusicPlayerService;
import com.aviconics.petrobot.petrobotbody.module.service.SoundMonitorService;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.GreenDaoManager;
import com.aviconics.petrobot.petrobotbody.util.CameraMonitorHelper;
import com.aviconics.petrobot.petrobotbody.util.MyFileUtils;
import com.aviconics.petrobot.petrobotbody.util.PetVideoInfoManager;
import com.aviconics.petrobot.petrobotbody.util.Pop;
import com.aviconics.petrobot.petrobotbody.util.SharedPrefs;
import com.aviconics.petrobot.petrobotbody.util.ThreadUtil;
import com.aviconics.petrobot.petrobotbody.util.WifiUtil;
import com.aviconics.petrobot.petrobotbody.view.CustomVideoView;
import com.aviconics.petrobot.petrobotbody.zxing.activity.CaptureActivity;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.NetworkUtils;
import com.blankj.utilcode.util.ServiceUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;

/**
 * Created by futao on 2017/9/7.
 */

public class SplashActivity extends BaseActivity implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {

    @BindView(R.id.iv_img_splash)
    ImageView mIvImg;
    @BindView(R.id.tv_version)
    TextView mTvVersion;
    @BindView(R.id.cvv_sp_guide)
    CustomVideoView mCvvGuide;
    @BindView(R.id.rl_splash_container)
    RelativeLayout mRlContainer;

    private final long SPLASH_DISPLAY_LENGHT = 2000l;
    private DelayTask mDelay;
    private MediaController mMediaController;
    private int uriType = -1;
    private Uri mUri;
    private Uri hintDownUri;
    private Uri hintAddUri;
    private Uri scanQrCodeUri;
    private int REQUEST_CODE = 1;

    private class DelayTask implements Runnable {
        @Override
        public void run() {
            if (!SpManager.getInstance().getBindState()) {
                showGuideUI();
            } else {
                goToActivityAndEnd(MainActivity.class);
            }
        }
    }


    public class InitThread extends Thread {
        private String PRELOAD_SRC = Environment.getRootDirectory()+"/pet";
        private String PRELOAD_DEST = "/storage/sdcard0/preload";
        @Override
        public void run() {
            checkLastVideoCanPlay();//检查最后一次录制的视频文件，如损坏则删除
            writeOrder();//写喂食口令
            //复制内置文件到sd
            if (MyFileUtils.copyFolder(new File(PRELOAD_SRC), new File(PRELOAD_DEST))) {
                LogUtil.e("NICK", "has copy!");
            } else {
                LogUtil.e("NICK", "not copy");
            }
            GreenDaoManager.getInstance().initDbData();//初始化本地media

            mInitThread = null;
        }
    }

    @Override
    protected void beforeInit() {
        super.beforeInit();
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mRlContainer.setVisibility(View.GONE);

        mIvImg.setBackground(getSplashBg());
        mTvVersion.setText(AppUtils.getAppVersionName());
    }

    @Override
    protected void initData() {

        NetworkUtils.setWifiEnabled(true);

        openThreadInitDeviceData();//初始化默认喂食录音



        if (mDelay == null) {
            mDelay = new DelayTask();
            ThreadUtil.runInUIThread(mDelay, SPLASH_DISPLAY_LENGHT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @SuppressWarnings("ResourceType")
    private BitmapDrawable getSplashBg() {
        BitmapFactory.Options opt = new BitmapFactory.Options();

        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        //获取资源图片
        InputStream is = getApplicationContext().getResources().openRawResource(R.mipmap.splash_bg);
        Bitmap bitmap = BitmapFactory.decodeStream(is, null, opt);
        try {
            is.close();
        } catch (IOException e) {
        }

        return new BitmapDrawable(getApplicationContext().getResources(), bitmap);
    }

    private void initService() {
        LogUtil.d("NICK", "-------isPetServiceRunning--------");
        /*if (!AppManager.getAppManager().isPetServiceRunning(RobotApp.getContext(),
                Pet2HelperService.pet2helperServiceName)) {
            startService(new Intent(this, Pet2HelperService.class));
        }
*/
        //startService(new Intent(this, PrintWifiService.class));//test wifi 可用

        if (ServiceUtils.isServiceRunning(MusicPlayerService.class.getName())) {
            ServiceUtils.stopService(MusicPlayerService.class);
        }

        //        if (AppManager.getAppManager().isPetServiceRunning(this, ProWifiService.proWifiServiceName)) {
        //            stopService(new Intent(this, ProWifiService.class));
        //        }

        if (ServiceUtils.isServiceRunning(SoundMonitorService.class.getName())) {
            ServiceUtils.stopService(SoundMonitorService.class);
        }

        CameraMonitorHelper.closeMonitor();
    }

    private InitThread mInitThread;
    private void openThreadInitDeviceData() {
        if (mInitThread == null) {
            mInitThread = new InitThread();
            mInitThread.start();
        }
    }

    private void checkLastVideoCanPlay() {
        if (App.getUsbHelper().isUsbEnable()) {
            File videoDir = new File(App.getUsbHelper().getPetVideoDir());
            String[] list = videoDir.list();
            if (list == null || list.length == 0) {
                return;
            }
            File lastVideoFile = new File(videoDir, list[list.length - 1]);
            if (lastVideoFile.exists()) {
                boolean lastMp4CanPlay = PetVideoInfoManager.checkMp4CanPlay(lastVideoFile.getAbsolutePath());
                if (!lastMp4CanPlay) {
                    if (!lastVideoFile.delete()) {
                        LogUtil.d("pet_file", "error video file is not deleted,splash");
                    }
                    String lastThumbName = list[list.length - 1].substring(0, 14) + ".png";
                    File lastThumbFile = new File(App.getUsbHelper().getPetThumbDir(), lastThumbName);
                    if (lastThumbFile.exists()) {
                        if (!lastThumbFile.delete()) {
                            LogUtil.d("pet_file", "error thumb file is not deleted,splash");
                        }
                    }
                    SharedPrefs.remove(App.getContext(), list[list.length - 1]);
                }
            }
        }
    }

    private void writeOrder() {
        FileOutputStream fos = null;
        InputStream in = null;
        try {
            File file = MyFileUtils.mkFile(ConstantValue.voice_sys_dir, ConstantValue.system_voice_name);
            if (!file.exists()) {
                in = SplashActivity.this.getResources().openRawResource(R.raw.yaho);
                fos = new FileOutputStream(file);
                int len = -1;
                byte[] buffer = new byte[8 * 1024];
                while ((len = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
        } catch (Exception e) {
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void showGuideUI() {
        mRlContainer.setVisibility(View.VISIBLE);

        //mMediaController = new MediaController(this);
        //mCvvGuide.setMediaController(mMediaController);
        //mMediaController.setVisibility(View.GONE);
        mCvvGuide.setOnCompletionListener(this);
        mCvvGuide.setOnErrorListener(this);

        initUri();
        play();
    }

    private void play() {
        if (mCvvGuide != null && !mCvvGuide.isPlaying()) {
            // Play Video
            try {
                mCvvGuide.setVideoURI(mUri);
            } catch (Exception e) {
            }
            mCvvGuide.setOnPreparedListener(this);
        }
    }

    private void initUri() {
        hintDownUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ui_hint_down_apk);
        hintAddUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ui_hint_add);
        scanQrCodeUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.ui_scan_2_2);

        uriType = 1;
        mUri = hintDownUri;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if (mCvvGuide != null) {
            mCvvGuide.start();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        switch (uriType) {
            case 1:
                uriType = 2;
                mUri = hintAddUri;
                play();
                break;
            case 2:
                uriType = 3;
                mUri = scanQrCodeUri;
                play();
                break;
            case 3: // 去扫描
                Pop.showSafe("取扫码");
                Intent intent = new Intent(SplashActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE);//这里的REQUEST_CODE是我们定义的int型常量
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        goToActivityAndEnd(MainActivity.class);
        return true;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            Bundle mBundle = data.getExtras();
            boolean flag = false;
            if (mBundle != null) {
                String result = mBundle.getString("result");
                Pop.showSafe("扫描的结果是:" + result);

                String[] split = result.split(" ");
                if (split != null && split.length == 3) {
                    String name = "\"" + split[0] + "\"";
                    String password = "\"" + split[1] + "\"";
                    String appPhoneNum = split[2];
                    if (!SpManager.getInstance().getBindState()) {
                        SpManager.getInstance().setAppNumber(appPhoneNum);
                    }
                    saveWifiInfo(name, password);
                    flag = true;
                    if (WifiUtil.connect(App.getContext(), name, password)) {
                        goToActivityAndEnd(MainActivity.class);
                    }
                }
            }
            if (!flag) {
                Pop.showSafe("扫描信息不对，请使用对应LaiBo APP 生成二维码。");
            }
        } else {
            Pop.showSafe("扫描出错");
        }

    }

    private void saveWifiInfo(String name, String password) {
        SpManager.getInstance().setWifiName(name);
        SpManager.getInstance().setWifiPwd(password);
    }

    private void goToActivityAndEnd(Class clazz) {
        ActivityUtils.startActivity(SplashActivity.this, clazz);
        finish();
    }
}
