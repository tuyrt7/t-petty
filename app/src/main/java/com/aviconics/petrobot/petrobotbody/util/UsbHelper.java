package com.aviconics.petrobot.petrobotbody.util;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;

import com.accloud.utils.LogUtil;
import com.aviconics.petrobot.petrobotbody.manager.SpManager;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.filter.MediaFilter;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.filter.MusicFilter;
import com.aviconics.petrobot.petrobotbody.mvp.model.db.filter.VideoFilter;
import com.blankj.utilcode.util.FileUtils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile.TYPE_MUSIC_SD;
import static com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile.TYPE_MUSIC_USB;
import static com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile.TYPE_VIDEO_SD;
import static com.aviconics.petrobot.petrobotbody.mvp.model.db.bean.MediaFile.TYPE_VIDEO_USB;

/**
 * usb相关的辅助类
 */
public class UsbHelper {

    private Context context;
    private SpManager mSpManager = SpManager.getInstance();

    static class Configs {
        static final String USB_MUSIC_DIR = "/music/";
        static final String USB_VIDEO_DIR = "/video/";
        static final String SD_MEDIA_DIR = "/storage/sdcard0/preload";
    }

    public void init(Context context) {
        this.context = context;
        if (isUsbEnable()) {
            makeMusicAndVideoDir();
        }
    }

    private UsbHelper(){
    }

    private static class UsbHelperTon {
        private static final UsbHelper SINGLE = new UsbHelper();
    }

    public static UsbHelper instance() {
        return UsbHelper.UsbHelperTon.SINGLE;
    }

    private static String[] audioFormat = new String[]{".mp3", ".wma", ".flac"};      //定义音频格式
    private static String[] videoFormat = new String[]{".mp4", ".3gp", ".mov"};      //定义视频格式

