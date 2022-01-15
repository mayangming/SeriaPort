package com.machine.serialport;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.machine.serialport.contact.Contacts;
import com.machine.serialport.contact.SmartFaceContacts;
import com.machine.serialport.http.HttpApi;
import com.machine.serialport.service.MipsIDFaceProService;
import com.machine.serialport.service.ServiceSerialPort;
import com.machine.serialport.util.LogUtils;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;
import http.OkHttpUtil;
import http.callback.StringCallback;
import kotlin.jvm.Volatile;
import okhttp3.Call;

public class SerialPortApplication extends Application {
//    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    private SerialPort mSerialPort = null;
    private static SerialPortApplication appContext = null;
    private static final int heartType = 1; //心跳包类型
    private long heartBeatDuration = 10_000; //心跳间隔
    private Handler heartHandle = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case heartType:
                    sendHeartData();
                    break;
            }
        }
    };
    /** Defines callbacks for service binding, passed to bindService()  */
//    private val connection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(
//                className: ComponentName,
//                service: IBinder
//        ) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance
//            val binder: ServiceSerialPort.LocalBinder = service as ServiceSerialPort.LocalBinder
//                    serviceSerialPort = binder.getService()
//            serviceSerialPort?.setSerialPortCallBack(this@LoginByFaceActivity)
//            mBound = true
//        }
//
//        override fun onServiceDisconnected(arg0: ComponentName) {
//            mBound = false
//        }
//    }
    private ServiceConnection faceServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MipsIDFaceProService.Binder serviceBinder = ( MipsIDFaceProService.Binder) service;
            MipsIDFaceProService mipsIDFaceProService = serviceBinder.getService();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mipsIDFaceProService.initMips(appContext,
                            SmartFaceContacts.licPath,
                            SmartFaceContacts.camera_orientation,
                            getAssets(),
                            SmartFaceContacts.choose_alg+""
                            );
                    LogUtils.INSTANCE.logE("YM", "人脸识别服务创建成功");
                }
            }).start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    public void startSmartFaceService(){
        Intent intent = new Intent(this,MipsIDFaceProService.class);
        startService(intent);
        bindService(intent,faceServiceConnection,BIND_AUTO_CREATE);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        initService();
        startSmartFaceService();
        appContext = this;
        sendHeartData();
    }

    public static SerialPortApplication getInstance(){
        return appContext;
    }

    public SerialPort getSerialPort() throws SecurityException, IOException, InvalidParameterException {
        if (mSerialPort == null) {
            /* Read serial port parameters */
//			SharedPreferences sp = getSharedPreferences("android_serialport_api.sample_preferences", MODE_PRIVATE);
            SharedPreferences sp = getSharedPreferences("com.test.serialport_preferences", MODE_PRIVATE);
            String path = sp.getString("DEVICE", "");
            int baudrate = Integer.decode(sp.getString("BAUDRATE", "-1"));
            Log.e("YM","路径为："+path);
            Log.e("YM","波特率为："+baudrate);
            /* Check parameters */
            if ( (path.length() == 0) || (baudrate == -1)) {
                throw new InvalidParameterException();
            }

            /* Open the serial port */
            mSerialPort = new SerialPort(new File(path), baudrate, 0);
        }
        return mSerialPort;
    }
    private void sendHeartData(){
        Log.e("YM","心跳包");
        heartBeatHttp();
        heartHandle.sendEmptyMessageDelayed(heartType,heartBeatDuration);
    }

    private void heartBeatHttp(){
        Log.e("YM","心跳间隔");
        OkHttpUtil.postJson()
                .url(HttpApi.INSTANCE.getURL(HttpApi.DEVICE_BEAT))
                .addParams("id", Contacts.INSTANCE.getTestDeviceId())
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                    }

                    @Override
                    public void onResponse(String response, int id) {
                    }
                });
    }

    public void closeSerialPort() {
        if (mSerialPort != null) {
            mSerialPort.close();
            mSerialPort = null;
        }
    }

    //初始化服务
    private void initService(){
        Intent service = new Intent(this, ServiceSerialPort.class);
        startService(service);
    }

    //销毁服务
    private void destroyService(){

    }

}