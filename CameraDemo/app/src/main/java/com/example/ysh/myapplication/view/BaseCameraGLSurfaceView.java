package com.example.ysh.myapplication.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;

import com.example.ysh.myapplication.util.CameraEngine;

public abstract class BaseCameraGLSurfaceView extends GLSurfaceView {

    public BaseCameraGLSurfaceView(Context context) {
        super(context);
    }


    public abstract void init(CameraEngine cameraEngine, boolean isPreviewStarted, Context context);


    public abstract Bitmap capture(final int parentWidth, final int parentHeight) throws InterruptedException;

    public abstract void setFlipX(boolean flipX);


}
