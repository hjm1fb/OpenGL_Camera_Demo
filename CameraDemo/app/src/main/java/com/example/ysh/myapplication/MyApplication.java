package com.example.ysh.myapplication;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {
    private static MyApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static MyApplication getsInstance() {
        return sInstance;
    }
}
