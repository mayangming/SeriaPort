package com.machine.serialport.http

import com.google.gson.GsonBuilder
import com.machine.serialport.model.HttpBaseResponseMode
import http.callback.Callback
import okhttp3.Response
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


//网络请求的公共数据处理
abstract class SerialPortHttpCallBack<T>: Callback<HttpBaseResponseMode<T>>() {
    private val gson = GsonBuilder().create()
    private val clazz: Class<*> = HttpBaseResponseMode::class.java
    override fun parseNetworkResponse(response: Response, id: Int): HttpBaseResponseMode<T>? {
        //Response.body().string()方法在调用后会关闭Response.body(),另外string()方法只能打印1M之内的内容,若打印内容超过1M需要使用流进行打印
        val json = response.body?.string()
        if (json.isNullOrEmpty()){
            return null
        }
        return parse(json)
    }


    private fun parse(json: String): HttpBaseResponseMode<T>? {
        try {
            val type = this.javaClass.genericSuperclass as ParameterizedType
            val objectType: Type = buildType(clazz, *type.actualTypeArguments)
            return gson.fromJson(json, objectType)
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
        return null
    }

    private fun buildType(raw: Class<*>, vararg args: Type): ParameterizedType {
        return object : ParameterizedType {
            override fun getRawType(): Type {
                return raw
            }

            override fun getActualTypeArguments(): Array<out Type> {
                return args
            }

            override fun getOwnerType(): Type? {
                return null
            }
        }
    }
}