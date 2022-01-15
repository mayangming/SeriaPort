package com.machine.serialport.view;

import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.List;

/**
 * 相机View
 */
public class MIPSCamera {

    private final static String TAG = "CameraView";
    private final static boolean DEBUG = true;
    public static int CameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private final static int SCALE_TYPE_4_3 = 1; // 自定义属性中4:3比例的枚举对应的值为1
    private final static int SCALE_TYPE_16_9 = 2; // 自定义属性中16:9比例的枚举对应的值为2

    private Camera mCamera; // 相机对象
    private Matrix matrix = new Matrix(); // 记录屏幕拉伸的矩阵，用于绘制人脸框使用
    private PreviewCallback mPreviewCallback; // 相机预览的数据回调
    private Size mPreviewSize; // 当前预览分辨率大小

    private float mPreviewScale; // 预览显示的比例(4:3/16:9)
    private int mResolution; // 分辨率大小，以预览高度为标准(320, 480, 720, 1080...)
    private int mCameraFacing; // 摄像头方向

    public int mPreviewWidth; // 预览宽度
    public int mPreviewHeight; // 预览高度
    public int mDegrees; // 预览显示的角度
    public byte[] mBuffer; // 预览缓冲数据，使用可以让底层减少重复创建byte[]，起到重用的作用
    public List<Size> msupportedPreviewSizes;
    public MIPSCamera() {

    }

