package com.machine.serialport.util;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import android_serialport_api.SerialPort;

//端口缓存管理
//这里就根据路径来进行缓存，如果后期有改动的话再说
public class SerialPortCache {
    private Map<String, SerialPort> stringSerialPortMap = new HashMap<>();
    private static SerialPortCache serialPortCache;
    static {
        serialPortCache = new SerialPortCache();
    }

    public static SerialPortCache getInstance(){
        return serialPortCache;
    }

    /**
     * @param path 串口路径
     * @param baudrate 波特率
     * @return 串口实例
     */
    public SerialPort getSerialPort(String path, int baudrate) throws SecurityException, IOException, InvalidParameterException {
        SerialPort serialPort;
        if (stringSerialPortMap.containsKey(path)){
            serialPort = stringSerialPortMap.get(path);
        }else {
            serialPort = createSerialPort(path,baudrate);
        }
        return serialPort;
    }

    //创建串口实例
    private SerialPort createSerialPort(String path, int baudrate) throws SecurityException, IOException, InvalidParameterException {
//        SharedPreferences sp = getSharedPreferences("com.test.serialport_preferences", MODE_PRIVATE);
//        String path = sp.getString("DEVICE", "");
//        int baudrate = Integer.decode(sp.getString("BAUDRATE", "-1"));
        Log.e("YM","路径为："+path);
        Log.e("YM","波特率为："+baudrate);
        /* Check parameters */
        if ( (path.length() == 0) || (baudrate == -1)) {
            throw new InvalidParameterException();
        }
        /* Open the serial port */
        return new SerialPort(new File(path), baudrate, 0);
    }

    //关闭串口
    public void closeSerialPort(String path){
        if (stringSerialPortMap.containsKey(path)){
            SerialPort serialPort = stringSerialPortMap.get(path);
            serialPort.close();
        }
    }

    //关闭所有串口
    public void closeAllSerialPort(){
        stringSerialPortMap.forEach((k, v) -> {
            v.close();
        });
    }

}