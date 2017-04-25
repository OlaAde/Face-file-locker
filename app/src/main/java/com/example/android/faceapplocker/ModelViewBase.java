package com.example.android.faceapplocker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by Abdulkarim on 4/22/2017.
 */

public abstract class ModelViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable{

    private static final String TAG = ModelViewBase.class.getSimpleName();

    public android.hardware.Camera mCamera;
    private  SurfaceHolder mHolder;
    private int mFrameWidth, mFrameHeight;
    private byte[] mFrame;
    private boolean mThread;
    private byte[] mBuffer;


    public ModelViewBase(Context context){
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public int getmFrameWidth() {
        return mFrameWidth;
    }


    public int getmFrameHeight() {
        return mFrameHeight;
    }

    public void setPreview() throws IOException{
        mCamera.setPreviewDisplay(null);
    }

    int getFrontCameraId(){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for (int i = 0; i < Camera.getNumberOfCameras(); i++){
            Camera.getCameraInfo(i,cameraInfo);

            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) return i;
        }
        return -1;
    }

    public boolean openCamera(){
        Log.i(TAG, "Open Camera");
        releaseCamera();

        int cameraId = getFrontCameraId();
        Log.i(TAG, cameraId + "ID");

        mCamera = Camera.open();
        if (mCamera == null){
            Log.e(TAG, "Can't open camera!");
            return false;
        }

        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                synchronized (ModelViewBase.this){
                    System.arraycopy(data, 0, mFrame, 0, data.length);
                    ModelViewBase.this.notify();
                }
                camera.addCallbackBuffer(mBuffer);
            }
        });
        return true;
    }

    public void releaseCamera(){
        Log.i(TAG, "Release Camera");
        mThread = false;
        synchronized (this){
            if (mCamera != null){
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        }
        onPreviewstopped();
    }

    public void setupCamera(int width, int height){
        Log.i(TAG, "Setup Camera");
        synchronized (this){
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            mFrameWidth = width;
            mFrameHeight = height;

            {
                int minDiff = Integer.MAX_VALUE;
                for (Camera.Size size : sizes){
                    if (Math.abs(size.height - height) < minDiff){
                        mFrameWidth = size.width;
                        mFrameHeight = size.height;
                        minDiff = Math.abs(size.height - height);
                    }
                }
            }

            parameters.setPreviewSize(getmFrameWidth(), getmFrameHeight());

            List<String> focusModes = parameters.getSupportedFocusModes();

            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)){
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }

            mCamera.setParameters(parameters);

            parameters = mCamera.getParameters();

            int size = parameters.getPreviewSize().width * parameters.getPreviewSize().height;
            size = size * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat())/8 * 2;
            mBuffer = new byte[size];

            mFrame = new byte[size];
            mCamera.addCallbackBuffer(mBuffer);

            try {
                setPreview();
            }catch (IOException e){
                Log.e(TAG, "mCamera.setPreviewDisplay/setPreviewTexture fails: " + e);
            }

            onPreviewStarted(parameters.getPreviewSize().width, parameters.getPreviewSize().height);

            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "Surface Created");
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.i(TAG, "Surface Changed");
        setupCamera(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Surface Destroyed");
        releaseCamera();
    }

    protected abstract Bitmap processFrame(byte[]data);


    protected abstract void onPreviewStarted(int previewWidth, int perviewHeight);


    protected abstract void onPreviewstopped();

    @Override
    public void run() {
        mThread = true;
        Log.i(TAG, "Start processing thread");
        while (mThread){
            Bitmap bitmap = null;

            synchronized (this){
                try {
                    this.wait();
                    bitmap = processFrame(mFrame);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }

            if (bitmap != null){
                Canvas canvas = mHolder.lockCanvas();
                if (canvas != null){
                    canvas.drawBitmap(bitmap, (canvas.getWidth() - getmFrameWidth()) / 2,
                            (canvas.getHeight() - getmFrameHeight())/2, null);
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }
}
