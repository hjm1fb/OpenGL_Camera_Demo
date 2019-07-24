package com.example.ysh.myapplication.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class DeviceRotationDetector implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float x, y, z;

    public DeviceRotationDetector(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        boolean registered = mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (!registered) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            registered = mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        x = sensorEvent.values[0];
        y = sensorEvent.values[1];
        z = sensorEvent.values[2];

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * 顺时针旋转角度扭正
     *
     * @return
     */
    public int getRotationDegree() {
        if (Math.abs(y) >= Math.abs(x)) {
            if (y >= 0) {
                return 0;
            } else {
                return 180;
            }
        } else {
            if (x >= 0) {
                return 90;
            } else {
                return 270;
            }
        }
    }
}
