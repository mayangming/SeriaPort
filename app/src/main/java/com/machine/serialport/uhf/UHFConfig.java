package com.machine.serialport.uhf;

import com.pow.api.cls.RfidPower;

//UHF串口基本配置，这里值定为固定值
//UHF主要是处理跟电子标签相关的功能
//因为该程序只运行在一种设备上，所以所有值均为固定值
public class UHFConfig {
    public static String uhfAddress =  "/dev/ttyS1";//盘点标签的串口地址
    public static RfidPower.PDATYPE uhfType =  RfidPower.PDATYPE.NONE;//盘点标签的平台类型
    public static int portc = 16;//天线数量
    public static int[] unats = new int[]{1,2,3,4,5,6,7,8};//天线接口
}