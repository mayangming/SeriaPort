package com.machine.serialport.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.machine.serialport.util.LogUtils;
import com.machine.serialport.view.FaceCanvasView;
import com.machine.serialport.view.MIPSCamera;
import com.smdt.deviceauth.Auth;
import com.smdt.facesdk.InitCallback;
import com.smdt.facesdk.mipsDeleteResult;
import com.smdt.facesdk.mipsFaceInfoTrack;
import com.smdt.facesdk.mipsFaceVerifyInfo;
import com.smdt.facesdk.mipsFaceVipDB;
import com.smdt.facesdk.mipsVideoFaceTrack;
import com.smdt.mipsfacesdk.mipsDetectResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Guasszjg on 2016/12/26 0026.
 * Email:guasszjg@gmail.com
 */
//<service android:name=".service.MipsCameraService" />
public class MipsIDFaceProService extends Service implements SurfaceHolder.Callback {

    private static final String TAG = MipsIDFaceProService.class.getSimpleName();
    private WindowManager windowManager;
    private SurfaceView surfaceView;
    protected MIPSCamera mMipsCameera=null;

    protected MIPSCamera mMipsCameeraIR=null;
    //protected Camera mCamera = null;
    //public byte[] mBuffer; // 预览缓冲数据，使用可以让底层减少重复创建byte[]，起到重用的作用
    protected CameraInfo mCameraInfo = null;
    protected int mCameraInit = 0;
    protected SurfaceHolder mSurfaceHolder = null;
    protected SurfaceHolder mSurfaceHolderDisplay = null;
    protected SurfaceHolder mSurfaceHolderIR = null;
    protected SurfaceHolder mSurfaceHolderDisplayIR = null;
    PreviewCallback previewCallback;
    int PREVIEW_WIDTH=0;
    int PREVIEW_HEIGHT=0;
    //final int VIP_FACE_CNT=2784;
    final int VIP_FACE_CNT=1000;
    final int VIP_FACE_CNT_MAX=20000;
    int vip_face_cnt = 1000;
    int CameraFacing = CameraInfo.CAMERA_FACING_BACK;

    private byte nv21[];
    private byte tmp[];
    private boolean isNV21ready = false;
    private byte nv21IR[];
    private byte tmpIR[];
    private boolean isNV21readyIR = false;
    private mipsVideoFaceTrack mfaceTrackLiveness=null;
    private int livenessflg=0;
    private boolean killed = false;
    private Thread mTrackThread;
    private Thread mInitTestThread;
    private Thread thread1;
    private int mcntCurFace=0;
    private int mdbFaceCnt=0;
    private int flgVipFaceVerInit=0;
    private long timeBak=0;
    private long timerIRShutdown=0;
    private int stateIRCamera=1;
    private int framePerSec=0;
    //private int midxDbVerify[];
    //private int mTrackID[];
    //private float faceSimilarity[];
    //private CvAttributeResult mfaceAttributeArray[];
    private int flgFaceChange;
    PoseCallBack mPoseListener;
    ResetCallBack mResetListener;

    private String license;
    Bitmap[] mFaceBipmap;
    mipsFaceInfoTrack[] mFaceInfoDetected;
    private mipsFaceVipDB[] mFaceVipDBArray;
    private final Lock lockFaceDb = new ReentrantLock();
    private final Lock lockFaceInfo = new ReentrantLock();
    private long timeDebug;
    private FaceCanvasView mOverlayCamera=null;
    private volatile boolean mIsTracking=false; // 是否正在进行track操作
    private String VIP_DB_PATH=null;//"/sdcard/mipsfacevip/";
    private int mtrackLivenessID=-1;
    public int mSdktype=-1;
    private int frameCnt=0;
    private int preTrackId=-1;

//    private SmdtManager mSmdtManager;
    private int flagCtlLed = 0;
    private int flagResetDone = 0;
    private volatile boolean mInitok=false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public void mipsEnableFaceAttr(Context context) {
        mfaceTrackLiveness.mipsEnableFaceAttr();
    }

    public void mipsDisableFaceAttr() {
        mfaceTrackLiveness.mipsDisableFaceAttr();
    }


    public class Binder extends android.os.Binder {
        public MipsIDFaceProService getService() {
            return  MipsIDFaceProService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        }
        Log.i(TAG, "onCreate: ");
        mFaceVipDBArray = new mipsFaceVipDB[VIP_FACE_CNT_MAX];

        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        surfaceView = new SurfaceView(this);
        mSurfaceHolder = surfaceView.getHolder();
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
//                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(surfaceView, layoutParams);
        mSurfaceHolder.addCallback(this);

//        mSmdtManager = new SmdtManager(this);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    public void stopCamera()
    {
        if (null != mMipsCameera) {
            /*
            mCamera.setPreviewCallbackWithBuffer(null);
            //mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            */
            mMipsCameera.releaseCamera();
            mMipsCameera = null;

            if(mMipsCameeraIR != null) {
                mMipsCameeraIR.releaseCamera();
                stateIRCamera = 0;
                mMipsCameeraIR = null;
            }
            mCameraInit = 0;
        }
    }

    public int openCamera()
    {

        int cameraCount =  Camera.getNumberOfCameras();
        Log.d("whw","输出相机的数量" +cameraCount);
        if(mCameraInit != 0)
        {
            return cameraCount;
        }

        if(mMipsCameera == null)
        {
            mMipsCameera = new MIPSCamera();
            mMipsCameera.setPreviewCallback(new PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (data == null) {
                        return; // 切分辨率的过程中可能这个地方的data为空
                    }
                    if (!mIsTracking && mInitok) {
                        synchronized (nv21) {
                            System.arraycopy(data, 0, nv21, 0, data.length);
                            isNV21ready = true;
                            //Log.i(TAG, "onPreviewFrame: " + data);
                        }
                        synchronized (mTrackThread) {
                            mTrackThread.notify();
                        }
                    }
                    mMipsCameera.addCallbackBuffer(data); // 将此预览缓冲数据添加到相机预览缓冲数据队列里
                }
            });
        }
        if(mMipsCameera != null ){
            //mMipsCameera.openCamera1(CameraInfo.CAMERA_FACING_BACK);
            mMipsCameera.openCamera1(MIPSCamera.CameraFacing);
        }