    private boolean isAudioFile(String path) {
        for (String format : audioFormat) {
            if (path.endsWith(format)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVideoFile(String path) {
        for (String format : videoFormat) {
            if (path.endsWith(format)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 判断usb是否可用
     *
     * @return
     */
    public boolean isUsbEnable() {
        for (int i = 1; i <= 6; i++) {
            if (FileUtils.createOrExistsFile("/storage/usbdisk" + i + "/pet_usb")) {
                SpManager.getInstance().setUsbpath("/storage/usbdisk" + i);
                return true;
            }
        }
        LogUtil.e("NICK", "--- 未插入usb ---");
        return false;
    }

    /**
     * 获取usb卡路径
     * 先调用 setUsbPathToSp()
     * @return
     */
    public String getUsbCardPath() {
        return mSpManager.getUsbpath();
    }

    /**
     * 获取usb的剩余量 单位byte
     *
     * @return
     */
    public long getUsbCardAllSize() {
        if (isUsbEnable()) {
            StatFs stat = new StatFs(getUsbCardPath());
            // 获取空闲的数据块的数量
            long availableBlocks = (long) stat.getAvailableBlocks() - 4;
            // 获取单个数据块的大小（byte）
            long freeBlocks = stat.getAvailableBlocks();
            return freeBlocks * availableBlocks;
        }
        return 0;
    }

    /**
     * 获取指定路径所在空间的剩余可用容量字节数，单位byte
     *
     * @param filePath
     * @return 容量字节 usb可用空间，内部存储可用空间
     */
    public long getFreeBytes(String filePath) {
        // 如果是sd卡的下的路径，则获取sd卡可用容量
        //        if (filePath.startsWith(getUsbCardPath())) {
        //            filePath = getUsbCardPath();
        //        } else {// 如果是内部存储的路径，则获取内存存储的可用容量
        //            filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //        }
        long l = 0;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            StatFs stat = new StatFs(filePath);
            long availableBlocks = (long) stat.getAvailableBlocks() - 4;
            l = stat.getBlockSize() * availableBlocks;
        } else {
            LogUtil.e("NICK", "external storage is mounted --> false。");
        }
        return l;
    }

    /**
     * 获取系统存储路径
     *
     * @return
     */
    public String getRootDirectoryPath() {
        return Environment.getRootDirectory().getAbsolutePath();
    }


    /**
     * 覆盖之前文件，如果失败，删除该文件
     * 返回 被覆盖删除的文件集合
     */
    public List<String> coverFile() {
        List<String> coverFileList = getCoverFileList();
        if (coverFileList == null) {
            return null;
        }

        for (int i = 0; i < coverFileList.size(); i++) {
            String fileName = coverFileList.get(i);
            File videoFile = new File(getPetVideoDir() + "/" + fileName);
            videoFile.delete();//直接删除
        }
        return coverFileList;
    }

    /**
     * 获取要被 覆盖的文件集合 （被覆盖的文件要大于 10M）
     *
     * @return
     */
    private List<String> getCoverFileList() {
        int index = 0;
        List<String> coverFiles = new ArrayList<>();
        File videoDir = new File(getPetVideoDir());
        String[] list = videoDir.list();
        if (list != null && list.length > 0) {
            File videoFile = new File(videoDir, list[index]);
            coverFiles.add(videoFile.getName());
            long coverSize = videoFile.length();
            //必须满足被覆盖的 文件大小 大于10M
            while (coverSize < 10 * 1024 * 1024) {
                index++;
                File addFile = new File(videoDir, list[index]);
                coverSize += addFile.length();
                coverFiles.add(addFile.getName());
            }
            return coverFiles;
        }
        return null;
    }

    public String getPetDir() {
        return getUsbCardPath() + "/PetRobot";
    }

    public String getPetVideoDir() {
        return getUsbCardPath() + "/PetRobot/video";
    }

    public String getPetThumbDir() {
        return getUsbCardPath() + "/PetRobot/thumb";
    }

    public String getPetTempVideoDir() {
        return getUsbCardPath() + "/PetRobot/temp_video";
    }

    public String getPetRecorderDir() {
        return getUsbCardPath() + "/PetRobot/voice";
    }

    public String getPetPhotoDir() {
        return getUsbCardPath() + "/PetRobot/photo";
    }


    public void makeMusicAndVideoDir() {
        boolean isMusicDirExists = FileUtils.createOrExistsDir(getUsbCardPath() + Configs.USB_MUSIC_DIR);
        boolean isVideoDirExists = FileUtils.createOrExistsDir(getUsbCardPath() + Configs.USB_VIDEO_DIR);
        Log.d("NICK", "make dir : music = " + isMusicDirExists + ",video = " + isVideoDirExists);
    }

    //------------------------------media--------------------------------

    private List<MediaFile> getMediaMusicList(String musicDirPath) {
        List<MediaFile> musics = new ArrayList<MediaFile>();
        File musicDir = new File(musicDirPath);
        if (!musicDir.exists()) {
            musicDir.mkdirs();
            return musics;
        }
        File[] files = musicDir.listFiles(new MusicFilter());
        if (files != null && files.length > 0) {
            for (File music : files) {

                if (music.isHidden()) {
                    continue;
                }
                if (music.isFile()) {
                    String name = music.getName();
                    String url = music.getAbsolutePath();
                    int mType = TYPE_MUSIC_USB;
                    if (url.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                        mType = TYPE_MUSIC_SD;
                    }
                    long mDuration = -1;
                    try {
                        mRetriever.setDataSource(url); //耗时操作
                        String duration = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        if (duration != null)
                            mDuration = Long.parseLong(duration) / 1000;//秒
                    } catch (Exception ex) {
                    }
                    if (mDuration < 1) { //小于1s的都作废
                        continue;
                    }

                    MediaFile mMediaFile = new MediaFile(null, url, mDuration, name, mType);
                    musics.add(mMediaFile);

                    LogUtil.d("NICK", "音乐：" + music.getAbsolutePath());
                }
            }
        }
        return musics;
    }

    private List<MediaFile> getMediaVideoList(String videoDirPath) {
        List<MediaFile> videos = new ArrayList<MediaFile>();
        File videoDir = new File(videoDirPath);
        if (!videoDir.exists()) {
            videoDir.mkdirs();
            return videos;
        }
        File[] files = videoDir.listFiles(new VideoFilter());
        if (files != null && files.length > 0) {
            for (File video : files) {
                if (video.isHidden()) {
                    continue;
                }

                String name = video.getName();
                String url = video.getAbsolutePath();
                int mType = TYPE_VIDEO_USB;
                if (url.startsWith(Environment.getExternalStorageDirectory().getAbsolutePath())) {
                    mType = TYPE_VIDEO_SD;
                }
                long mDuration = -1;
                try {
                    mRetriever.setDataSource(url); //耗时操作
                    String duration = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (duration != null)
                        mDuration = Long.parseLong(duration) / 1000;
                } catch (Exception ex) {
                }
                if (mDuration <= 1) { //小于1s音视频抛弃
                    continue;
                }

                MediaFile mMediaFile = new MediaFile(null, url, mDuration, name, mType);
                videos.add(mMediaFile);
                LogUtil.d("NICK", "视频：" + mMediaFile.getUrl());
            }
        }
        return videos;
    }

    private List<MediaFile> getSdMediaList(String sdDirPath) {
        List<MediaFile> videos = new ArrayList<MediaFile>();
        File videoDir = new File(sdDirPath);
        if (!videoDir.exists()) {
            videoDir.mkdirs();
            return videos;
        }
        File[] files = videoDir.listFiles(new MediaFilter());
        if (files != null && files.length > 0) {
            for (File media : files) {
                if (media.isHidden()) {
                    continue;
                }

                String name = media.getName();
                String url = media.getAbsolutePath();
                int mType;
                if (isAudioFile(url)) {
                    mType = TYPE_MUSIC_SD;
                } else if (isVideoFile(url)) {
                    mType = TYPE_VIDEO_SD;
                } else {
                    continue;
                }

                long mDuration = -1;
                try {
                    mRetriever.setDataSource(url); //耗时操作
                    String duration = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (duration != null)
                        mDuration = Long.parseLong(duration) / 1000;
                } catch (Exception ex) {
                }
                if (mDuration <= 1) { //小于1s音视频抛弃
                    continue;
                }

                MediaFile mMediaFile = new MediaFile(null, url, mDuration, name, mType);
                videos.add(mMediaFile);
                LogUtil.d("NICK", "多媒体：" + mMediaFile.getUrl());
            }
        }
        return videos;
    }


    public List<MediaFile> getAllMediaList() {
        List<MediaFile> allMedia = new ArrayList<MediaFile>();
        List<MediaFile> sdMediaList = getSdMediaList(Configs.SD_MEDIA_DIR);
        allMedia.addAll(sdMediaList);
        if (isUsbEnable()) {
            List<MediaFile> list = getUsbMedia(getUsbCardPath());
            allMedia.addAll(list);
        }
        return allMedia;
    }

    private MediaFile mediaFile = new MediaFile();
    private List<MediaFile> mMediaList = new ArrayList<>();
    private MediaMetadataRetriever mRetriever = new MediaMetadataRetriever();

    public List<MediaFile> getUsbMedia(String usbPath) {
        mMediaList.clear();
        mMediaList = new ArrayList<>();
        scanFolder(usbPath);
        return mMediaList;
    }

    private void scanFolder(String filePath) {
        File inputFile = new File(filePath);
        if (inputFile.isDirectory()) {
            File[] files = inputFile.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.isHidden()) {
                        continue;
                    }
                    if (file.isDirectory()) {
                        if (file.getAbsolutePath().contains("/PetRobot/video")) {
                            continue;
                        }
                        scanFolder(file.getAbsolutePath());
                    } else {
                        scanFile(file.getAbsolutePath(), file.getName());
                    }
                }
            }
        } else {
            scanFile(filePath, inputFile.getName());
        }
    }

    private void scanFile(String path, String name) {
        if (isAudioFile(path)) {
            int type = TYPE_MUSIC_USB;
            long mDuration = 0;
            try {
                mRetriever.setDataSource(path); //耗时操作
                String duration = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (duration != null)
                    mDuration = Long.parseLong(duration) / 1000;//秒
            } catch (Exception ex) {
            }
            if (mDuration < 1) { //小于1s的都作废
                return;
            }
            mediaFile = new MediaFile(null, path, mDuration, name, type);
            mMediaList.add(mediaFile);
            //            Log.d("futao-media", "音乐：" + path);
        } else if (isVideoFile(path)) {
            int type = MediaFile.TYPE_VIDEO_USB;
            long mDuration = 0;
            try {
                mRetriever.setDataSource(path); //耗时操作
                String duration = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (duration != null)
                    mDuration = Long.parseLong(duration) / 1000;//秒
            } catch (Exception ex) {
            }
            if (mDuration < 1) { //小于1s的都作废
                return;
            }
            mediaFile = new MediaFile(null, path, mDuration, name, type);
            mMediaList.add(mediaFile);
            //            Log.d("futao-media", "视频：" + path);
        }
    }


    //------------------------------media--------------------------------end


    /**
     * 获取指定路径所在空间的剩余可用容量字节数，单位byte
     *
     * @param filePath
     * @return 容量字节 usb可用空间，内部存储可用空间
     */
    public String getFreeMemorySize(String filePath) {
        // 如果是sd卡的下的路径，则获取sd卡可用容量
        if (filePath.startsWith(getUsbCardPath())) {
            filePath = getUsbCardPath();
        } else {// 如果是内部存储的路径，则获取内存存储的可用容量
            filePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        StatFs stat = new StatFs(filePath);
        long blockSize = stat.getBlockSize();
        long availableBlocks = (long) stat.getAvailableBlocks() - 4;
        return Formatter.formatFileSize(context, blockSize * availableBlocks);
    }

    /**
     * 获取指令路径总的存储空间
     *
     * @return
     */
    public String getTotalMemorySize(String path) {
        if (!new File(path).exists())
            return "";
        StatFs stat = new StatFs(path);
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return Formatter.formatFileSize(context, totalBlocks * blockSize);
    }

    private static DecimalFormat fileIntegerFormat = new DecimalFormat("#0");
    private static DecimalFormat fileDecimalFormat = new DecimalFormat("#0.#");

    /**
     * 单位换算
     *
     * @param size      单位为B
     * @param isInteger 是否返回取整的单位
     * @return 转换后的单位
     */
    public static String formatFileSize(long size, boolean isInteger) {
        DecimalFormat df = isInteger ? fileIntegerFormat : fileDecimalFormat;
        String fileSizeString = "0M";
        if (size < 1024 && size > 0) {
            fileSizeString = df.format((double) size) + "B";
        } else if (size < 1024 * 1024) {
            fileSizeString = df.format((double) size / 1024) + "K";
        } else if (size < 1024 * 1024 * 1024) {
            fileSizeString = df.format((double) size / (1024 * 1024)) + "M";
        } else {
            fileSizeString = df.format((double) size / (1024 * 1024 * 1024)) + "G";
        }
        return fileSizeString;
    }

    public static boolean fileIsExists(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {

            return false;
        }
        return true;
    }

}
