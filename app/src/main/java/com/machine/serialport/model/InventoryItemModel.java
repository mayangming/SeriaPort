package com.machine.serialport.model;

//盘点数据的条目
public class InventoryItemModel {
    private String id = "0";//数据id
    private String name = "大衣";//数据名字
    private int count = 10; //该类型数据的数量
    private String catId;
    private String catId_dictText;//物品分类
    private String deviceId;
    private String deviceId_dictText;
    private String idX;
    private int totalCount; //物品数量

    public InventoryItemModel() {
    }

    public InventoryItemModel(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getCatId() {
        return catId;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

    public String getCatidDicttext() {
        return catId_dictText;
    }

    public void setCatidDicttext(String catidDicttext) {
        this.catId_dictText = catidDicttext;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceidDicttext() {
        return deviceId_dictText;
    }

    public void setDeviceidDicttext(String deviceidDicttext) {
        this.deviceId_dictText = deviceidDicttext;
    }

    public String getIdX() {
        return idX;
    }

    public void setIdX(String idX) {
        this.idX = idX;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public String toString() {
        return "InventoryItemModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", count=" + count +
                ", catId='" + catId + '\'' +
                ", catid_dicttext='" + catId_dictText + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceid_dicttext='" + deviceId_dictText + '\'' +
                ", idX='" + idX + '\'' +
                ", totalCount=" + totalCount +
                '}';
    }
}