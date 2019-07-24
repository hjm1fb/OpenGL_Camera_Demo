package com.example.ysh.myapplication.activity;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ysh.myapplication.R;
import com.example.ysh.myapplication.util.CameraEngine;
import com.example.ysh.myapplication.util.CameraEngineBitmapUtils;
import com.example.ysh.myapplication.util.CameraUtils;
import com.example.ysh.myapplication.view.SimpleCameraGLSurfaceView;

import java.io.IOException;

import static com.example.ysh.myapplication.util.CameraEngine.CAMERA_ID_BACK;
import static com.example.ysh.myapplication.util.CameraEngine.CAMERA_ID_FRONT;


public class CameraGLSurfaceActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    FrameLayout cameraViewContainer;
    SimpleCameraGLSurfaceView surfaceView;
    private CameraEngine cameraEngine;
    private boolean openBackCamera;
    private int cameraRotateAdjust;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_glserface);

        cameraViewContainer = findViewById(R.id.camera_preview);
        surfaceView = new SimpleCameraGLSurfaceView(this);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        surfaceView.setLayoutParams(layoutParams);
        cameraEngine = new CameraEngine(this);
        cameraEngine.setCameraId(openBackCamera ? CAMERA_ID_BACK : CAMERA_ID_FRONT);
        surfaceView.init(cameraEngine,false,this);
        cameraViewContainer.addView(surfaceView);
        findViewById(R.id.iv_take_pic).setOnClickListener(this);
        findViewById(R.id.bFlipX).setOnClickListener(this);
        cameraEngine.setCameraCallBack(new CameraEngine.CameraCallBack() {
            @Override
            public void openCameraError(Exception e) {

            }

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {

            }

            @Override
            public void justOneCamera() {

            }

            @Override
            public void openCameraSucceed(Camera mCamera, int cameraId) {
                // 特殊设备手动适配
                int cameraRotate = cameraEngine.getDisplayOrientationClockWise();
                cameraRotate += cameraRotateAdjust;
                cameraRotate += 360;
                cameraRotate %= 360;
                mCamera.setDisplayOrientation(cameraRotate);//顺时针要旋转的角度
                Camera.Size sizeValue = CameraUtils.findBestPreviewSizeValue(new Point(640, 480), mCamera);
                cameraEngine.setPreviewSize(sizeValue);
                resizeSurfaceView(sizeValue);
            }
        });
    }

    public void resizeSurfaceView(Camera.Size previewSize) {
        if (previewSize == null || cameraViewContainer.getMeasuredWidth() == 0){
            return;
        }
        int measuredWidth = cameraViewContainer.getMeasuredWidth();
        int measuredHeight = cameraViewContainer.getMeasuredHeight();
        int previewWidth;
        int previewHeight;
        if (cameraEngine.isTransPose(cameraRotateAdjust)){
            previewWidth = previewSize.height;
            previewHeight = previewSize.width;
        }else {
            previewWidth = previewSize.width;
            previewHeight = previewSize.height;
        }
        float ratioW = measuredWidth * 1f / previewWidth;
        float ratioH = measuredHeight * 1f / previewHeight;
        float scaleRatio = Math.max(ratioW, ratioH);
        int  newViewWidth = (int) (previewWidth * scaleRatio);
        int  newViewHeight = (int) (previewHeight * scaleRatio);
        int originX = (measuredWidth - newViewWidth) / 2;
        int originY = (measuredHeight - newViewHeight) / 2;
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(newViewWidth, newViewHeight);
        layoutParams.setMargins(originX, originY, originX, originY);
        surfaceView.setLayoutParams(layoutParams);
        Log.d("resizeSurfaceView", " measuredWidth: " + measuredWidth + " measuredHeight: " + measuredHeight +
                " previewWidth: " + previewWidth + " previewHeight: " + previewHeight + " scaleRatio: " + scaleRatio);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_take_pic:
                try {
                    Bitmap previewBitmap = surfaceView.capture(cameraViewContainer.getMeasuredWidth(), cameraViewContainer.getMeasuredHeight());
                    Bitmap uprightBitmap = CameraEngineBitmapUtils.roateFlipBitmap(previewBitmap, cameraEngine.getDeviceRoation() - cameraEngine.getActivityRoate(), false, true);
                    try {
                        String path = CameraEngineBitmapUtils.saveBitmap(this, uprightBitmap);
                        Toast.makeText(CameraGLSurfaceActivity.this,getString(R.string.picture_saved_successfully_at) + path,Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bFlipX:
                surfaceView.setFlipX(!surfaceView.isFlipX());
                break;
            default:
                break;
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraEngine.onResume();

    }

    @Override
    protected void onPause() {
        cameraEngine.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
