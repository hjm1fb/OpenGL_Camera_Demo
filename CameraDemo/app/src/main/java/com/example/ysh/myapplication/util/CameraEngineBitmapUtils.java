package com.example.ysh.myapplication.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.example.ysh.myapplication.MyApplication;
import com.example.ysh.myapplication.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraEngineBitmapUtils {
    private static String fileDir;

    public static File savePhotoToSDCard(Bitmap photoBitmap, String path, String photoName, Bitmap.CompressFormat format) {
        if (checkSDCardAvailable()) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File photoFile = new File(path, photoName);

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(photoFile);
                if (photoBitmap.compress(format, 100, fileOutputStream)) {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
                return photoFile;
            } catch (FileNotFoundException e) {
                photoFile.delete();
                e.printStackTrace();
            } catch (IOException e) {
                photoFile.delete();
                e.printStackTrace();
            } finally {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Check the SD card
     *
     * @return 是否获能获取到SD卡
     */
    public static boolean checkSDCardAvailable() {
        return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    }

    public static Bitmap roateFlipBitmap(Bitmap currentSelBitmap, float angle, boolean imageFilp, boolean isRecycle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        if (imageFilp){
            matrix.postScale(-1,1);
        }
        return createBitmapByMatrix(currentSelBitmap, matrix, isRecycle);
    }

    public static Bitmap createBitmapByMatrix(Bitmap source,
                                              Matrix m, boolean isRecycle) {
        Bitmap result = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), m, false);
        // createBitmapByMatrix 有可能会直接返回source出来，所以此处需要判断result和source是否相同
        if (isRecycle && (result != source)) {
            recycleBitmap(source);
        }
        return result;
    }

    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    public static void setFileDir(String fileDir) {
        CameraEngineBitmapUtils.fileDir = fileDir;
    }

    public static String saveBitmap(Context context, Bitmap bmp) throws IOException {
        return saveBitmap(context, bmp,"", Bitmap.CompressFormat.JPEG);
    }

    public static String saveBitmap(Context context, Bitmap b, String fileName, Bitmap.CompressFormat format) {
        if (TextUtils.isEmpty(fileName)) {
            long dataTake = System.currentTimeMillis();
            String postfix = ".jpg";
            switch (format) {
                case PNG:
                    postfix = ".png";
                    break;
                case JPEG:
                    postfix = ".jpg";
                    break;
                case WEBP:
                    postfix = ".webp";
                    break;
            }
            if (TextUtils.isEmpty(fileName))
                fileName = "IMG_" + dataTake + postfix;
        }
        Pattern pattern = Pattern.compile("[\\s\\\\/:\\*\\?\\\"<>\\|]");
        Matcher matcher = pattern.matcher(fileName);
        fileName = matcher.replaceAll(""); // 将非法字符移除
        File file = new File(fileName);
        File photoFile = savePhotoToSDCard(b, getBitmapPath(), file.getName(), format);
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(photoFile);
        intent.setData(uri);
        context.sendBroadcast(intent);
        assert photoFile != null;
        return photoFile.getAbsolutePath();
    }

    public static String getBitmapPath() {
        String path =getFilePath()+File.separator+"bitmap";
        File file=new File(path);
        if (!file.exists()){
            file.mkdirs();
        }
        return path;
    }

    /**
      * 取SD卡路径
      **/
    public static String getFilePath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);  //判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();  //获取根目录
        }
        if (sdDir != null) {
            return sdDir.toString() + File.separator + MyApplication.getsInstance().getResources().getString(R.string.app_name);
        } else {
            String absolutePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();
            return absolutePath + File.separator + MyApplication.getsInstance().getString(R.string.app_name);
        }
    }
}
