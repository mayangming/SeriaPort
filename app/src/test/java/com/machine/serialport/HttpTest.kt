package com.machine.serialport

import com.machine.serialport.http.SerialPortHttpCallBack
import com.machine.serialport.model.HttpBaseResponseMode
import http.OkHttpUtil
import okhttp3.Call
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

//一个Http网络请求的测试类
class HttpTest {
    @Test
    fun testPost(){
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setBody(
                    """
                        {
                            code:0,
                            message:"success",
                            data:{
                                age:12,
                                name:"张三"
                            }
                        }
                    """.trimIndent()
                )
        )
        mockWebServer.start()
        val mockWebServerUrl = mockWebServer.url("/post")

        val url = "http://${mockWebServerUrl.host}:${mockWebServer.port}${mockWebServerUrl.encodedPath}"
        println("网络请求:$url--->${mockWebServerUrl.encodedPath}")
        OkHttpUtil.postFrom()
            .url(url)
            .build()
            .execute(object : SerialPortHttpCallBack<UserModel>(){
                override fun onError(call: Call?, e: Exception?, id: Int) {
                    e?.printStackTrace()
                }

                override fun onResponse(response: HttpBaseResponseMode<UserModel>?, id: Int) {
                    if (response?.code != 0){
                        return
                    }
                    val data = response.data
                    println("解析的结果:${data?.name}")
                }
            })
        Thread.sleep(5*1000)
    }
}