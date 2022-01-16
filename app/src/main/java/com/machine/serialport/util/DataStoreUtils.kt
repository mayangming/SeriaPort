package com.machine.serialport.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.machine.serialport.SerialPortApplication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val DATA_STORE_KEY = "inventory"
val Context.myDataStore by preferencesDataStore(DATA_STORE_KEY)

class DataStoreUtils {
    //字段的key，需要使用stringPreferencesKey包装一下
    private val cacheKey = stringPreferencesKey("inventory_cache")
    private val tokenKey = stringPreferencesKey("token")
    private val faceDataKey = stringPreferencesKey("face_data")
    private val deviceIdKey = stringPreferencesKey("device_id")
    private val subDeviceIdKey = stringPreferencesKey("sub_device_id")//副柜的id
    private val context = SerialPortApplication.getInstance()
    companion object{
        @Volatile
        private var INSTANCE: DataStoreUtils? = null
        fun getInstance(): DataStoreUtils {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let {
                    return it
                }
                val instance = DataStoreUtils()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun putInventoryCache(cache: String){
        context.myDataStore.edit {
            it[cacheKey] = cache
        }
    }

    suspend fun getInventoryCache(): String {
        val nameFlow: Flow<String> = context.myDataStore.data.map { settings ->
            settings[cacheKey] ?: ""
        }
        return nameFlow.first()
    }

    suspend fun putFaceDataCache(faceData: String){
        context.myDataStore.edit {
            it[faceDataKey] = faceData
        }
    }

    suspend fun getFaceDataCache(): String {
        val nameFlow: Flow<String> = context.myDataStore.data.map { settings ->
            settings[faceDataKey] ?: ""
        }
        return nameFlow.first()
    }

    suspend fun putInventoryToken(token: String){
        context.myDataStore.edit {
            it[tokenKey] = token
        }
    }

    suspend fun getInventoryToken(): String {
        val nameFlow: Flow<String> = context.myDataStore.data.map { settings ->
            settings[tokenKey] ?: ""
        }
        return nameFlow.first()
    }

    suspend fun putDeviceId(deviceId: String){
        context.myDataStore.edit {
            it[deviceIdKey] = deviceId
        }
    }

    suspend fun getDeviceId(): String {
        val nameFlow: Flow<String> = context.myDataStore.data.map { settings ->
            settings[deviceIdKey] ?: ""
        }
        return nameFlow.first()
    }


    //副柜的id
    suspend fun putSubDeviceId(subDeviceId: String){
        context.myDataStore.edit {
            it[subDeviceIdKey] = subDeviceId
        }
    }

    //获取副柜的id
    suspend fun getSubDeviceId(): String {
        val nameFlow: Flow<String> = context.myDataStore.data.map { settings ->
            settings[subDeviceIdKey] ?: ""
        }
        return nameFlow.first()
    }

}