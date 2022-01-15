package com.machine.serialport.activity

import android.content.*
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import cn.trinea.android.common.util.ToastUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.machine.serialport.R
import com.machine.serialport.contact.Contacts
import com.machine.serialport.contact.SmartFaceContacts
import com.machine.serialport.http.HttpApi
import com.machine.serialport.http.SerialPortHttpCallBack
import com.machine.serialport.model.HttpBaseResponseMode
import com.machine.serialport.model.UserFaceModel
import com.machine.serialport.model.UserModel
import com.machine.serialport.service.MipsIDFaceProService
import com.machine.serialport.service.ServiceDataIpc
import com.machine.serialport.service.ServiceSerialPort
import com.machine.serialport.util.*
import com.machine.serialport.view.FaceCanvasView
import com.smdt.facesdk.mipsFaceInfoTrack
import http.OkHttpUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import java.lang.reflect.Type
import java.util.*


//人脸识别的页面
class LoginByFaceActivity : BaseActivity(), ServiceConnection, ServiceDataIpc {
    private val REQUEST_CODE_1 = 0x001
    private var isIniting = false//是否初始化了
    private var strFileAdd: String? = null
    var mIntent: Intent ?= null
    var mCameraSize: List<Camera.Size>? = null
    private var camcnt = 0 //摄像头数量
    private var mipsFaceService: MipsIDFaceProService? = null
    private var cntVIP:Int = 0//该值用于添加VIP时候使用
    private var mfaceOverlay: FaceCanvasView? = null//人脸识别的窗口
    private var faceUserCache: MutableList<UserFaceModel> = mutableListOf()

