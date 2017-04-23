package com.example.android.faceapplocker;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.opencv.objdetect.CascadeClassifier;
/**
 * Created by Abdulkarim on 4/22/2017.
 */

public class ModelView extends ModelViewBase{

    private int mFrameSize;
    private Bitmap bitmap;
    private int[] mRGBA;

    public static File mCascadeFile;

    public ModelView(Context context){
        super(context);

        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(FaceActivity.working_Dir, "haarcascade_frontalface_alt2.xml");
            FileOutputStream outputStream = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1){
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    protected Bitmap processFrame(byte[] data) {
        int[] rgba = mRGBA;

        FindFeatures(getmFrameWidth(), getmFrameHeight(), data, rgba);

        Bitmap bitmap1 = bitmap;
        bitmap1.setPixels(rgba, 0, getmFrameWidth(), 0 , 0, getmFrameWidth(), getmFrameHeight());
        return bitmap1;
    }

    @Override
    protected void onPreviewStarted(int previewWidth, int perviewHeight) {

        mFrameSize = previewWidth * perviewHeight;
        mRGBA = new int[mFrameSize];
        bitmap = Bitmap.createBitmap(previewWidth, perviewHeight, Bitmap.Config.ARGB_8888);
    }

    @Override
    protected void onPreviewstopped() {
        if (bitmap != null){
            bitmap.recycle();
            bitmap = null;
        }
        mRGBA = null;
    }

    public native void FindFeatures(int width, int height, byte data[], int[] rgba);
    public native void FindFaces(String imageName, String fileName);
    public native int Find(String imageName, String fileName, String csv);
}
