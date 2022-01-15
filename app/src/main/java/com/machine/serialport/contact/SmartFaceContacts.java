package com.machine.serialport.contact;

import android.hardware.Camera;

import com.machine.serialport.view.MIPSCamera;

//人脸识别的关键字段
public class SmartFaceContacts {
    //选择的算法模式 0: 人证 1: 人证(单目活体) 2：人证(双目活体) 3：通用(单双目活体) 4：通用
    public static int choose_alg = 0;
    public static int camera_state= Camera.CameraInfo.CAMERA_FACING_BACK;//后置摄像头MIPSCamera
    public static int camera_orientation = 1;//画面方向 0: "横屏" 1: "竖屏" 2: "反向横屏" 3:"反向竖屏"
    public static String licPath = "/sdcard/mipsLic/mipsAi.lic";//软件需要使用授权，这个是授权路径
    static {
        MIPSCamera.CameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    }
}