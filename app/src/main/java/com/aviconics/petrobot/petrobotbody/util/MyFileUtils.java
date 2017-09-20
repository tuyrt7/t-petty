package com.aviconics.petrobot.petrobotbody.util;


import android.util.Log;

import com.aviconics.petrobot.petrobotbody.app.App;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;

/**
 * 文件操作类
 */
public class MyFileUtils {

    /**
     * 删除文件
     *
     * @param path 文件绝对路径
     * @return
     */
    public static boolean delFile(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            Log.d("NICK", "-----file------:" + deleteFileSafely(file));
            return deleteFileSafely(file);
        }
        return false;
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

    /**
     * 安全删除文件.
     *
     * @param file
     * @return
     */
    public static boolean deleteFileSafely(File file) {
        if (file != null) {
            String tmpPath = file.getParent() + File.separator + System.currentTimeMillis();
            File tmp = new File(tmpPath);
            file.renameTo(tmp);
            return tmp.delete();
        }
        return false;
    }


    /**
     * 删除某个文件夹下的所有文件夹和文件
     *
     * @param delPath
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static boolean deleteAllFile(String delPath) throws IOException {
        File file = new File(delPath);
        if (file == null || !file.exists()) {
            return false;
        }
        if (!file.isDirectory()) {
            file.delete();
        } else if (file.isDirectory()) {
            File[] fileList = file.listFiles();
            if (fileList.length != 0 && fileList != null) {
                for (int i = 0; i < fileList.length; i++) {
                    File delFile = fileList[i];
                    if (!delFile.isDirectory()) {
                        delFile.delete();
                        System.out.println("删除文件成功," + "文件全名=" + delFile.getName() + ",绝对路径=" + delFile.getAbsolutePath() + ",相对路径=" + delFile.getPath());
                    } else if (delFile.isDirectory()) {
                        deleteAllFile(fileList[i].getPath());
                    }
                }
            }
            file.delete();
        }
        return true;
    }

    /**
     * 创建文件file路径
     *
     * @param dirPath  目录路径
     * @param fileName 文件名
     * @return
     */
    public static File mkFile(String dirPath, String fileName) {
        File mDir = new File(dirPath);
        if (!mDir.exists()) {
            mDir.mkdirs();
        }
        File file = new File(mDir, fileName);
        return file;
    }


    /**
     * 创建目录
     *
     * @param dirPath 目录路径
     * @return
     */
    public static File mkDir(String dirPath) {
        File mDir = new File(dirPath);
        if (!mDir.exists()) {
            mDir.mkdirs();
        }
        return mDir;
    }

    public static long getFileSize(String videoFileName) {
        File videoFile = new File(App.getUsbHelper().getPetVideoDir() + File.separator + videoFileName);
        if (videoFile.exists()) {
            long length = videoFile.length();
            return length;
        }
        return 0;
    }

    /**
     * 写 text 到指定目录的文件
     *
     * @param dirPath
     * @param name
     * @param text    文本内容（）
     * @return
     */
    public static boolean writeText2File(String dirPath, String name, String text) {
        File file = mkFile(dirPath, name);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write(text);
            bw.flush();
            bw.close();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void appendMethodB(String fileName, String content) {
        try {
            //打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static boolean copyFolder(File srcFile, File destFile){
        boolean result = false;
        File[] list = null;
        try {
            if (!destFile.exists()) {
                destFile.mkdirs();
            } else {
                if (destFile.list().length == srcFile.list().length) {
                    return true;
                }
            }

            if (!srcFile.isDirectory()) {
                return false;
            }

            result = true;
            list = srcFile.listFiles();
        } catch (Exception e) {}

        if (list == null || list.length == 0) {
            return false;
        }
        for (File f : list) {
            if (f.isDirectory()) {
                result &= copyFolder(f, new File(destFile, f.getName()));
            } else if(f.isFile()){
                result &= copyFile(f, new File(destFile, f.getName()));
            }
        }
        return result;
    }

    public static boolean copyFile(File srcFile, File destFile) {
        boolean result = false;
        try {
            InputStream in = new FileInputStream(srcFile);
            try {
                result = copyToFile(in, destFile);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            result = false;
        }
        return result;
    }


    private static boolean copyToFile(InputStream inputStream, File destFile) {
        try {
            if (destFile.exists()) {
                destFile.delete();
            }
            FileOutputStream out = new FileOutputStream(destFile);
            try {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) >= 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.flush();
                try {
                    out.getFD().sync();
                } catch (IOException e) {
                }
                out.close();
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
