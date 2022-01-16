package com.machine.serialport.util

import com.machine.serialport.view.FaceCanvasView
import com.smdt.facesdk.mipsFaceInfoTrack

//VIP校验的工具类
object VIPCheckUtil {
    var mState = FaceCanvasView.DETECT_STATE
    fun isVip(mFaceInfoDetected: Array<mipsFaceInfoTrack>):Boolean{
        for (faceinfo in mFaceInfoDetected) {
            // 画识别名
            var name = ""
            var name1: String? = ""
            if (mState == FaceCanvasView.ANALYSIS_STATE) {
                //name += "检测评分:" + faceinfo.mfaceScore;
                //name += ",";
                if (faceinfo.livenessDetectedCnt != 0) {
                    name += faceinfo.livenessDetectedCnt
                    name += ","
                }
            }
            if (faceinfo.flgSetAttr == 1) {
                val analysisInfo: String = FaceCanvasView.getGenderAgeInfo(faceinfo)
                //Log.e("yunboa","flgRefreshFaceAttr ,analysisInfo:" + analysisInfo);
                name1 += analysisInfo
            }
            if (faceinfo.FaceIdxDB >= 0) {
                name += "VIP_" + faceinfo.FaceIdxDB
                name += ","
                //name += "名字:" + faceinfo.name;
/*					name += faceinfo.name;
					name += ",";*/
                //name += "相似度:"+faceinfo.mfaceSimilarity;
                //name += "相似度:"+String.format("%.2f",faceinfo.mfaceSimilarity);
                val mfaceSimilarity = faceinfo.mfaceSimilarity
                if (mfaceSimilarity >= 0.9) {
                    return true
                }
                name += String.format("%.2f", faceinfo.mfaceSimilarity)
                name += ","
                //name += "集合:"+faceinfo.similaritySet;
/*					name += faceinfo.similaritySet;
					name += ",";*/
                //name += "搜索人数:"+faceinfo.serchFaceCount;
/*					name += faceinfo.serchFaceCount;
					name += ",";*/
            }
            if (faceinfo.flgSetLiveness == 1) {

/*					if(faceinfo.livenessScore > 0){
						name1 += "L:" + faceinfo.livenessScore;
						name1 += " ,";
					}*/
                if (faceinfo.flgLiveness == 1) {
                    name1 += ","
                    name1 += "真人"
                } else {
                    name1 += ","
                    name1 += "图片"
                }
            }
        }
        return false
    }

}