        if(mMipsCameeraIR == null && cameraCount >1 )
        {
            mMipsCameeraIR = new MIPSCamera();
            mMipsCameeraIR.setPreviewCallback(new PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (data == null) {
                        return; // 切分辨率的过程中可能这个地方的data为空
                    }
                    if(nv21IR == null) {
                        nv21IR = new byte[data.length];
                    }
                    if(tmpIR == null) {
                        tmpIR = new byte[data.length];
                    }
                    if (!mIsTracking && mInitok) {
                        synchronized (nv21IR) {
                            System.arraycopy(data, 0, nv21IR, 0, data.length);
                            isNV21readyIR = true;
                            //Log.i(TAG, "onPreviewFrame: " + data);
                        }
                        synchronized (mTrackThread) {
                            mTrackThread.notify();
                        }
                    }
                    mMipsCameeraIR.addCallbackBuffer(data); // 将此预览缓冲数据添加到相机预览缓冲数据队列里
                }
            });
        }
        if(mMipsCameeraIR != null ){
            //mMipsCameeraIR.openCamera1(CameraInfo.CAMERA_FACING_FRONT);
            if(MIPSCamera.CameraFacing == CameraInfo.CAMERA_FACING_FRONT) {
                mMipsCameeraIR.openCamera1(CameraInfo.CAMERA_FACING_BACK);
            }
            else
            {
                mMipsCameeraIR.openCamera1(CameraInfo.CAMERA_FACING_FRONT);
            }
        }

        return cameraCount;
    }

    public List<Camera.Size> mipsGetCameraSize()
    {
        if(mMipsCameera != null)
        {
            return mMipsCameera.msupportedPreviewSizes;
        }
        return null;
    }

    //两个摄像头渲染
    public int startCamera(int width, int height, SurfaceHolder holder, SurfaceHolder holderIR, int rotation)
    {
        if(mCameraInit != 0)
        {
            return 0;
        }
        if(width > 0 && height > 0)
        {
            PREVIEW_WIDTH = width;
            PREVIEW_HEIGHT = height;
        }
        mMipsCameera.initPreviewSize(PREVIEW_WIDTH,PREVIEW_HEIGHT);
        if(mCameraInit==0 ) {
            int cameraCount = Camera.getNumberOfCameras();
            if(mMipsCameera != null){
                if(holder != null)
                {
                    LogUtils.INSTANCE.logE("YM","holder!=null");
                    mSurfaceHolderDisplay = holder;
                    //mMipsCameera.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK,PREVIEW_WIDTH,PREVIEW_HEIGHT);
                    mMipsCameera.initPreviewBuffer();
                    mMipsCameera.setCameraDisplayOrientation(rotation);
                    mMipsCameera.startPreview(mSurfaceHolderDisplay);
                    if(mSurfaceHolderDisplay != null ) {
                        mSurfaceHolderDisplay.addCallback(mSurfaceCallback);
                    }
                    //openCamera(CameraFacing,mSurfaceHolderDisplay);
                }
                else
                {
                    LogUtils.INSTANCE.logE("YM","holder==null");
                    //mMipsCameera.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK,PREVIEW_WIDTH,PREVIEW_HEIGHT);
                    mMipsCameera.initPreviewBuffer();
                    mMipsCameera.setCameraDisplayOrientation(rotation);
                    mMipsCameera.startPreview(null);
                    //openCamera(CameraFacing,mSurfaceHolder);
                }
            }
            Log.e("YM","相机数量"+cameraCount);
            if(mMipsCameeraIR != null && cameraCount > 1) {
                mMipsCameeraIR.initPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
                if (holderIR != null) {
                    LogUtils.INSTANCE.logE("YM","holderIR!=null");
                    mSurfaceHolderDisplayIR = holderIR;
                    //mMipsCameera.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK,PREVIEW_WIDTH,PREVIEW_HEIGHT);
                    mMipsCameeraIR.initPreviewBuffer();
                    mMipsCameeraIR.setCameraDisplayOrientation(rotation);
                    mMipsCameeraIR.startPreview(mSurfaceHolderDisplayIR);
                    //openCamera(CameraFacing,mSurfaceHolderDisplay);
                } else {
                    LogUtils.INSTANCE.logE("YM","holderIR==null");
                    //mMipsCameera.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK,PREVIEW_WIDTH,PREVIEW_HEIGHT);
                    mMipsCameeraIR.initPreviewBuffer();
                    mMipsCameeraIR.setCameraDisplayOrientation(rotation);
                    mMipsCameeraIR.startPreview(null);
                    //openCamera(CameraFacing,mSurfaceHolder);
                }
            }
        }


        mCameraInit =1;
        nv21 = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        tmp = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        return 0;
    }

    //单个摄像头渲染
    public int startCamera(int width, int height, SurfaceHolder holder, int rotation)
    {
        if(mCameraInit != 0)
        {
            return 0;
        }
        if(width > 0 && height > 0)
        {
            PREVIEW_WIDTH = width;
            PREVIEW_HEIGHT = height;
        }
        mMipsCameera.initPreviewSize(PREVIEW_WIDTH,PREVIEW_HEIGHT);
        if(mCameraInit==0 ) {
            int cameraCount = Camera.getNumberOfCameras();
            if(mMipsCameera != null){
                if(holder != null)
                {
                    LogUtils.INSTANCE.logE("YM","holder!=null");
                    mSurfaceHolderDisplay = holder;
                    //mMipsCameera.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK,PREVIEW_WIDTH,PREVIEW_HEIGHT);
                    mMipsCameera.initPreviewBuffer();
                    mMipsCameera.setCameraDisplayOrientation(rotation);
                    mMipsCameera.startPreview(mSurfaceHolderDisplay);
                    if(mSurfaceHolderDisplay != null ) {
                        mSurfaceHolderDisplay.addCallback(mSurfaceCallback);
                    }
                    //openCamera(CameraFacing,mSurfaceHolderDisplay);
                }
                else
                {
                    LogUtils.INSTANCE.logE("YM","holder==null");
                    //mMipsCameera.openCamera(Camera.CameraInfo.CAMERA_FACING_BACK,PREVIEW_WIDTH,PREVIEW_HEIGHT);
                    mMipsCameera.initPreviewBuffer();
                    mMipsCameera.setCameraDisplayOrientation(rotation);
                    mMipsCameera.startPreview(null);
                    //openCamera(CameraFacing,mSurfaceHolder);
                }
            }
        }


        mCameraInit =1;
        nv21 = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        tmp = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        return 0;
    }

    private SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.i("yunboa2" , "on surfaceCreated");
            if(mMipsCameera != null){
                if(holder != null)
                {
                    mSurfaceHolderDisplay = holder;
                    mMipsCameera.startPreview(mSurfaceHolderDisplay);
                }
                else
                {
                    mMipsCameera.startPreview(null);
                }
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


            if(mMipsCameeraIR != null && mSurfaceHolderDisplayIR != null) {
              // mMipsCameeraIR.setPreviewDisplay(mSurfaceHolderDisplayIR);
                mMipsCameeraIR.startPreview(mSurfaceHolderDisplayIR);


            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.i("yunboa2" , "on surfaceDestroyed");
        }
    };

    int getLivenessTrackID(mipsFaceInfoTrack[] info)
    {
        int faceWidthMax=0;
        int idx=-1;
        for(int i = 0; i< mipsFaceInfoTrack.MAX_FACE_CNT_ONEfRAME; i++) {
            if (info[i] == null) {
                continue;
            }
            if(info[i].flgSetLiveness == 1 &&info[i].flgLiveness == 0&& mfaceTrackLiveness.mipsGetRefreshFaceLivenessState() == 0)
            {
                continue;
            }
            if(info[i].livenessDetecting == 1)
            {
                return info[i].FaceTRrackID;
            }
            if(info[i].faceRect.width() > faceWidthMax)
            {
                faceWidthMax = info[i].faceRect.width();
                idx = i;
            }
        }
        if(idx >= 0)
        {
            return info[idx].FaceTRrackID;
        }

        return -1;
    }

    public void initDetect(final Context context, String licPath, AssetManager assetManager , final String choose_alg , int distanceType, int degree, final InitCallback initCallback){
//        int resultCode =
        mfaceTrackLiveness = new mipsVideoFaceTrack();

        livenessflg= Integer.parseInt(choose_alg);
        mSdktype= mipsVideoFaceTrack.mipsCheckSdkType(context);
        Log.i(TAG, "sdktype: "+mSdktype);
        mfaceTrackLiveness.mipsInit(context, distanceType, degree, licPath, livenessflg, new InitCallback() {
            @Override
            public void onSucceed() {
                initData(context);
                initTrackerThread();
                if (initCallback != null) {
                    initCallback.onSucceed();
                }
            }

            @Override
            public void onFailed(int code) {
                if (initCallback != null) {
                    initCallback.onFailed(code);
                }
            }
        });
    }

    public int initDetect(final Context context, String licPath, AssetManager assetManager , final String choose_alg , int distanceType, int degree)
    {
        mfaceTrackLiveness = new mipsVideoFaceTrack();
        int ret = -1;

/*        if (choose_alg.equals("0")||choose_alg.equals("1")||choose_alg.equals("4")||choose_alg.equals("5")||choose_alg.equals("6")){
            ret=mfaceTrackLiveness.mipsInit(context,licPath,choose_alg);
            Log.d("yunboa"," initDetect , mipsInit ret :" +ret);
        }
        if (choose_alg.equals("2")||choose_alg.equals("7")){
            ret=mfaceTrackLiveness.mipsInit(context,licPath,2,assetManager,choose_alg);
            Log.d("yunboa"," initDetect , mipsInit ret :" +ret);
        }
        if (choose_alg.equals("3")){
            ret=mfaceTrackLiveness.mipsInit(context,distanceType,licPath);op
            Log.d("yunboa"," initDetect , mipsInit ret :" +ret);
        }*/
        livenessflg= Integer.parseInt(choose_alg);
        mSdktype= mipsVideoFaceTrack.mipsCheckSdkType(context);
        Log.i(TAG, "sdktype: "+mSdktype);

        ret=mfaceTrackLiveness.mipsInit(context, distanceType,degree,licPath, livenessflg);
        if(ret < 0)
        {
            Log.d("yunboa","mipsInit failed , ret "  + ret);
            return ret;
        }
        initData(context);
        initTrackerThread();
/*
        thread1 = new Thread() {
            @Override
            public void run() {
                int faceCntScan;
                int ret;

                ret=mfaceDetect.initFaceDB(context,VIP_DB_PATH+"mipsVipFaceDB",VIP_DB_PATH+"image",1);
                if(ret > 0)//没有VIP人脸库
                {
                    vip_face_cnt = 0;
                    for(int i=0; i<VIP_FACE_CNT_MAX; i++)
                    {
                        mFaceVipDBArray[i] = new mipsFaceVipDB(Environment.getExternalStorageDirectory().getPath()+"/faceVIP/image" + '/' + i + ".jpg",i);
                        mFaceVipDBArray[i].idxInFaceDB = i+1;
                        vip_face_cnt++;
                    }
                    mfaceDetect.mipsEnableVipFaceVerify();
                    flgVipFaceVerInit = 1;
                }
                else if(ret == 0)
                {
                    vip_face_cnt=0;

                    for(int i=0; i<VIP_FACE_CNT_MAX; i++)
                    {
                        mFaceVipDBArray[i] = new mipsFaceVipDB(Environment.getExternalStorageDirectory().getPath()+"/faceVIP/image" + '/' + i + ".jpg",i);
                        mfaceDetect.addOneFaceToDB(context,mFaceVipDBArray[i]);
                        vip_face_cnt++;
                    }
                    mfaceDetect.saveFaceDB(VIP_DB_PATH+"mipsVipFaceDB");

                    mfaceDetect.mipsEnableVipFaceVerify();
                    flgVipFaceVerInit = 1;
                }

            }
        };*/
        //thread1.start();
        return 0;
    }

    private void initData(Context context) {
        VIP_DB_PATH=context.getFilesDir().getAbsolutePath()+ File.separator;
        //mfaceTrackLiveness.initFaceDB(context,VIP_DB_PATH+"mipsVipFaceDB",VIP_DB_PATH+"image",1);

        mfaceTrackLiveness.mipsSetSimilarityThrehold(0.8f);
        mfaceTrackLiveness.mipsSetFaceWidthThrehold(120);
        mfaceTrackLiveness.mipsEnableRefreshFaceRect();
        mfaceTrackLiveness.mipsEnableRefreshFaceAttr();
        mfaceTrackLiveness.initFaceDB(context,VIP_DB_PATH+"mipsVipFaceDB",VIP_DB_PATH+"image",1);
        mfaceTrackLiveness.mipsEnableVipFaceVerify();
        //mfaceTrackLiveness.mipsEnableFaceMoveDetect();
        //mfaceTrackLiveness.mipsSetFaceMoveRitio_THRESHOLD(25);
        mfaceTrackLiveness.mipsEnableIDFeatureCropFace();
        mfaceTrackLiveness.mipsSetIDProCropFaceBorderwidth(50);
        mfaceTrackLiveness.mipsSetFaceScoreThreholdVIP(0.1f);
        mfaceTrackLiveness.mipsSetFaceScoreThrehold(0.1f);
        //mfaceTrackLiveness.mipsDisableVipFastMod();
        mfaceTrackLiveness.mipsSetMaxFaceTrackCnt(2);
        if(mfaceTrackLiveness.mipsGetAlgorithmInfo().equals("SenseTime IDCheck V2"))
        {
            mfaceTrackLiveness.mipsSetSimilarityThreholdMask(0.7f);
        }
        else if(mfaceTrackLiveness.mipsGetAlgorithmInfo().equals("SenseTime IDCheck V3.5.4"))
        {
            mfaceTrackLiveness.mipsSetSimilarityThreholdMask(0.8f);
            mfaceTrackLiveness.mipsSetLivenessMode(0);
        }

        //通用+活体
//        mfaceTrackLiveness.mipsEnableFaceVerifyArea();
//        mfaceTrackLiveness.mipsSetFaceVerifyArea(new Rect(400,100,880,620));
//        mfaceTrackLiveness.mipsFaceVerifyAreaAutoReset();

        mfaceTrackLiveness.mipsEnableMirrorFrame();
    }

    private void initTrackerThread() {
        killed = false;

        //final byte[] tmp = new byte[PREVIEW_WIDTH * PREVIEW_HEIGHT * 2];
        //final byte[] tmp2 = new byte[640 * 480 * 2];
        timerIRShutdown = System.currentTimeMillis();
        mTrackThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!killed) {
                        mInitok = true;
                        if ((isNV21ready && isNV21readyIR && mipsGetFaceLivenessState() > 1)||
                                (isNV21ready && (mipsGetFaceLivenessState() < 2|| mtrackLivenessID<0 ))) {
                            mIsTracking = true;
                            synchronized (nv21) {
                                System.arraycopy(nv21, 0, tmp, 0, nv21.length);
                            }
                            //frameCnt++;
                            //if(frameCnt%2 == 0)
                            {
                                if (mMipsCameeraIR != null && mipsGetFaceLivenessState() > 1 && mtrackLivenessID >= 0) {
                                    if ((nv21IR == null) || (tmpIR == null)) {
                                        continue;
                                    }
                                    synchronized (nv21IR) {
                                        System.arraycopy(nv21IR, 0, tmpIR, 0, nv21IR.length);
                                    }
                                    lockFaceDb.lock();
                                    flgFaceChange = mfaceTrackLiveness.mipsDetectOneFrame(tmp, PREVIEW_WIDTH, PREVIEW_HEIGHT, tmpIR, PREVIEW_WIDTH, PREVIEW_HEIGHT, mtrackLivenessID);
                                    lockFaceDb.unlock();
                                } else {
                                    lockFaceDb.lock();
                                    flgFaceChange = mfaceTrackLiveness.mipsDetectOneFrame(tmp, PREVIEW_WIDTH, PREVIEW_HEIGHT, mtrackLivenessID);
                                    lockFaceDb.unlock();
                                }
                            }
                          /*  if(true) {
                               if((nv21IR != null) && (tmpIR != null)) {
                                    synchronized (nv21IR) {
                                        System.arraycopy(nv21IR, 0, tmpIR, 0, nv21IR.length);
                                    }
                                    lockFaceDb.lock();
                                    //flgFaceChange = mfaceTrackLiveness.mipsDetectOneFrame(tmp, PREVIEW_WIDTH, PREVIEW_HEIGHT, tmpIR, PREVIEW_WIDTH, PREVIEW_HEIGHT, mtrackLivenessID);

                                    if(choose_alg.equals("0")||choose_alg.equals("4")||choose_alg.equals("5")||choose_alg.equals("6")){
                                        flgFaceChange = mfaceTrackLiveness.mipsDetectOneFrame(tmp, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                                    }
                                    if(choose_alg.equals("1")){
                                        flgFaceChange = mfaceTrackLiveness.mipsDetectOneFrame(tmp, PREVIEW_WIDTH, PREVIEW_HEIGHT,mtrackLivenessID);
                                    }
                                    if(choose_alg.equals("2")||choose_alg.equals("7")){
                                        flgFaceChange = mfaceTrackLiveness.mipsDetectOneFrame(tmp, PREVIEW_WIDTH, PREVIEW_HEIGHT, tmpIR, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                                    }
                                    if(choose_alg.equals("3")){
                                        flgFaceChange = mfaceTrackLiveness.mipsDetectOneFrame(tmp, PREVIEW_WIDTH, PREVIEW_HEIGHT,tmpIR,mtrackLivenessID);
                                    }

                                    lockFaceDb.unlock();
                                }
                            }*/

                            //byte[] data=FileUtil.readFileFromSDCard3("/sdcard/4.yuv");
                            //System.arraycopy(data, 0, tmp, 0, data.length);

/*
                            mfaceTrackLiveness.mipsVerifyID(tmp,PREVIEW_WIDTH,PREVIEW_HEIGHT,null);
*/
                            /** if (timeBak == 0) {
                                timeBak = System.currentTimeMillis();
                            }
                            if ((System.currentTimeMillis() - timeBak) > 1000) {
                                //Log.i(TAG, "frameRate: " + framePerSec);
                                framePerSec = 0;
                                timeBak = System.currentTimeMillis();
                            }
                            framePerSec++;*/

                            /**if(mMipsCameeraIR != null) {
                                if (mfaceTrackLiveness.mipsGetFaceCnt() <= 0) {
                                    if(stateIRCamera == 1) {
                                        if ((System.currentTimeMillis() - timerIRShutdown) > 10000) {
                                            mMipsCameeraIR.stopPreview();
                                            stateIRCamera = 0;
                                        }
                                    }
                                } else if(mipsGetFaceLivenessState() > 1) {
                                    timerIRShutdown = System.currentTimeMillis();
                                    if(stateIRCamera == 0) {
                                        if (mSurfaceHolderDisplayIR != null) {
                                            mMipsCameeraIR.setPreviewCallbackWithBuffer();
                                            mMipsCameeraIR.startPreview(mSurfaceHolderDisplayIR);
                                        } else {
                                            mMipsCameeraIR.setPreviewCallbackWithBuffer();
                                            mMipsCameeraIR.startPreview(null);
                                        }
                                        stateIRCamera = 1;
                                    }
                                }
                            }*/

                            if (timeBak == 0) {
                                timeBak = System.currentTimeMillis();
                            }

                            if (flgFaceChange == 1) {

                                mcntCurFace = mfaceTrackLiveness.mipsGetFaceCnt();
                                //mdbFaceCnt = mfaceTrackLiveness.mipsGetDbFaceCnt();
                                mFaceInfoDetected = mfaceTrackLiveness.mipsGetFaceInfoDetected();
                               // Log.d("mFaceInfoDetected" , "mcntCurFace : " + mcntCurFace + " , " + Arrays.toString(mFaceInfoDetected) );
                                if(mipsGetFaceLivenessState()>0){
                                    mtrackLivenessID=getLivenessTrackID(mFaceInfoDetected);
                                }
                               // timeDebug = System.currentTimeMillis();



                             //重置跟踪
                               if(timeDebug == 0 || (System.currentTimeMillis() -  timeDebug) > 5000) {
                                    timeDebug = System.currentTimeMillis();
                                    if (preTrackId >= 0) {
                                        Log.d("yunboa","debug: " + mfaceTrackLiveness.mipsGetTrackerStatus(preTrackId));

                                        mipsResetTracker(preTrackId, 0, 1, 0, 0);
                                        flagResetDone = 0;
                                    }
                                }
//                                }else{
//                                    if((timeDebug == 0 || (System.currentTimeMillis() -  timeDebug) > 3000) && (flagResetDone == 0 )){
//                                        mResetListener.onReset(); // for reset track id to display picture
//                                        flagResetDone = 1;
//                                    }
//                                }

                                /************************************************************************/
                                if(mFaceInfoDetected != null){
                                    for (mipsFaceInfoTrack faceinfo : mFaceInfoDetected) {

                                        if (faceinfo == null) {
                                            continue;
                                        }

                                        if(preTrackId != faceinfo.FaceTRrackID && faceinfo.flgSetVIP == 1){
                                        //if(faceinfo.flgSetVIP == 1){
                                            preTrackId = faceinfo.FaceTRrackID;
                                            Log.d("yunboa","trackId: " + faceinfo.FaceTRrackID + ", userId: " + faceinfo.FaceIdxDB +", score: " +faceinfo.mfaceSimilarity + ",cnt:" + faceinfo.vipVerifiedCnt);
                                        }
                                    }

                                }
                                /************************************************************************/

                                if (mOverlayCamera != null) {
                                    mOverlayCamera.addFacesLiveness(mFaceInfoDetected, FaceCanvasView.ANALYSIS_STATE);
                                    mOverlayCamera.postInvalidate();
                                }

                                mPoseListener.onPosedetected("pose", mcntCurFace, mdbFaceCnt, mFaceInfoDetected);

                                //Log.d("yunboa","flagCtlLed ：" +  flagCtlLed + " , 可见光 检测到人脸了了，但是灯是 灭 着的状态，打开它");
                                if(flagCtlLed == 0) { //如果灭灯状态，打开灯
//                                    if(mSmdtManager != null) {
//                                        mSmdtManager.smdtSetControl(3, 1);
//                                    }
                                    flagCtlLed = 1;
                                }

                            }else {

                                if (mMipsCameeraIR != null && mipsGetFaceLivenessState() > 1 ) {

                                    if ((System.currentTimeMillis() - timeBak) > 1000) {
                                        //Log.i(TAG, "frameRate: " + framePerSec);

                                        timeBak = System.currentTimeMillis();
                                        //Log.d("yunboa","检测不到人脸超过 1s 钟");

                                        if (tmpIR == null) {
                                            continue;
                                        }
                                        synchronized (nv21IR) {
                                            System.arraycopy(nv21IR, 0, tmpIR, 0, nv21IR.length);
                                        }

                                        lockFaceDb.lock();
                                        List<mipsDetectResult> result = mfaceTrackLiveness.mipsDetectFaces(tmpIR,PREVIEW_WIDTH,PREVIEW_HEIGHT);
                                        lockFaceDb.unlock();
                                        if(result != null && result.size() > 0  ){  // 检测到人脸

                                            //Log.d("yunboa","flagCtlLed ：" +  flagCtlLed + " , 红外检测到人脸了了，但是灯是 灭 着的状态，打开它");
                                            if(flagCtlLed == 0) {
//                                                if (mSmdtManager != null) {
//                                                    mSmdtManager.smdtSetControl(3, 1);
//                                                }
                                                flagCtlLed = 1;
                                            }
                                        }else{  //检测不到人脸 ，但已经控制了亮灯了，关闭灯


                                            //Log.d("yunboa","flagCtlLed ：" +  flagCtlLed + " , 检测不到人脸了，但是灯是亮着的状态，关闭它");
                                            if(flagCtlLed == 1) {
//                                                if(mSmdtManager != null) {
//                                                    mSmdtManager.smdtSetControl(3, 0);
//                                                }
                                                flagCtlLed = 0;
                                            }

                                        }

                                    }
                                }
                            }
                            mIsTracking = false;
                            isNV21ready = false;
                            isNV21readyIR = false;
                        }
                        else {
                            synchronized (this) {
                                mTrackThread.wait(); // 数据没有准备好就等待
                            }
                       }
                    }
                    if(mfaceTrackLiveness != null) {
                        mfaceTrackLiveness.mipsUninit();
                    }
                    Log.i(TAG, "mTrackThread exit: ");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    stopThread();
                }
            }
        };
        mTrackThread.start();
    }

    public int startDetect(Context context, String licPath, int width, int height, SurfaceHolder holder, SurfaceHolder holderIR, int rotation, AssetManager assetManager, String choose_alg)
    {
        int ret=0;
         if(((PREVIEW_WIDTH!= width) || (PREVIEW_HEIGHT!=height)))
         {
             if(mCameraInit != 0)
             {
                 stopCamera();
             }
             LogUtils.INSTANCE.logE("YM","开启相机:"+rotation);
             startCamera(width,height,holder,holderIR,rotation);
             try {
                 Thread.sleep(1000);
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
             if(mfaceTrackLiveness == null)
             {
                 Log.d("whw", "nihao"+ choose_alg);
                 ret = initDetect(context,licPath,assetManager,choose_alg ,1,rotation);
             }
             if(ret < 0)
             {
                 mfaceTrackLiveness = null;
                 return ret;
             }
         }

        return 0;
    }
    //单个摄像头渲染
    public int startDetect(Context context, String licPath, int width, int height, SurfaceHolder holder, int rotation, AssetManager assetManager, String choose_alg)
    {
        int ret=0;
         if(((PREVIEW_WIDTH!= width) || (PREVIEW_HEIGHT!=height)))
         {
             if(mCameraInit != 0)
             {
                 stopCamera();
             }
             LogUtils.INSTANCE.logE("YM","开启相机:"+rotation);
             startCamera(width,height,holder,rotation);
             try {
                 Thread.sleep(1000);
             } catch (InterruptedException e) {
                 e.printStackTrace();
             }
             if(mfaceTrackLiveness == null)
             {
                 Log.d("whw", "nihao"+ choose_alg);
                 ret = initDetect(context,licPath,assetManager,choose_alg ,1,rotation);
             }
             if(ret < 0)
             {
                 mfaceTrackLiveness = null;
                 return ret;
             }
         }

        return 0;
    }

    //两个摄像头渲染
    public int startDetect(Context context, String licPath, final int width, final int height, final SurfaceHolder holder, final SurfaceHolder holderIR, final int rotation, AssetManager assetManager, String choose_alg, final InitCallback initCallback)
    {
         if(((PREVIEW_WIDTH!= width) || (PREVIEW_HEIGHT!=height)))
         {
             if(mfaceTrackLiveness == null)
             {
                 Log.d("whw", "nihao"+ choose_alg);
                 initDetect(context, licPath, assetManager, choose_alg, 1, rotation, new InitCallback() {
                     @Override
                     public void onSucceed() {
                         if (initCallback != null) {
                             initCallback.onSucceed();
                         }
                         if(mCameraInit != 0)
                         {
                             stopCamera();
                         }
                         startCamera(width,height,holder,holderIR,rotation);
                     }

                     @Override
                     public void onFailed(int code) {
                         if (initCallback != null) {
                             initCallback.onFailed(code);
                         }
                         mfaceTrackLiveness = null;
                     }
                 });
             }

         }

        return 0;
    }

    //初始化必要的人脸识别服务
    public int initMips(Context context, String licPath, final int rotation, AssetManager assetManager, String choose_alg){
        int ret = 0;
        if(mfaceTrackLiveness == null)
        {
            Log.d("YM-->", "MipsIDFaceProSerface-->人脸服务初始化"+ choose_alg);
            ret = initDetect(context,licPath,assetManager,choose_alg ,1,rotation);
        }
        if(ret < 0)
        {
            mfaceTrackLiveness = null;

        }
        return ret;
    }

    public mipsFaceVipDB getVipFaceInfo(int idxInDb)
    {
        for(int i=0; i<vip_face_cnt; i++)
        {
            if(mFaceVipDBArray[i] == null)
            {
                continue;
            }
            if(mFaceVipDBArray[i].idxInFaceDB == idxInDb)
            {
                return mFaceVipDBArray[i];
            }
        }

        return null;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private void stopThread() {
        killed = true;
        if (mTrackThread != null) {
            mTrackThread.interrupt();
            try {
                mTrackThread.interrupt();
                mTrackThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopservice()
    {
        Log.i(TAG, "SurfaceHolder.Callback?Surface Destroyed");
        stopThread();
        if (null != mMipsCameera) {
            /*
            mCamera.setPreviewCallbackWithBuffer(null);
            //mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            */
            mMipsCameera.stopPreview();
            mMipsCameera.releaseCamera();
            mMipsCameera = null;
        }

        if (null != mMipsCameeraIR) {
            /*
            mCamera.setPreviewCallbackWithBuffer(null);
            //mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            */
            mMipsCameeraIR.stopPreview();
            stateIRCamera = 0;
            mMipsCameeraIR.releaseCamera();
            mMipsCameeraIR = null;
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
    }

    public interface PoseCallBack {
        public void onPosedetected(String flag, int curFaceCnt, int cntFaceDB, mipsFaceInfoTrack[] faceInfo);
    }
    public void registPoseCallback( MipsIDFaceProService.PoseCallBack callback) {
        mPoseListener = callback;
    }


    public interface ResetCallBack {
        public void onReset();
    }

    public void registResetCallback( MipsIDFaceProService.ResetCallBack callback) {
        mResetListener = callback;
    }

    //设置人脸检测角度阈值
    public void mipsSetRollAngle(float threhold)
    {
        lockFaceDb.lock();
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsSetVIPRollAngle(threhold);
        }
        lockFaceDb.unlock();
    }
    public float mipsGetRollAngle()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1.0f;
        }
        return mfaceTrackLiveness.mipsGetVIPRollAngle();
    }

    public void mipsSetFaceWidthThrehold(int width)
    {
        mfaceTrackLiveness.mipsSetVIPDetectFaceWidthThrehold(width);
    }
    public int mipsGetFaceWidthThrehold()
    {
        return mfaceTrackLiveness.mipsGetVIPDetectFaceWidthThrehold();
    }
    public void mipsSetPicFaceWidthThrehold(int width)
    {
        mfaceTrackLiveness.mipsSetPicFaceWidthThrehold(width);
    }
    public int mipsGetPicFaceWidthThrehold()
    {
        return mfaceTrackLiveness.mipsGetPicFaceWidthThrehold();
    }
    //设置人脸检测角度阈值
    public void mipsSetYawAngle(float threhold)
    {
        lockFaceDb.lock();
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsSetVIPYawAngle(threhold);
        }
        lockFaceDb.unlock();
    }
    public float mipsGetYawAngle()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1.0f;
        }
        return mfaceTrackLiveness.mipsGetVIPYawAngle();
    }
    //设置人脸检测角度阈值
    public void mipsSetPitchAngle(float threhold)
    {
        lockFaceDb.lock();
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsSetVIPPitchAngle(threhold);
        }
        lockFaceDb.unlock();
    }
    public float mipsGetPitchAngle()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1.0f;
        }
        return mfaceTrackLiveness.mipsGetVIPPitchAngle();
    }
    //设置人脸检测角度阈值
    public void mipsSetFaceScoreThrehold(float threhold)
    {
        lockFaceDb.lock();
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsSetFaceScoreThrehold(threhold);
        }
        lockFaceDb.unlock();
    }
    public float mipsGetFaceScoreThrehold()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1.0f;
        }
        return mfaceTrackLiveness.mipsGetFaceScoreThrehold();
    }
    //设置人脸相似度阈值
    public void mipsSetSimilarityThrehold(float threhold)
    {
        lockFaceDb.lock();
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsSetSimilarityThrehold(threhold);
        }
        lockFaceDb.unlock();
    }
    public float mipsGetSimilarityThrehold()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1.0f;
        }
        return mfaceTrackLiveness.mipsGetSimilarityThrehold();
    }
    //设置人脸图片校验分数阈值
    public void mipsSetVerifyScoreThrehold(float threhold)
    {
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsSetVerifyScoreThrehold(threhold);
        }
    }
    public int mipsGetLivenessFaceWidthThrehold()
    {
        return mfaceTrackLiveness.mipsGetLivenessFaceWidthThrehold();
    }

    public void mipsSetLivenessFaceWidthThrehold(int width)
    {
        mfaceTrackLiveness.mipsSetLivenessFaceWidthThrehold(width);
    }
    public float mipsGetVerifyScoreThrehold()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1.0f;
        }
        return mfaceTrackLiveness.mipsGetVerifyScoreThrehold();
    }
    //设置摄像头追踪最大人脸数
    public void mipsSetMaxFaceTrackCnt(int cnt)
    {
        lockFaceDb.lock();
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsSetMaxFaceTrackCnt(cnt);
        }
        lockFaceDb.unlock();
    }
    public int mipsGetMaxFaceTrackCnt()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1;
        }
        return mfaceTrackLiveness.mipsGetMaxFaceTrackCnt();
    }
    //使能VIP人脸校验
    public void mipsEnableVipFaceVerify()
    {
        lockFaceDb.lock();
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsEnableVipFaceVerify();
        }
        lockFaceDb.unlock();
    }
    //禁止VIP人脸校验
    public void mipsDisableVipFaceVerify()
    {
        lockFaceDb.lock();

        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsDisableVipFaceVerify();
        }

        lockFaceDb.unlock();
    }
    //获取VIP人脸校验状态
    public int mipsGetVipFaceVerifyState()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1;
        }
        return mfaceTrackLiveness.mipsGetVipFaceVerifyState();
    }

    private int mipsGetFreeFaceVip()
    {
        for(int i=0;i<VIP_FACE_CNT_MAX; i++)
        {
            if(mFaceVipDBArray[i] ==null)
            {
                return i;
            }
        }

        return -1;
    }
    /**
     * 复制单个文件
     * @param oldPath String 原文件路径 如：c:/fqf.txt
     * @param newPath String 复制后路径 如：f:/fqf.txt
     * @return boolean
     */
    private void copyFile(String oldPath, String newPath) {
        try {
            int bytesum = 0;
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { //文件不存在时
                String name = oldfile.getName();
                File targetPath = new File(newPath);
                if(!targetPath.exists()){
                    targetPath.mkdirs();
                }

                InputStream inStream = new FileInputStream(oldPath); //读入原文件
                FileOutputStream fs = new FileOutputStream(newPath + name);
                byte[] buffer = new byte[1444];
                int length;
                while ( (byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    //System.out.println(bytesum);
                    fs.write(buffer, 0, byteread);

                }
                inStream.close();

                /*if(oldfile.isFile()){
                    oldfile.delete();
                }*/


            }
        }
        catch (Exception e) {
            System.out.println("复制单个文件操作出错");
            e.printStackTrace();

        }
    }

    public int mipsVerifyVipImage(String[] imagePath, int imageCnt) {
        if(imagePath ==null)
        {
            return -1;
        }
        lockFaceInfo.lock();
        long t1 = System.currentTimeMillis();
        int ret=mfaceTrackLiveness.verifyVipImage(imagePath,imageCnt);
        Log.d("yunboa","mipsVerifyVipImage take time :" + (System.currentTimeMillis() -  t1));
        lockFaceInfo.unlock();

        return ret;
    }




    public Bitmap convertBitmap(Bitmap srcBitmap) {
        int width = srcBitmap.getWidth();
        int height = srcBitmap.getHeight();

        //Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas();
        Matrix matrix = new Matrix();

        matrix.postScale(-1, 1);

        Bitmap newBitmap2 = Bitmap.createBitmap(srcBitmap, 0, 0, width, height, matrix, true);

        canvas.drawBitmap(newBitmap2,
                new Rect(0, 0, width, height),
                new Rect(0, 0, width, height), null);

        return newBitmap2;

    }


    public Bitmap convert(Bitmap a, int width, int height)
    {
        int w = a.getWidth();
        int h = a.getHeight();
        Bitmap newb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        Canvas cv = new Canvas(newb);
        Matrix m = new Matrix();
        m.postScale(-1, 1); //镜像水平翻转
        Bitmap new2 = Bitmap.createBitmap(a, 0, 0, w, h, m, true);
        cv.drawBitmap(new2, new Rect(0, 0, new2.getWidth(), new2.getHeight()),new Rect(0, 0, width, height), null);
        return newb;
    }



    public int mipsGetDbFaceCnt()
    {
        return mfaceTrackLiveness.mipsGetDbFaceCnt();
    }

    //获取VIP人脸校验状态
    public int mipsAddVipFace(Context context, String imagePath, int cnt, boolean flgCopyImageFile)
    {
        lockFaceInfo.lock();

        int idx=mipsGetFreeFaceVip();

        if(idx < 0)
        {
            lockFaceInfo.unlock();
            return -1;
        }


        mFaceVipDBArray[idx] = new mipsFaceVipDB(imagePath,cnt);


        Bitmap sourceBitmapFace = BitmapFactory.decodeFile(imagePath);
        mFaceVipDBArray[idx].sourceBitmapFace = convertBitmap(sourceBitmapFace);


        long timg1 = System.currentTimeMillis();
        int ret=mfaceTrackLiveness.addOneFaceToDB(context,mFaceVipDBArray[idx]);
        long timg2 = System.currentTimeMillis();
        Log.d("yunboa","addOneFaceToDB take time :" + (timg2 -  timg1) + ", ret:" + ret);
        if(mFaceVipDBArray[idx].sourceBitmapFace != null)
        {
            mFaceVipDBArray[idx].sourceBitmapFace.recycle();
            mFaceVipDBArray[idx].sourceBitmapFace =null;
        }
        if(ret >= 0){
            //for test
            //mfaceTrackLiveness.saveFaceDB(VIP_DB_PATH+"mipsVipFaceDB");
            //

            //String path = Environment.getExternalStorageDirectory().getPath()+"/faceVIP/imageVIP" + '/' + (cnt) + ".jpg";
            String path = Environment.getExternalStorageDirectory().getPath()+"/faceVIP/imageVIP"+ '/';
            if(flgCopyImageFile) {
                copyFile(imagePath, path);
            }
            mFaceVipDBArray[idx].imagePath = path;
            ret = 0;
        }
        else
        {
            mFaceVipDBArray[idx] = null;
            ret = -1;
        }
        lockFaceInfo.unlock();
        return ret;
    }

    //获取VIP人脸校验状态
    public int mipsAddVipFace(Context context, String imagePath)
    {
        lockFaceInfo.lock();

        int idx=mipsGetFreeFaceVip();

        if(idx < 0)
        {
            lockFaceInfo.unlock();
            return -1;
        }


        mFaceVipDBArray[idx] = new mipsFaceVipDB(imagePath,vip_face_cnt);
        int ret=mfaceTrackLiveness.addOneFaceToDB(context,mFaceVipDBArray[idx]);
        if(ret >= 0){
            //for test
            //mfaceTrackLiveness.saveFaceDB(VIP_DB_PATH+"mipsVipFaceDB");
            //
            String path = Environment.getExternalStorageDirectory().getPath()+"/faceVIP/image" + '/' + (vip_face_cnt) + ".jpg";
            copyFile(imagePath, path);
            mFaceVipDBArray[idx].imagePath = path;
            vip_face_cnt++;
            ret = 0;
        }
        else
        {
            mFaceVipDBArray[idx] = null;
            ret = -1;
        }
        lockFaceInfo.unlock();
        return ret;
    }
    public void mipsSetSurface(SurfaceHolder holder, SurfaceHolder holderIR)
    {
        try {
            Log.i(TAG, "SurfaceHolder.Callback?surface Created");
            if(mMipsCameera != null)
            {
                mMipsCameera.setPreviewDisplay(holder);// set the surface to be used for live preview

                Log.i("yunboa2", "mipsSetSurface mMipsCameera");
            }
            if(mMipsCameeraIR != null) {
                mMipsCameeraIR.setPreviewDisplay(holderIR);

                Log.i("yunboa2" , "mipsSetSurface mMipsCameeraIR");
            }
        } catch (Exception ex) {
			Log.i(TAG + "initCamera", ex.getMessage());
            Log.e("yunboa2" , "mipsSetSurface setPreviewDisplay error");
        }
    }

    public void mipsSetOverlay(FaceCanvasView overlay)
    {
        mOverlayCamera = overlay;
        if(mOverlayCamera != null) {
            mOverlayCamera.setFaceVerifyRect(mfaceTrackLiveness.mipsGetFaceVerifyArea());
        }
    }

    public long mipsGetTimeDebug()
    {
        return timeDebug;
    }

    private int mipsfindFaceVipID(int idxInFaceDB)
    {
        if(idxInFaceDB < 0)
        {
            return -1;
        }
        for(int i=0;i<VIP_FACE_CNT_MAX; i++)
        {
            if(mFaceVipDBArray[i] ==null)
            {
                continue;
            }
            if(mFaceVipDBArray[i].idxInFaceDB == idxInFaceDB)
            {
                return i;
            }
        }

        return -1;
    }

    //获取VIP人脸校验状态
    public int mipsDeleteVipFace(Context context, int idxInFaceDB)
    {
        lockFaceInfo.lock();

        //int idx=mipsfindFaceVipID(idxInFaceDB);
        //if(idx < 0)
        //{
        //    lockFaceInfo.unlock();
        //    return -1;
        //}

        int ret=mfaceTrackLiveness.deleteOneFaceFrDB(context,idxInFaceDB);
        if(ret >= 0) {
            vip_face_cnt--;
            //for test
            //mfaceTrackLiveness.saveFaceDB(VIP_DB_PATH+"mipsVipFaceDB");
            //
            //mFaceVipDBArray[idx] = null;
        }
        lockFaceInfo.unlock();
        return ret;
    }

    public  static String mipsGetDeviceInfo(Context context)
    {
        String deviceInfo=null;

        try {
            deviceInfo = Auth.mipsGetDeviceInfo() + "\n";
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return deviceInfo;
    }

    //设置摄像头横屏
    public void mipsSetTrackLandscape()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsSetTrackLandscape();
            if(mMipsCameera != null){
                mMipsCameera.setCameraDisplayOrientation(0);
            }

            if(mMipsCameeraIR != null) {
                mMipsCameeraIR.setCameraDisplayOrientation(0);
            }
        }
    }
    //设置摄像头横屏
    public void mipsSetTrackPortrait()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsSetTrackPortrait();
            if(mMipsCameera != null) {
                mMipsCameera.setCameraDisplayOrientation(1);
            }

            if(mMipsCameeraIR != null) {
                mMipsCameeraIR.setCameraDisplayOrientation(1);
            }
        }
    }

    //设置摄像头反向横屏
    public void mipsSetTrackReverseLandscape()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsSetTrackReverseLandscape();
            if(mMipsCameera != null) {
                mMipsCameera.setCameraDisplayOrientation(2);
            }

            if(mMipsCameeraIR != null) {
                mMipsCameeraIR.setCameraDisplayOrientation(2);
            }
        }
    }
    //设置摄像头反向横屏
    public void mipsSetTrackReversePortrait()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsSetTrackReversePortrait();
            if(mMipsCameera != null) {
                mMipsCameera.setCameraDisplayOrientation(3);
            }

            if(mMipsCameeraIR != null) {
                mMipsCameeraIR.setCameraDisplayOrientation(3);
            }
        }
    }

    //使能人脸属性提取
    public void mipsEnableFaceLiveness(Context context)
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsEnableFaceLiveness();
        }
    }
    //禁止V人脸属性提取
    public void mipsDisableFaceLiveness()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsDisableFaceLiveness();
        }
    }
    //获取VIP人脸校验状态
    public int mipsGetFaceLivenessState()
    {
        if(mfaceTrackLiveness == null) {
            return -1;
        }
        return mfaceTrackLiveness.mipsGetFaceLivenessState();
    }

    public void mipsEnableRefreshFaceLiveness()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsEnableRefreshFaceLiveness();
        }
    }
    public void mipsDisableRefreshFaceLiveness()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsDisableRefreshFaceLiveness();
        }
    }
    //获取实时刷新人脸属性状态
    public int mipsGetRefreshFaceLivenessState()
    {
        if(mfaceTrackLiveness == null) {
            return -1;
        }
        return mfaceTrackLiveness.mipsGetRefreshFaceLivenessState();
    }

    public void mipsEnableRefreshFaceVIP()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsEnableRefreshFaceVIP();
        }
    }
    public void mipsDisableRefreshFaceVIP()
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsDisableRefreshFaceVIP();
        }
    }
    //获取实时刷新VIP状态
    public int mipsGetRefreshFaceVIPState()
    {
        if(mfaceTrackLiveness == null) {
            return -1;
        }
        return mfaceTrackLiveness.mipsGetRefreshFaceVIPState();
    }

    public void mipsSetLivenessMode(int mod)
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsSetLivenessMode(mod);
        }
    }
    public int mipsGetLivenessMode()
    {
        if(mfaceTrackLiveness == null) {
            return -1;
        }
        return mfaceTrackLiveness.mipsGetLivenessMode();
    }

    public void mipsSetLivenessType(int mod)
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsSetLivenessType(mod);
        }
    }
    public int mipsGetLivenessType()
    {
        if(mfaceTrackLiveness == null) {
            return -1;
        }
        return mfaceTrackLiveness.mipsGetLivenessType();
    }

    public void mipsSetLivenessDetectCnt(int cnt)
    {
        if(mfaceTrackLiveness != null) {
            mfaceTrackLiveness.mipsSetMaxLivenessDetectCnt(cnt);
        }
    }
    public int mipsGetLivenessDetectCnt()
    {
        if(mfaceTrackLiveness == null) {
            return -1;
        }
        return mfaceTrackLiveness.mipsGetLivenessDetectCnt();
    }

    //获取实时刷新VIP状态
    public int mipsStartLoopInit(final Context context, final String licPath, final int second)
    {
   /*
        String pathFile=context.getFilesDir().getAbsolutePath();
        license = FileUtil.readFileFromSDCard2(pathFile+"/"+"license.lic");
        if(license == null) {
            return -1;
        }
        mfaceDetect = new mipsVideoFaceTrack();

        mInitTestThread = new Thread() {
            @Override
            public void run() {
                {
                    while (!killed) {

                        //int ret=mfaceDetect.checkLicense(context,license);
                        int ret=mfaceDetect.mipsLicenseVerify(license);
                        if(ret < 0)
                        {
                            break;
                        }
                        try {
                            Thread.sleep(1000*second);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        mInitTestThread.start();
*/
        return 0;
    }

    /*
    //设置摄像头横屏
    public void mipsSetTrackReverseLandscape()
    {
        if(mfaceDetect != null) {
            mfaceDetect.mipsSetTrackReverseLandscape();
        }
    }
    //设置摄像头横屏
    public void mipsSetTrackReversePortrait()
    {
        if(mfaceDetect != null) {
            mfaceDetect.mipsSetTrackReversePortrait();
        }
    }
    */

    public float mipsCompareFeatureInfaceDb(Context context , int userId1, int userId2) {

        lockFaceInfo.lock();
        float ret= 1; //mfaceTrackLiveness.mipsCompareFeatureInfaceDb(context,userId1,userId2);
        lockFaceInfo.unlock();

        return ret;
    }
    public String mipsGetVersion()
    {
        return mipsVideoFaceTrack.mipsGetVersion();
    }

    public float mipsGetLivenessThresholdBinocular()
    {
        if(mfaceTrackLiveness == null)
        {
            return -1.0f;
        }
        return mfaceTrackLiveness.mipsGetLivenessScoreThreholdBinocular();
    }

    public void mipsSetLivenessThreholdBinocular(float threshold)
    {
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.mipsSetLivenessScoreThreholdBinocular(threshold);
        }
    }

    public void capturePictures(String name) {
        if(mfaceTrackLiveness != null)
        {
            mfaceTrackLiveness.capturePictures(name);
        }
    }
    public mipsFaceVerifyInfo verifyVipImage(Bitmap bitmap){
        if(mfaceTrackLiveness != null){
           return mfaceTrackLiveness.verifyVipImage(bitmap);
        }
        return null;
    }

    public List<mipsDeleteResult> mipsDeleteFaces(Context context , int[] ids){
        if(mfaceTrackLiveness !=null){
            return mfaceTrackLiveness.deleteBulkFacesFrDB(context,ids);
        }
        return null;
    }

    public void mipsResetTracker(int trackId ,int flagLiveness, int flagVip, int flagIdPro ,int flagAttr)
    {
        lockFaceInfo.lock();
        mfaceTrackLiveness.mipsResetTracker(trackId , flagLiveness, flagVip, flagIdPro, flagAttr);
        lockFaceInfo.unlock();
    }

    //设置摄像头横屏
    public String mipsGetAlgorithmInfo()
    {
        if(mfaceTrackLiveness != null) {
            return mfaceTrackLiveness.mipsGetAlgorithmInfo();
        }
        return "null";
    }
}