    public void openCamera1(int mCameraFacing) throws RuntimeException {
        releaseCamera();
        //mPreviewWidth = width;
        //mPreviewHeight = height;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == mCameraFacing) {
                mCamera = Camera.open(i); // 打开对应的摄像头，获取到camera实例
                break;
            }
        }
        initParameters();
        //initPreviewSize(width,height);
        //initPreviewBuffer();
    }

    public void openCamera(int mCameraFacing,int width, int height) throws RuntimeException {
        releaseCamera();
        mPreviewWidth = width;
        mPreviewHeight = height;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == mCameraFacing) {
                mCamera = Camera.open(i); // 打开对应的摄像头，获取到camera实例
                break;
            }
        }
        initParameters();
        initPreviewSize(width,height);
        //initPreviewBuffer();
    }

    public void initParameters() {
        if (mCamera == null) {
            return;
        }
        try {
            Parameters parameters = mCamera.getParameters();
            // 如果摄像头不支持这些参数都会出错的，所以设置的时候一定要判断是否支持
            List<String> supportedFlashModes = parameters.getSupportedFlashModes();
            if (supportedFlashModes != null && supportedFlashModes.contains(Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Parameters.FLASH_MODE_OFF); // 设置闪光模式
            }
            List<String> supportedFocusModes = parameters.getSupportedFocusModes();
            if (supportedFocusModes != null && supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO); // 设置聚焦模式
            }
            //List<Integer> supportedPreviewFmts = parameters.getSupportedPreviewFormats();
            msupportedPreviewSizes = parameters.getSupportedPreviewSizes();
            parameters.setPreviewFormat(ImageFormat.NV21); // 设置预览图片格式
            parameters.setPictureFormat(ImageFormat.JPEG); // 设置拍照图片格式
            mCamera.setParameters(parameters); // 将设置好的parameters添加到相机里
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 初始化预览尺寸大小并设置，根据拉伸比例、分辨率来计算
     */
    public void initPreviewSize(int width, int height) {
        if (mCamera == null) {
            return;
        }
        Parameters parameters = mCamera.getParameters();
        mPreviewSize = getFitPreviewSize(parameters,width,height); // 获取适合的预览大小
        mPreviewWidth = mPreviewSize.width;
        mPreviewHeight = mPreviewSize.height;
        parameters.setPreviewSize(mPreviewWidth, mPreviewHeight); // 设置预览图片大小
        if (DEBUG) {
            Log.d(TAG, "initPreviewSize() mPreviewWidth: " + mPreviewWidth + ", mPreviewHeight: " + mPreviewHeight);
        }
        mCamera.setParameters(parameters);
    }

    /**
     * 具体计算最佳分辨率大小的方法
     */
    public Size getFitPreviewSize(Parameters parameters, int width, int height) {
        List<Size> previewSizes = parameters.getSupportedPreviewSizes(); // 获取支持的预览尺寸大小
        int minDelta = Integer.MAX_VALUE; // 最小的差值，初始值应该设置大点保证之后的计算中会被重置
        int index = 0; // 最小的差值对应的索引坐标
        for (int i = 0; i < previewSizes.size(); i++) {
            Size previewSize = previewSizes.get(i);
            if (DEBUG) {
                Log.d(TAG, "SupportedPreviewSize, width: " + previewSize.width + ", height: " + previewSize.height);
            }
            // 找到一个与设置的分辨率差值最小的相机支持的分辨率大小
            if (previewSize.width  == width && previewSize.height == height) {
                int delta = Math.abs(mResolution - previewSize.height);
                if (delta == 0) {
                    return previewSize;
                }
                if (minDelta > delta) {
                    minDelta = delta;
                    index = i;
                }
            }
        }
        return previewSizes.get(index); // 默认返回与设置的分辨率最接近的预览尺寸
    }

    public void initPreviewBuffer() {
        if (mCamera == null) {
            return;
        }
        for(int i=0; i<1; i++) {
            //byte[] buffer;
            mBuffer = new byte[mPreviewWidth * mPreviewHeight * 3 / 2]; // 初始化预览缓冲数据的大小
            if (DEBUG) {
                Log.d(TAG, "initPreviewBuffer() mBuffer.length: " + mBuffer.length);
            }
            mCamera.addCallbackBuffer(mBuffer); // 将此预览缓冲数据添加到相机预览缓冲数据队列里
        }
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback); // 设置预览的回调
    }

    /**
     * 设置相机显示的方向，必须设置，否则显示的图像方向会错误
     */
    public void setCameraDisplayOrientation(int rotation) {

        switch (rotation) {
            case Surface.ROTATION_0: // portrait
                mDegrees = 0;
                break;
            case Surface.ROTATION_90: // landscape
                mDegrees = 90;
                break;
            case Surface.ROTATION_180: // portrait-reverse
                mDegrees = 180;
                break;
            case Surface.ROTATION_270: // landscape-reverse
                mDegrees = 270;
                break;
            default:
                mDegrees = 0; // 大部分使用场景都是portrait，默认使用portrait的显示方向
                break;
        }
        if(mCamera!= null){
            mCamera.setDisplayOrientation(mDegrees);
        }
    }

    /**
     * 释放相机资源
     */
    public void releaseCamera() {
        if (null != mCamera) {
            if (DEBUG) {
                Log.v(TAG, "releaseCamera()");
            }
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setPreviewCallback(PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    /**
     * 每次预览的回调中，需要调用这个方法才可以起到重用mBuffer
     */
    public void addCallbackBuffer(byte[] data) {
        if (mCamera != null) {
            mCamera.addCallbackBuffer(mBuffer);
        }
    }

    public Camera getCamera() {
        return mCamera;
    }

    public List<Size> getSupportPreviewSize() {
        if (mCamera == null) {
            return null;
        }
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public Matrix getMatrix() {
        return matrix;
    }

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public boolean isFrontCamera() {
        return mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    public void startPreview(SurfaceHolder holder) {
        if (mCamera != null) {
            if (DEBUG) {
                Log.d(TAG, "startPreview()");
            }

            if(holder != null) {
                try {
                    mCamera.setPreviewDisplay(holder);// set the surface to be used for live preview
                } catch (Exception ex) {
                    if (null != mCamera) {
                        mCamera.release();
                        mCamera = null;
                    }
                }
            }
            mCamera.startPreview();
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            if (DEBUG) {
                Log.d(TAG, "stopPreview()");
            }
            mCamera.stopPreview();
        }
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        if (mCamera != null) {
            if(holder != null) {
                try {
                    mCamera.setPreviewDisplay(holder);// set the surface to be used for live preview
                } catch (Exception ex) {
                    if (null != mCamera) {
                        mCamera.release();
                        mCamera = null;
                    }
                }
            }
        }
    }
}