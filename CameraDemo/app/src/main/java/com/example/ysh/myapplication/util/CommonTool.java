package com.example.ysh.myapplication.util;

import android.app.Activity;
import android.os.Build;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.exceptions.CompositeException;


/**
 * * Created by hjm1fb
 *
 * 通用工具类
 *
 * @author hjm1fb
 */

public class CommonTool {

    public static boolean isEmpty(List list) {
        return list == null || list.isEmpty();
    }

    public static boolean isEmpty(WeakReference weakReference) {
        return weakReference == null || weakReference.get() == null;
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.size() == 0;
    }

    public static boolean isEmpty(Set set) {
        return set == null || set.size() == 0;
    }

    public static boolean isEmpty(float[] flaotArray) {
        return flaotArray == null || flaotArray.length == 0;
    }

    /**
     *
     * 当前Activity是否已经结束了其生命周期
     *
     */
    public static boolean isInLifeCycle(Activity activity){
        if (activity == null){
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return !activity.isFinishing() && !activity.isDestroyed();
        } else {
            return !activity.isFinishing();
        }
    }

}