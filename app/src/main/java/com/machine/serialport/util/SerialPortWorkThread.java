package com.machine.serialport.util;

import android.util.Log;

import com.machine.serialport.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.Arrays;

import android_serialport_api.SerialPort;

//串口扫描的工作线程
public class SerialPortWorkThread extends Thread{

    protected SerialPort mSerialPort;
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private SerialPortDataReceiverIpc serialPortDataReceiverIpc;
    private SerialPortCache serialPortCache;
    private String path;
    private int baudrate;

    @Override
    public void run() {
        super.run();
        initStream();
        while(!isInterrupted()) {
            int size;
            try {
                byte[] buffer = new byte[64];
                if (mInputStream == null) return;
                String value = Arrays.toString(buffer);
//                Log.e("YM","获取读取的值:"+value);
                size = mInputStream.read(buffer);
//                Log.e("YM","size是否大于0:"+(size > 0));
                if (size > 0) {
                    if (null != serialPortDataReceiverIpc){
                        serialPortDataReceiverIpc.onDataReceived(buffer, size);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    //线程创建
    public void create(String path,int baudrate){
        serialPortCache = SerialPortCache.getInstance();
        this.path = path;
        this.baudrate = baudrate;
        start();
    }

    private void initStream(){
        try {
            mSerialPort = serialPortCache.getSerialPort(path,baudrate);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            /* Create a receiving thread */
//            mReadThread = new SerialPortActivity.ReadThread();
//            mReadThread.start();
        } catch (SecurityException e) {
            if (null != serialPortDataReceiverIpc){
                serialPortDataReceiverIpc.onError(R.string.error_security);
            }
        } catch (IOException e) {
            if (null != serialPortDataReceiverIpc){
                serialPortDataReceiverIpc.onError(R.string.error_unknown);
            }
        } catch (InvalidParameterException e) {
            if (null != serialPortDataReceiverIpc){
                serialPortDataReceiverIpc.onError(R.string.error_configuration);
            }
        }
    }

    /**
     * 写入文本
     */
    public void writeText(String text) throws IOException {
        char[] chars;
        chars = new char[((CharSequence) text).length()];
        StringBuilder str = new StringBuilder();
				int i;
				for (i=0; i< ((CharSequence) text).length(); i++) {
//					text[i] = t.charAt(i);
					int ch = (int) ((CharSequence) text).charAt(i);
					String s4 = Integer.toHexString(ch);
					str.append(s4);
				}
        mOutputStream.write(new String(chars).getBytes());
        mOutputStream.write('\n');
    }

    /**
     * 写入十六进制
     */
    public void writeHex(String text) throws IOException {
        mOutputStream.write(HexUtil.decodeHex(text));
//        mOutputStream.write('\n'); //485接口要有这个换行，ttl接口没有这个换行
    }

    //线程销毁
    public void clear(){
        interrupt();
        serialPortCache.closeSerialPort(path);
        mSerialPort = null;
    }

    public void setOnSerialPortDataReceiverIpc(SerialPortDataReceiverIpc serialPortDataReceiverIpc) {
        this.serialPortDataReceiverIpc = serialPortDataReceiverIpc;
    }

    public interface SerialPortDataReceiverIpc{
      void onDataReceived(final byte[] buffer, final int size);
      void onError(int resourceId);
    }

}