package com.example.ysh.myapplication.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.view.Surface;


import java.io.ByteArrayOutputStream;
import java.util.List;

public class CameraUtils {
    private static final String TAG = "CameraUtil";

    public static int getActivityDisplayOrientation(Activity activity) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        return degrees;
    }

    public static boolean isFlip(int mCameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        return info.facing != Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    /**
     * 360 - info.orientation, 这一步得出的是对于相机来说,逆时针要矫正的角度
     * 前置摄像头时, 由于degrees是顺时针要纠正的角度, 所以要减一下(- degrees). 后置摄像头刚好相反
     * @param info
     * @param degrees 逆时针偏离的角度, 即顺时针旋转此角度后矫正成正方向
     * @return 返回矫正时, 逆时针要旋转的角度;
     */
    public static int getImageOrient(Camera.CameraInfo info, int degrees) {
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = (360 - info.orientation + degrees + 360) % 360;
        } else {
            result = (360 - info.orientation - degrees + 360) % 360;
        }
        return result;
    }


    /**
     *  camera.setDisplayOrientation(degree)需要用到的角度
     * @param info
     * @param degrees 逆时针偏离的角度, 即顺时针旋转此角度后矫正成正方向
     * @return 返回矫正时, 顺时针要旋转的角度, 考虑了前置摄像头的水平镜像;
     */
    public static int getDisplayOrientationClockWise(Camera.CameraInfo info, int degrees) {
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }



    public static Bitmap getPreviewBitmap(byte[] bytes, int format, Camera.Size size) {
        return getPreviewBitmap(bytes, format, size.width, size.height);
    }

    public static Bitmap getPreviewBitmap(byte[] bytes, int format, int width, int height) {

        YuvImage yuv = new YuvImage(bytes, format, width, height, null);

        ByteArrayOutputStream jpgStream = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, jpgStream);

        byte[] jpgByte = jpgStream.toByteArray();
        return BitmapFactory.decodeByteArray(jpgByte, 0, jpgByte.length);
    }

    public static int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }
    public static Camera openDefaultCamera() {
        return Camera.open(0);
    }
    public static Camera openCamera(final int id) {
        return Camera.open(id);
    }
    public static boolean hasCamera(final int facing) {
        return getCameraId(facing) != -1;
    }

    /**
     * 按照camera facing获取对应的camera id
     * @param facing
     * @return
     */
    private static int getCameraId(final int facing) {
        int cameraId = 0;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int k = 0; k < Camera.getNumberOfCameras(); k++) {
            Camera.getCameraInfo(k, info);
            if (info.facing == facing) {
                cameraId = k;
                break;
            }
        }
        return cameraId;
    }

    /**
     * 根据camera id获取该相机的facing
     */
    private static int getCameraFacing(int cameraId){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);
        return cameraInfo.facing;
    }

    public static Camera.Size findBestPreviewSizeValue(Point screenResolution, Camera mCamera) {
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
        return findBestPreviewSizeValue(sizeList,screenResolution);
    }

    private static Camera.Size findBestPreviewSizeValue(List<Camera.Size> sizeList, Point screenResolution) {
        int size = 0;
        int index =0;
        for(int i = 0; i < sizeList.size(); i ++){
            // 如果有符合的分辨率，则直接返回
            if(sizeList.get(i).width == screenResolution.x && sizeList.get(i).height == screenResolution.y){
                return sizeList.get(i);
            }

            int newX = sizeList.get(i).width;
            int newY = sizeList.get(i).height;
            int newSize = Math.abs(newX * newX) + Math.abs(newY * newY);
            float ratio = (float)newY / (float)newX;
            //取较高的分辨率
            if (newSize >= size) {
                index =i;
                size = newSize;
            } else if (newSize < size) {
                continue;
            }
        }
        return sizeList.get(index);
    }

    /**
     * 选择最接近比例的相机分辨率
     * 较大的值作为参数w
     * @param mCamera
     * @param w  较大的值作为参数w
     * @param h
     * @return
     */
    public static Camera.Size findBestPreviewSizeValueByRatio(Camera mCamera, int w, int h) {
        List<Camera.Size> sizeList = mCamera.getParameters().getSupportedPreviewSizes();
        return findBestPreviewSizeValueByRatio(sizeList,w, h,w/3);
    }

    /**
     *
     * @param sizes
     * @param h
     * @param w  较大的值作为参数w
     * @param minWidth 最小的宽度
     * @return
     */
    public static Camera.Size findBestPreviewSizeValueByRatio(List<Camera.Size> sizes, int w, int h, int minWidth) {
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = sizes.get(0);
        double minRatioDiff = Double.MAX_VALUE;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            double ratioDiff = Math.abs(ratio - targetRatio);
            //一体机比较大, 小的分辨率看起来像素很渣, 所以过滤
            if (ratioDiff < minRatioDiff && size.width >=minWidth) {
                optimalSize = size;
                minRatioDiff = ratioDiff;
            }else if( ratioDiff == minRatioDiff && optimalSize.height > size.height //相同的分辨率比例, 就选尺寸小的
                    && size.width >=minWidth){
                optimalSize = size;
                minRatioDiff = ratioDiff;
            }
        }

  /*      // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }*/

        return optimalSize;
    }


    public static boolean hasFrontCamera() {
        return hasCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    public static boolean hasBackCamera() {
        return hasCamera(Camera.CameraInfo.CAMERA_FACING_BACK);

    }



    public static void stopCamera(Camera mCamera) {
        if (null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
        }
    }

}
