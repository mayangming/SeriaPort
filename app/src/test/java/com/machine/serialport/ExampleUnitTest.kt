package com.machine.serialport

import com.machine.serialport.util.HexUtil
import org.json.JSONObject
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
//        assertEquals(4, 2 + 2)
//        var list = mutableListOf<Model>()
//        list.add(Model())
//        list.add(Model())
//        list.add(Model())
//        list.add(Model())
//        var count = list.sumOf {
//            it.count
//        }
//        println("结果:${count}")5DDB
        val code = "00031A00000000004A0362002B000000000000000200000000000000016B3E000000000000000000000000000000000000000000000000000000000000000000";
        val inputCode = code.substring(14,18);
        val list = HexUtil.decodeHex(inputCode)
        var result = HexUtil.conver2HexStr(list).reversed()
        result = String.format("%-8s",result).replace(" ","0")
        println("result-->$result")
        println("lockStatus--->${result[0]}")
        println("lockStatus--->${result[1]}")
        println("lockStatus--->${result[2]}")
        println("lockStatus--->${result[3]}")

    }

    @Test
    fun crcTest(){
//        //广播开锁的命令
//        val UN_LOCK = "0006000F000179D8"
          //主机开锁
//        val UN_LOCK = "0306000F000179EB"
          //从机开锁
//        val UN_LOCK = "0406000F0001785C"

//        //广播推拉杆，开门
//        val open_Putter = "0006000E00012818"
//        //主机推拉杆，开门
//        val open_Putter = "0306000E0001282B"
//        //从机推拉杆，开门
//        val open_Putter = "0406000E0001299C"
//        //广播推拉杆，关门
//        val Close_Putter = "0006000E00FFA998"
//        //主机推拉杆，关门
//        val Close_Putter = "0306000E00FFA9AB"
//        //从机推拉杆，关门
//        val Close_Putter = "0406000E00FFA81C"
//        //推拉杆，停止
//        val STOP_Putter = "0006000E00F0E99C"
//        val TEMPORARY_STORAGE_STATUS = "00030000000D85DE"
//        //广播读取寄存器状态
//        val READ_STATUS = "00030000000D85DE"
//        //主机读取寄存器状态
//        val READ_STATUS = "03030000000D85ED"
//        //从机读取寄存器状态
//        val READ_STATUS = "04030000000D845A"
        var crc = CRCUtils.getCRC("04030000000D")
        println("结果:${crc}")
    }

    @Test
     fun isL(){
//        val source = "00031A0000000000420362002B000000000000000200000000000000016B3E000000000000000000000000000000000000000000000000000000000000000000"
//        println("数据长度:${source.length}")
        val a = "0042"
        val b = "004A"
        var result1 = HexUtils.hexString2binaryString(a)
        var result2 = HexUtils.hexString2binaryString(b)
        println("数据长度:$result1")
        println("数据长度:$result2")
    }

    @Test
    fun jsonTest(){
        val jsonObject = JSONObject()
        jsonObject.put("deviceId","1")
        jsonObject.put("codes", mutableListOf("1","2","3"))
        println("--->${jsonObject.toString()}")
    }

}

class Model{
    var count = 1
}