package com.example.yanfa.myapplication.app;

import android.app.Application;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.example.yanfa.myapplication.mode.db.MyObjectBox;
import com.example.yanfa.myapplication.mode.netWork.Api;
import com.example.yanfa.myapplication.mode.netWork.ConstantStr;
import com.example.yanfa.myapplication.utlis.SPHelper;

import io.objectbox.BoxStore;

public class DataApp extends Application {

    private static DataApp instance;
    private BoxStore boxStore;
    private static final Handler sHandler = new Handler();
    private static Toast sToast; // 单例Toast,避免重复创建，显示时间过长

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("za", "App start");
        instance = this;
        boxStore = MyObjectBox.builder().androidContext(this).build();
        sToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    public static DataApp getInstance() {
        return instance;
    }

    public BoxStore getBoxStore() {
        return boxStore;
    }

    public static void toast(String txt, int duration) {
        sToast.setText(txt);
        sToast.setDuration(duration);
        sToast.show();
    }

    public static void runUi(Runnable runnable) {
        sHandler.post(runnable);
    }

    public String AppHost() {
        return SPHelper.readString(this, ConstantStr.APP_DATA, ConstantStr.APP_HOST, Api.HOST);
    }
}
