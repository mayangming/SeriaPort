package com.machine.serialport.util;

import com.machine.serialport.contact.ContactsCmd;

//根据不同场景获取不同的串口命令
public class SerialPortCmdUtil {
    public static int scene= 0;
    //广播场景
    public static int SCENE_BROADCAST = 0;
    //主机场景
    public static int SCENE_MASTER = 1;
    //从机场景
    public static int SCENE_SLAVE = 2;

    //设置具体的场景
    public static void setScene(int sceneValue){
        scene = sceneValue;
    }

    //获取开锁命令
    public static String getUnlockCmd(){
        String cmd = ContactsCmd.UN_LOCK_BROADCAST;
        if (scene == SCENE_BROADCAST){
            cmd = ContactsCmd.UN_LOCK_BROADCAST;
        }
        if (scene == SCENE_MASTER){
            cmd = ContactsCmd.UN_LOCK_MASTER;
        }
        if (scene == SCENE_SLAVE){
            cmd = ContactsCmd.UN_LOCK_SLAVE;
        }
        return cmd;
    }
    //获取开推拉杆命令
    public static String getOpenPutterCmd(){
        String cmd = ContactsCmd.open_Putter_BROADCAST;
        if (scene == SCENE_BROADCAST){
            cmd = ContactsCmd.open_Putter_BROADCAST;
        }
        if (scene == SCENE_MASTER){
            cmd = ContactsCmd.open_Putter_MASTER;
        }
        if (scene == SCENE_SLAVE){
            cmd = ContactsCmd.open_Putter_SLAVE;
        }
        return cmd;
    }
    //获取关推拉杆命令
    public static String getClosePutterCmd(){
        String cmd = ContactsCmd.Close_Putter_BROADCAST;
        if (scene == SCENE_BROADCAST){
            cmd = ContactsCmd.Close_Putter_BROADCAST;
        }
        if (scene == SCENE_MASTER){
            cmd = ContactsCmd.Close_Putter_MASTER;
        }
        if (scene == SCENE_SLAVE){
            cmd = ContactsCmd.Close_Putter_SLAVE;
        }
        return cmd;
    }
    //获取读取状态的命令
    public static String getReadStatusCmd(){
        String cmd = ContactsCmd.READ_STATUS_BROADCAST;
        if (scene == SCENE_BROADCAST){
            cmd = ContactsCmd.READ_STATUS_BROADCAST;
        }
        if (scene == SCENE_MASTER){
            cmd = ContactsCmd.READ_STATUS_MASTER;
        }
        if (scene == SCENE_SLAVE){
            cmd = ContactsCmd.READ_STATUS_SLAVE;
        }
        return cmd;
    }

}