    var mSurfaceviewCamera: SurfaceView? = null
    var mSurfaceHolderCamera: SurfaceHolder? = null
    private val surface_width = 0
    private var surface_left = 0
    private var surface_right = 0
    private var surface_top = 0
    private var surface_bottom = 0
    private var camera_w = 0
    private var camera_h:Int = 0
    private var isPause = false //是否暂停过页面
    private var serviceSerialPort: ServiceSerialPort? = null
    private var mBound = false
    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: ServiceSerialPort.LocalBinder = service as ServiceSerialPort.LocalBinder
            serviceSerialPort = binder.getService()
            serviceSerialPort?.setSerialPortCallBack(this@LoginByFaceActivity)
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_face)
        initFaceCacheData()
        initView()
        initSmartFaceView()
        initPermission()
        bindSerialPortService()
    }

    private fun initFaceCacheData(){
        GlobalScope.launch {
            val faceData = DataStoreUtils.getInstance().getFaceDataCache()
            Log.e(">>>>>>>>>>>>", "获取数据:$faceData-->数据是否为空:${TextUtils.isEmpty(faceData)}")
            if (TextUtils.isEmpty(faceData)){
                return@launch
            }
            val type: Type = object : TypeToken<ArrayList<UserFaceModel>>() {}.type
            faceUserCache = Gson().fromJson(faceData,type)
        }

    }
    private fun faceServiceIsRuning(){}

    private fun initPermission(){

        XXPermissions.with(this)
            .permission(Permission.SYSTEM_ALERT_WINDOW)
//            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
                        ToastUtils.show(this@LoginByFaceActivity, "权限已授予")
                        startSmartFaceService()

                    } else {
                        ToastUtils.show(this@LoginByFaceActivity, "获取部分权限成功，但部分权限未正常授予")

                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        ToastUtils.show(this@LoginByFaceActivity, "被永久拒绝授权，请手动授予悬浮窗权限")
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(this@LoginByFaceActivity, permissions)
                    } else {
                        ToastUtils.show(this@LoginByFaceActivity, "获取悬浮窗权限失败")
                    }
                }
            })
    }

    private fun initView(){
        mfaceOverlay = findViewById(R.id.canvasview_draw)
        mSurfaceviewCamera = findViewById(R.id.surfaceViewCamera)
        mSurfaceHolderCamera = mSurfaceviewCamera?.holder

        findViewById<View>(R.id.add_vip).setOnClickListener(this::onClick)
        findViewById<View>(R.id.choose_vip).setOnClickListener(this::onClick)
        findViewById<View>(R.id.quit).setOnClickListener(this::onClick)
        findViewById<View>(R.id.back_green).setOnClickListener(this::onClick)

    }

    //绑定服务
    private fun bindSerialPortService() {
        // Bind to LocalService
        val intent = Intent(this, ServiceSerialPort::class.java)
        startService(intent)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }


    //解绑服务
    private fun unBindService() {
        unbindService(connection)
        mBound = false
    }

    var isDeliver = false//数据是否处理中
    //通过人脸识别登录
    private fun loginForFace(faceId: Int){
        LogUtils.logE(mess = "是否在处理登录数据:$isDeliver")
        if (isDeliver){
            return
        }
        isDeliver = true
        var phone = ""
        faceUserCache.forEach {
            if (it.faceId == faceId){
                phone = it.facePhone
            }
        }
        if(TextUtils.isEmpty(phone)){
            ToastUtils.show(this@LoginByFaceActivity,"没有录入人脸信息，请录入后再进行操作!")
            return
        }
        OkHttpUtil.postJson()
            .url(HttpApi.getURL(HttpApi.FACE_LOGIN))
            .addParams("code",phone)
            .build().execute(object : SerialPortHttpCallBack<UserModel>(){
                override fun onError(call: Call?, e: Exception?, id: Int) {
                    ToastUtils.show(this@LoginByFaceActivity,"网络，请检查网络!")
//                    isDeliver = false
                }

                override fun onResponse(response: HttpBaseResponseMode<UserModel>, id: Int) {
                   if (!response.success){
                       ToastUtils.show(this@LoginByFaceActivity,"服务出错，请联系运营商，错误信息:${response.msg}")
//                       isDeliver = false
                       return
                   }
                    val data = response.data
                    GlobalScope.launch {
                        LogUtils.logE(mess = "设置本次的token:${data!!.token}")
                        DataStoreUtils.getInstance().putInventoryToken(data!!.token)
                        goHome()
//                        isDeliver = false
                    }
                }


            })
    }

    private fun unLock(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        if (AppUtils.isDistribute(packageName)) {//假如是分发柜
//            serviceSerialPort?.writeHex(ContactsCmd.UN_LOCK_BROADCAST)
            serviceSerialPort?.writeHex(SerialPortCmdUtil.getUnlockCmd())
        }else{//回收柜的话开推拉杆
//            serviceSerialPort?.writeHex(ContactsCmd.OPEN_Putter)
//            serviceSerialPort?.writeHex(ContactsCmd.open_Putter_BROADCAST)
            serviceSerialPort?.writeHex(SerialPortCmdUtil.getOpenPutterCmd())
        }
    }
    //进入首页
    private fun goHome(){
        unLock()
        startActivity(
            Intent(
                this@LoginByFaceActivity,
                MainActivity::class.java
            )
        )
        finish()
    }

    private fun onClick(v: View) {
        when(v.id){
            R.id.add_vip -> {
                if (strFileAdd == null) {
                    Toast.makeText(applicationContext, "未选择图片", Toast.LENGTH_SHORT).show()
                }
                LogUtils.logE("YM", "添加VIP")
                addVip()
            }
            R.id.choose_vip -> {
                chooseBitmap(REQUEST_CODE_1)
            }
            R.id.quit -> {
                finish()
            }
            R.id.back_green -> {
                finish()
            }
        }
    }

    private fun addVip(){
        var ret = -1;
        val vipCount = mipsFaceService!!.mipsGetDbFaceCnt()
        ret = mipsFaceService!!.mipsAddVipFace(this, strFileAdd, vipCount, true)
        if (ret < 0) {
            Toast.makeText(applicationContext, "添加VIP失败", Toast.LENGTH_SHORT).show()
        } else {
            cntVIP++
            Toast.makeText(applicationContext, "添加VIP成功", Toast.LENGTH_SHORT).show()
        }
        strFileAdd = null
    }


    override fun onStart() {
        super.onStart()
    }

    override fun onPause() {
        super.onPause()
        isPause = true
        mipsFaceService?.stopCamera()
        LogUtils.logE(mess = "关闭相机")
    }
    override fun onPostResume() {
        super.onPostResume()
        if (isPause && mipsFaceService != null) {
            isPause = false
            val w: Int = camera_w
            val h: Int = camera_h
            mipsFaceService!!.openCamera()
            mipsFaceService!!.startCamera(w, h, mSurfaceHolderCamera, SmartFaceContacts.camera_orientation)
        }
    }

    private fun initSmartFaceView(){
        //设置相机是前置摄像头还是后置摄像头
    }

    private fun initSmartFace(){
        if(!isIniting){
            LogUtils.logE("YM","渲染页面")
            Thread{
                isIniting = true
                var ret: Int = 0
                var w = 640 //宽
                var h = 480 //高
                var licPath = SmartFaceContacts.licPath//授权路径
                var similarity: Float
                var liveness_threshold: Float
                var face_score: Float
                var track_cnt: Int
                var faceWidth: Int
                var faceWidthPic: Int
                var roll: Float
                var pitch: Float
                var YAW: Float
                var vip: Int
                var attr: Int
                var vipRefresh: Int
                var attrRefresh: Int
                var livenessfacewidth: Int
                var live_mode: Int
                var live_type: Int
                var live_cnt: Int
                if (mCameraSize == null) {
                    LogUtils.logE(mess = "打开相机")
                    camcnt = mipsFaceService!!.openCamera()
                    if (camcnt == 0) {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "未检测到摄像头",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@Thread
                    }
                    mCameraSize = mipsFaceService?.mipsGetCameraSize()
                }
                //mipsFaceService.algorithm_version = algorithm_version;
                //显示画面

//                ret =  mipsFaceService!!.startDetect(
//                    applicationContext,
//                    licPath,
//                    w,
//                    h,
//                    mSurfaceHolderCamera,
//                    mSurfaceHolderCameraIR,
//                    SmartFaceContacts.camera_state,
//                    assets,
//                    SmartFaceContacts.choose_alg.toString()
//                )


//                ret =  mipsFaceService!!.startDetect(
//                    applicationContext,
//                    licPath,
//                    w,
//                    h,
//                    mSurfaceHolderCamera,
//                    SmartFaceContacts.camera_orientation,
//                    assets,
//                    SmartFaceContacts.choose_alg.toString()
//                )

                mipsFaceService!!.openCamera()
                mipsFaceService!!.startCamera(w, h, mSurfaceHolderCamera, SmartFaceContacts.camera_orientation)

                //不显示画面
//                ret = mipsFaceService!!.startDetect(
//                    applicationContext, licPath, w, h, null, null, SmartFaceContacts.camera_state,
//                    assets, SmartFaceContacts.choose_alg.toString()
//                )
                if (ret >= 0) {//返回初始化的结果
                    similarity = mipsFaceService!!.mipsGetSimilarityThrehold() // 相似度

                    face_score = mipsFaceService!!.mipsGetFaceScoreThrehold()
                    track_cnt = mipsFaceService!!.mipsGetMaxFaceTrackCnt()
                    roll = mipsFaceService!!.mipsGetRollAngle()
                    pitch = mipsFaceService!!.mipsGetPitchAngle()
                    YAW = mipsFaceService!!.mipsGetYawAngle()
                    vip = mipsFaceService!!.mipsGetVipFaceVerifyState()
                    attr = mipsFaceService!!.mipsGetFaceLivenessState()
                    vipRefresh = mipsFaceService!!.mipsGetRefreshFaceVIPState()
                    attrRefresh = mipsFaceService!!.mipsGetRefreshFaceLivenessState()
                    liveness_threshold = mipsFaceService!!.mipsGetLivenessThresholdBinocular()
                    faceWidth = mipsFaceService!!.mipsGetFaceWidthThrehold()
                    faceWidthPic = mipsFaceService!!.mipsGetPicFaceWidthThrehold()
                    cntVIP = mipsFaceService!!.mipsGetDbFaceCnt()
                    Log.e("YM", "获取当前VIP人脸库的数量:$cntVIP")
                    livenessfacewidth = mipsFaceService!!.mipsGetLivenessFaceWidthThrehold()
                    live_mode = mipsFaceService!!.mipsGetLivenessMode()
                    //live_type = mipsFaceService.mipsGetLivenessType();
                    //live_cnt = mipsFaceService.mipsGetLivenessDetectCnt();
                    val left = mSurfaceviewCamera!!.left
                    val right = mSurfaceviewCamera!!.right
                    val top = mSurfaceviewCamera!!.top
                    val bottom = mSurfaceviewCamera!!.bottom
                    surface_left = left
                    surface_right = right
                    surface_top = top
                    surface_bottom = bottom
                    camera_w = w
                    camera_h = h
                    mfaceOverlay!!.setOverlayRect(left, right, top, bottom, h, w)
                    mipsFaceService!!.mipsSetOverlay(mfaceOverlay)
                    setCameraState(SmartFaceContacts.camera_orientation)
                    LogUtils.logE("YM", "YM-------->渲染成功")
                    runOnUiThread {
                        ToastUtils.show(this,"人脸识别服务已经启动可以使用")
                    }

                } else {
                    if (ret == -7) {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "请确认授权文件是否正确", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else if (ret == -1) {
                        runOnUiThread {
                            Toast.makeText(
                                applicationContext,
                                "请确保系统时间及AI硬件授权是否正确,mSdktype:" + mipsFaceService!!.mSdktype,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(applicationContext, "SDK初始化失败:$ret", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
                isIniting = false
            }.start()
        }else{
            ToastUtils.show(this, "人脸识别功能初始化中，请稍后...")
        }
    }

    private fun startSmartFaceService(){
        mIntent = Intent(this, MipsIDFaceProService::class.java)
//        startService(mIntent)
        bindService(mIntent, this, BIND_AUTO_CREATE)
    }

    private fun stopSmartFaceService(){
        if (ServiceUtils.isServiceRunning(this, MipsIDFaceProService::class.java.name)){
//            mipsFaceService?.stopservice()
            unbindService(this)
//            stopService(mIntent)
            LogUtils.logE("YM", "人脸识别服务已关闭")
        }else{
            LogUtils.logE("YM", "人脸识别服务没有运行")
        }

    }

    override fun onStop() {
        super.onStop()

    }

    override fun onDestroy() {
        super.onDestroy()
        stopSmartFaceService()
//        mipsFaceService?.stopCamera()
//        LogUtils.logE(mess = "相机关闭-------ondestory")
        unBindService()
    }

    private var isLogining = false //是否登陆中

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MipsIDFaceProService.Binder
        mipsFaceService = binder.service
        mipsFaceService?.registPoseCallback(object : MipsIDFaceProService.PoseCallBack {
            override fun onPosedetected(
                flag: String,
                curFaceCnt: Int,
                cntFaceDB: Int,
                faceInfo: Array<mipsFaceInfoTrack>
            ) {
                // TODO Auto-generated method stub
                runOnUiThread {
                    if ("pose" == flag) {
                        var i: Int
                        i = 0
                        while (i < mipsFaceInfoTrack.MAX_FACE_CNT_ONEfRAME) {
                            //if((faceInfo[i].mBitmapFaceIR ==null&&mipsFaceService.mipsGetLivenessMode()==0) || faceInfo[i].mfaceFeature == null || faceInfo[i].mfaceFeature.mBitmapFace == null)
                                if(null == faceInfo[i]){
                                    return@runOnUiThread
                                }
//                            LogUtils.logE(mess = "mFace是否为空:${faceInfo[i].mfaceFeature == null}")
//                            LogUtils.logE(mess = "mBitmapFace是否为空:${faceInfo[i].mfaceFeature.mBitmapFace == null}")
//                            if (faceInfo[i].mfaceFeature == null || faceInfo[i].mfaceFeature.mBitmapFace == null) {
//                                i++
//                                continue
//                            }
                            LogUtils.logE(mess = "获取的mfaceSimilarity:${faceInfo[i].mfaceSimilarity}")
                            if (faceInfo[i].mfaceSimilarity >= 0.90){
                                var faceId = faceInfo[i].FaceIdxDB
                                LogUtils.logE(mess = "识别出来的人脸ID:$faceId")
                                LogUtils.logE(mess = "是否正在登录中:$isLogining")
//                                ToastUtils.show(this@LoginByFaceActivity,"识别通过")
                                if(isLogining){
                                    return@runOnUiThread
                                }
                                isLogining = true
                                loginForFace(faceId)
                                return@runOnUiThread
                            }
                            i++

                        }
                        //cnt_cur_faceText.setText("当前画面人数: "+curFaceCnt);
                    }
                    System.currentTimeMillis()
                }
            }
        })

//        refreshCamera()
        initSmartFace()
    }
    private fun refreshCamera(): Int {
        camcnt = mipsFaceService!!.openCamera()
        mCameraSize = mipsFaceService!!.mipsGetCameraSize()
        return 0
    }

    private fun setCameraState(state: Int) {
        if (state == 0) {
            if (mipsFaceService != null) {
                mipsFaceService!!.mipsSetTrackLandscape()
            }
            mfaceOverlay!!.setOverlayRect(
                surface_left,
                surface_right,
                surface_top,
                surface_bottom,
                camera_w,
                camera_h
            )
            mfaceOverlay!!.setCavasLandscape()
        } else if (state == 1) {
            if (mipsFaceService != null) {
                mipsFaceService!!.mipsSetTrackPortrait()
            }
            mfaceOverlay!!.setOverlayRect(
                surface_left,
                surface_right,
                surface_top,
                surface_bottom,
                camera_h,
                camera_w
            )
            mfaceOverlay!!.setCavasPortrait()
        } else if (state == 2) {
            if (mipsFaceService != null) {
                mipsFaceService!!.mipsSetTrackReverseLandscape()
            }
            mfaceOverlay!!.setOverlayRect(
                surface_left,
                surface_right,
                surface_top,
                surface_bottom,
                camera_w,
                camera_h
            )
            mfaceOverlay!!.setCavasReverseLandscape()
        } else if (state == 3) {
            if (mipsFaceService != null) {
                mipsFaceService!!.mipsSetTrackReversePortrait()
            }
            mfaceOverlay!!.setOverlayRect(
                surface_left,
                surface_right,
                surface_top,
                surface_bottom,
                camera_h,
                camera_w
            )
            mfaceOverlay!!.setCavasReversePortrait()
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }
    //选择图片
    private fun chooseBitmap(requestCode: Int) {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(photoPickerIntent, requestCode)
    }

    override fun onDataReceived(buffer: ByteArray, size: Int) {
        //接收的数据
        val receiverContent = String(buffer, 0, size)
//        LogUtils.logE(mess = "接收的内容${ HexUtil.encodeHexStr(buffer)}")

    }

    override fun onError(resourceId: Int) {
        //错误信息
        DisplayError(resourceId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            return
        }
        val imageUri = data.data
        if (requestCode == REQUEST_CODE_1) {
            strFileAdd = getRealFilePath(baseContext, imageUri) //getPhotoPathFromContentUri(getBaseContext(),imageUri);
            LogUtils.logE("Ym", "选择的图片路径:$strFileAdd")
        }
    }
    fun getRealFilePath(context: Context, uri: Uri?): String? {
        if (null == uri) return null
        val scheme = uri.scheme
        var data: String? = null
        if (scheme == null) data = uri.path else if (ContentResolver.SCHEME_FILE == scheme) {
            data = uri.path
        } else if (ContentResolver.SCHEME_CONTENT == scheme) {
            val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null)
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                    if (index > -1) {
                        data = cursor.getString(index)
                    }
                }
                cursor.close()
            }
        }
        return data
    }
}