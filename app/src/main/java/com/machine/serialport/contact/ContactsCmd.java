package com.machine.serialport.contact;

//一些命令
public class ContactsCmd {
    //广播开锁的命令
    public static String UN_LOCK_BROADCAST = "0006000F000179D8";
    //主机开锁的命令
    public static String UN_LOCK_MASTER = "0306000F000179EB";
    //从机开锁的命令
    public static String UN_LOCK_SLAVE = "0406000F0001785C";
    //广播推拉杆，开门
    public static String open_Putter_BROADCAST = "0006000E00012818";
    //主机推拉杆，开门
    public static String open_Putter_MASTER = "0306000E0001282B";
    //从机推拉杆，开门
    public static String open_Putter_SLAVE = "0406000E0001299C";
    //广播推拉杆，关门
    public static String Close_Putter_BROADCAST = "0006000E00FFA998";
    //主机推拉杆，关门
    public static String Close_Putter_MASTER = "0306000E00FFA9AB";
    //从机推拉杆，关门
    public static String Close_Putter_SLAVE = "0406000E00FFA81C";
    //广播读取寄存器状态
    public static String READ_STATUS_BROADCAST = "00030000000D85DE";
    //主机读取寄存器状态
    public static String READ_STATUS_MASTER = "03030000000D85ED";
    //从机读取寄存器状态
    public static String READ_STATUS_SLAVE = "04030000000D845A";

    //推拉杆，停止
    public static String STOP_Putter_BROADCAST = "0006000E00F0E99C";

    //开始报警
    public static String ALARM_OPEN = "000600130001B81E";

    //停止报警
    public static String ALARM_CLOSE = "00060013000079DE";

    public static String TEMPORARY_STORAGE_STATUS = "00030000000D85DE";

//    //正转
//    public static String putter_1 = "000600100001481E";
//    //反转
//    public static String putter_2 = "000600100002081F";
}