/*
package com.tuyrt7.pet.util;

import android.content.Context;
import android.os.Environment;

import com.accloud.clientservice.AC;
import com.accloud.clientservice.PayloadCallback;
import com.accloud.common.ACException;
import com.accloud.common.ACOTAUpgradeInfo;
import com.accloud.service.ACOTAMgr;
import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.activity.MainActivity;
import com.aviconics.petrobot.petrobotbody.bean.event.UIEvent;
import com.aviconics.petrobot.petrobotbody.config.ConstantValue;
import com.blankj.utilcode.util.AppUtils;

import java.io.File;
import java.io.IOException;

import cn.mindpush.petrobot.controlboardcom.ControlBoardUtils;
import cn.mindpush.petrobot.controlboardcom.controlboardcom;
import de.greenrobot.event.EventBus;

*/
/**
 * Created by win7 on 2016/7/19.
 *//*

public class OtaUpdateHelper {

    private static ACOTAMgr otaMgr;
    private static Context mContext;
    private static OtaUpdateHelper mOtaUpdateHelper = new OtaUpdateHelper();


    private OtaUpdateHelper() {
    }

    public static OtaUpdateHelper getInstance(Context context) {
        mContext = context;
        otaMgr = AC.otaMgr();
        return mOtaUpdateHelper;
    }


    */
/**
     * 检查更新监听（本体只会是静默安装）
     *//*

    public void startCheckUpadte() {
        otaMgr.setOtaListener(new PayloadCallback<ACOTAUpgradeInfo>() {
            @Override
            public void success(ACOTAUpgradeInfo acotaUpgradeInfo) {
                LogUtil.d("NICK", "检测到升级更新信息：" + acotaUpgradeInfo.toString());
                String[] target = acotaUpgradeInfo.getTargetVersion().split("-");
                String[] old = AppUtils.getVersionName(mContext).split("\\.");

                boolean isUpdate = false;
                LogUtil.d("NICK", "new " + acotaUpgradeInfo.getTargetVersion() + "-----------old:" + AppUtils.getVersionName(mContext));
                try {
                    for (int i = 0; i < target.length; i++) {
                        if (Integer.parseInt(old[i]) < Integer.parseInt(target[i])) {
                            isUpdate = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    LogUtil.d("NICK", "apk 升级版本号错误");
                }

                if (isUpdate) {
                    //获取升级类型
                    if (acotaUpgradeInfo.getOtaMode() == 0) {
                        //静默升级
                        downFile(acotaUpgradeInfo);
                    } else if (acotaUpgradeInfo.getOtaMode() == 1) {
                        //用户确认升级
                    } else {
                        //强制升级
                    }
                }
            }


            @Override
            public void error(ACException e) {

            }
        });


    }

    File apk = null;

    */
/**
     * 下载安装 apk
     *
     * @param upgradeInfo
     *//*

    public void downFile(ACOTAUpgradeInfo upgradeInfo) {
        String url = upgradeInfo.getFiles().get(0).getDownloadUrl();
        int checksum = upgradeInfo.getFiles().get(0).getChecksum();

        //创建目录，指定下载路径
        File apkDir = createApkDir(ConstantValue.apk_dir);
        try {
            apk = createApkFile(apkDir, "petrobot.apk");
        } catch (IOException e) {
        }

        SignDevice.getSign().setPetUpdating(true);
        EventBus.getDefault().post(new UIEvent(MainActivity.UI_UPDATE_SHOW));

        LogUtil.d("NICK", "开始下载 3：" + url);

        DownloadUtil.get().download(url, apk.getAbsolutePath(), new DownloadUtil.OnDownloadListener() {
            @Override
            public void onDownloadSuccess() {
                SignDevice.getSign().setPetUpdating(false);
                EventBus.getDefault().post(new UIEvent(MainActivity.UI_DISMISS));

                LogUtil.d("NICK", "apk 下载完成，替换安装");
                //下载成功，建议调用otaMediaDone()接口通知云端下载文件成功，用于日志追踪

                //闪黄常亮灯
                ControlBoardUtils.getInstance().led_seting(controlboardcom.LED_YELLOW_LONGBRIGHT);
                //同时进行设备ota升级，另升级成功后，建议在此清理已完成升级的版本文件
                PackInstallUtils.execCommand(mContext, apk.getAbsolutePath());
            }

            @Override
            public void onDownloading(int progress) {
                //下载进度更新
                LogUtil.i("NICK", "进度：" + progress);
            }

            @Override
            public void onDownloadFailed() {
                SignDevice.getSign().setPetUpdating(false);
                EventBus.getDefault().post(new UIEvent(MainActivity.UI_DISMISS));

                //下载失败，建议清理掉当前下载的不完整文件
                LogUtil.i("NICK", "下载失败");
                if (apk.exists()) {
                    apk.delete();
                }
            }
        });


        //        fileMgr.downloadFile(apk, url, checksum, new ProgressCallback() {
        //            @Override
        //            public void progress(double v) {
        //                //下载进度更新
        //                LogUtil.i("NICK","进度：" + v);
        //            }
        //        }, new VoidCallback() {
        //            @Override
        //            public void success() {
        //                SignDevice.getSign().setPetUpdating(false);
        //
        //                LogUtil.d("NICK", "apk 下载完成，替换安装");
        //
        //                //下载成功，建议调用otaMediaDone()接口通知云端下载文件成功，用于日志追踪
        //
        //                //闪黄常亮灯
        //                ControlBoardUtils.getInstance().led_seting(controlboardcom.LED_YELLOW_LONGBRIGHT);
        //                //同时进行设备ota升级，另升级成功后，建议在此清理已完成升级的版本文件
        //                PackInstallUtils.execCommand(mContext, apk.getAbsolutePath());
        //            }
        //
        //            @Override
        //            public void error(ACException e) {
        //
        //                SignDevice.getSign().setPetUpdating(false);
        //
        //                //下载失败，建议清理掉当前下载的不完整文件
        //                LogUtil.i("NICK","下载失败：" + e.getMessage());
        //                if (apk.exists()) {
        //                    apk.delete();
        //                }
        //            }
        //        });
    }


    public static File createApkDir(String dirName) {
        String SDPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        File dir = new File(SDPATH + dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static File createApkFile(File apkDir, String fileName) throws IOException {
        File apk = new File(apkDir, fileName);
        if (apk.exists()) {
            apk.delete();
        }
        apk.createNewFile();
        return apk;
    }

}
*/
