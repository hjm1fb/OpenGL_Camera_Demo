package com.example.ysh.myapplication.util;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import com.example.ysh.myapplication.MyApplication;
import com.example.ysh.myapplication.R;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.lang.ref.WeakReference;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class CameraEngine {

    /**
     * 如果设备的两个摄像头都是前置摄像头, 那么这两个id区分是的是彩色相机和红外相机, 但安装顺序未知, 所以不能确定彩色相机的编号是0还是1
     */
    public static final int CAMERA_ID_FRONT = 1; //一般的手机, 前置摄像头的cameraId都是1;
    public static final int CAMERA_ID_BACK = 0; //一般的手机, 后置摄像头的cameraId都是0;

    private CameraCallBack cameraCallBack;
    private int mCameraId = CAMERA_ID_FRONT;
    private Camera mCamera = null;
    private WeakReference<FragmentActivity> activityWeakReference;
    private int activityRotation;
    private DeviceRotationDetector mDeviceRotationDetector;
    private Camera.Size previewSize;
    //private SurfaceTexture surfaceTexture;
    private SurfaceTexture surfaceTexture;
    private Camera.CameraInfo cameraInfo;
    private boolean startPreviewSuccess;

    public CameraEngine(FragmentActivity activity) {
        this.activityWeakReference = new WeakReference<>(activity);
        activityRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        mDeviceRotationDetector = new DeviceRotationDetector(activity);
    }

    public void startCamera(final int cameraId,final boolean resetPreviewSize) {
        FragmentActivity activity = activityWeakReference.get();
        if (CommonTool.isInLifeCycle(activity)){
            new RxPermissions(activity).request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean b) throws Exception {
                            if (b) {
                                openCamera(cameraId,resetPreviewSize);
                            } else {
                                Toast.makeText(MyApplication.getsInstance(), R.string.obtain_permissions_failed,Toast.LENGTH_SHORT).show();
                                if (cameraCallBack != null) {
                                    cameraCallBack.openCameraError(new Exception(MyApplication.getsInstance().getString(R.string.obtain_permissions_failed)));
                                }
                            }
                        }
                    });
        }

    }

    public void onResume() {
        try {
            startCamera(mCameraId,false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int cameraId, boolean resetPreviewSize) {
        try {
            if (mCamera != null) {
                stopCamera();
            }
            int cameraCount = Camera.getNumberOfCameras();
            if (cameraCount < 2) {
                if (cameraCallBack != null) {
                    cameraCallBack.justOneCamera();
                }
            }
//            boolean hasCamera = CameraUtils.hasCamera(cameraId);
//            if (hasCamera) {
            try {
                mCamera = CameraUtils.openCamera(cameraId);
                mCameraId = cameraId;
            } catch (Exception e) {
                cameraId = ((cameraId == CAMERA_ID_FRONT)
                        ? CAMERA_ID_BACK : CAMERA_ID_FRONT);
                mCamera = CameraUtils.openCamera(cameraId);
                mCameraId = cameraId;
            }
//            } else if (cameraCount > 0) {
//                L.d(TAG, "!hasCamera: opencamera 0");
//                mCamera = CameraUtils.openDefaultCamera();
//                mCameraId = 0;
//            }
            if (previewSize == null || resetPreviewSize) {
//                Point screenResolution = new Point(640, 480);
                Point screenResolution = new Point(640,480);
                previewSize = CameraUtils.findBestPreviewSizeValue(screenResolution, mCamera);
            }
            Camera.Parameters mParams = mCamera.getParameters();
            List<String> supportedFlashModes = mParams.getSupportedFocusModes();
            if (supportedFlashModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            } else if (supportedFlashModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (supportedFlashModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
            int[] minFps = new int[1],
                    maxFps = new int[1];
            determineCameraPreviewFpsRange(mParams, minFps, maxFps);
            mParams.setPreviewFpsRange(minFps[0], maxFps[0]);

            mParams.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(mParams);
            getCameraInfo(true);
            //cameraContainer.onCameraOpenSucceed(mCamera,previewSize);
            if (cameraCallBack != null) {
                cameraCallBack.openCameraSucceed(mCamera, mCameraId);
            }
        } catch (Exception e) {
            if (cameraCallBack != null) {
                cameraCallBack.openCameraError(e);
            }
            e.printStackTrace();
        }
    }

    public boolean isTransPose(int cameraRotateAdjust){
        int cameraRotate = getCameraOrientation();
        cameraRotate += cameraRotateAdjust;
        cameraRotate += 360;
        cameraRotate %= 360;
        return (cameraRotate == 270 || cameraRotate == 90);
    }

    /**
     * 获取的相机预览数据逆时针旋转到activity方向所需要的旋转角度,
     * 主要用于绘制时转换坐标系，因为绘制时，始终以Activity的左上角为原点
     *
     * @return
     */
    public int getCameraOrientation() {
        return CameraUtils.getImageOrient(getCameraInfo(false), getActivityRoate());
    }

    private void determineCameraPreviewFpsRange(Camera.Parameters parameters, int[] minFps, int[] maxFps) {
        final int MAX_FPS = 30 * 1000;   // Frame rates are scaled by 1000 in Android.
        List<int[]> frameRates = parameters.getSupportedPreviewFpsRange();
//        Log.d(TAG, "FPS range options: " + frameRates.size());

        minFps[0] = 0;
        for (int[] intArr : frameRates) {
            if (minFps[0] == 0) {
                minFps[0] = intArr[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
                maxFps[0] = intArr[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
                continue;
            }

            if (intArr[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] <= MAX_FPS) {
                if (intArr[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] >= minFps[0] &&
                        intArr[Camera.Parameters.PREVIEW_FPS_MAX_INDEX] >= maxFps[0]) {
                    minFps[0] = intArr[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
                    maxFps[0] = intArr[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
                }
            }
        }
    }

    public List<Camera.Size> getSupportPreviewSize() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void setPreviewSize(Camera.Size size) {
        if (previewSize == null || !previewSize.equals(size) || !startPreviewSuccess){
            previewSize = size;
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            Camera.Parameters mParams = mCamera.getParameters();
            mParams.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(mParams);
            //cameraContainer.onPreviewSizeChange(previewSize);
            startPreview(surfaceTexture);
        }
    }

    public synchronized boolean startPreview(SurfaceTexture newSurfaceTexture) {
        if (newSurfaceTexture != null){
            this.surfaceTexture = newSurfaceTexture;
        }
        if (surfaceTexture == null || mCamera == null){
            return false;
        }
        try {
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (cameraCallBack != null) {
                        cameraCallBack.onPreviewFrame(data, camera);
                    }
                }
            });
            mCamera.startPreview();
            startPreviewSuccess = true;
            Log.d("startPreview"," startPreviewSuccess = true");
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 画面是否需要旋转
     *
     * @return
     */
    public boolean getImageFilp() {
        return CameraUtils.isFlip(mCameraId);
    }


    private void stopCamera() {
        try {
            if (mCamera != null) {
                CameraUtils.stopCamera(mCamera);
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean hasFrontCamera() {
        return CameraUtils.hasFrontCamera();
    }

    public boolean hasBackCamera() {
        return CameraUtils.hasBackCamera();
    }


    public Camera.Size getPreviewSize() {
        return previewSize;
    }


    public void setCameraCallBack(CameraCallBack cameraCallBack) {
        this.cameraCallBack = cameraCallBack;
    }

    public boolean isFrontCamera() {
        return getCameraInfo(false).facing == CAMERA_ID_FRONT;
    }

    public void onPause(){
        stopCamera();
        startPreviewSuccess = false;
    }
    public void setCameraId(int cameraId) {
        this.mCameraId = cameraId;
    }

    public interface CameraCallBack {
        void openCameraError(Exception e);

        void onPreviewFrame(byte[] data, Camera camera);

        void justOneCamera();

        void openCameraSucceed(Camera mCamera, int cameraId);
    }

    public synchronized Camera.CameraInfo getCameraInfo(boolean reset){
        if (reset || cameraInfo == null){
            synchronized(this) {
                if (reset || cameraInfo == null){
                    cameraInfo = new Camera.CameraInfo();
                    Camera.getCameraInfo(mCameraId, cameraInfo);
                }
            }
        }
        return cameraInfo;
    }

    /**
     * camera.setDisplayOrientation(degree)需要用到的角度
     *
     * @return
     */
    public int getDisplayOrientationClockWise() {
        return CameraUtils.getDisplayOrientationClockWise(getCameraInfo(false), getActivityRoate());
    }

    /**
     * 顺时针旋转此角度后扭正
     * @return
     */
    public int getDeviceRoation() {
        return mDeviceRotationDetector.getRotationDegree();
    }

    /**
     * 顺时针旋转此角度后扭正
     * @return
     */
    public int getActivityRoate(){
        int rotation = activityRotation;
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


}
