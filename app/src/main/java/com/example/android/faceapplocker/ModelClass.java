package com.example.android.faceapplocker;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by Abdulkarim on 4/22/2017.
 */

public class ModelClass extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = ModelClass.class.getSimpleName();
    private ModelView mView;
    Button okButton, clickButton, cancelButton, tryAgainButton;
    ProgressDialog progressDialog;
    Bitmap bitmap;
    ImageView imageView;
    FrameLayout previewLayout;
    int count;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        imageView = (ImageView) findViewById(R.id.image);
        previewLayout = (FrameLayout)findViewById(R.id.preview_frame_layout);



        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_2, this, mOpenCVCallBack)){
            Log.e(TAG, "Cannot connect to OpenCv Manager");
        }
    }

    public ModelClass(){
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        if (null != mView){
            mView.releaseCamera();
        }
    }

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback() {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCv loaded successfully");

                    System.loadLibrary("native_sample");

                    mView = new ModelView(mAppContext);
                    setContentView(R.layout.capture);
                    previewLayout.addView(mView);
                    previewLayout.setVisibility(VISIBLE);
                    imageView.setVisibility(GONE);

                    clickButton = (Button) findViewById(R.id.capture_button);
                    clickButton.setVisibility(VISIBLE);
                    clickButton.setOnClickListener(ModelClass.this);

                    tryAgainButton = (Button) findViewById(R.id.recapture_button);
                    tryAgainButton.setVisibility(GONE);
                    tryAgainButton.setOnClickListener(ModelClass.this);

                    cancelButton = (Button) findViewById(R.id.cancel_button);
                    cancelButton.setVisibility(VISIBLE);
                    cancelButton.setOnClickListener(ModelClass.this);

                    count = 0;

                    if (!mView.openCamera()){
                        AlertDialog alertDialog = new AlertDialog.Builder(mAppContext).create();
                        alertDialog.setCancelable(false);
                        alertDialog.setMessage("Fatal error: can't open camera!");
                        alertDialog.setButton("OK", new DialogInterface.OnClickListener(){

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        });
                        alertDialog.show();
                    }
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        if (null != mView && !mView.openCamera()){
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setCancelable(false);
            alertDialog.setMessage("Fatal error: can't open camera!");
            alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id){
                    dialog.dismiss();
                    finish();
                }
            });
            alertDialog.show();
        }
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            System.out.println("In ShutterCallback");
        }
    };

    Camera.PictureCallback rawCall = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if(data != null){
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                imageView.setVisibility(VISIBLE);
                imageView.setImageBitmap(bitmap);
                previewLayout.setVisibility(GONE);

                if (progressDialog != null){
                    progressDialog.dismiss();
                }
                okButton.setVisibility(VISIBLE);
                clickButton.setVisibility(GONE);
                tryAgainButton.setVisibility(VISIBLE);
            }
        }
    };

    Camera.PictureCallback jpgCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            if (data != null){
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                imageView.setVisibility(VISIBLE);
                imageView.setImageBitmap(bitmap);
                previewLayout.setVisibility(GONE);

                if (progressDialog != null)
                    progressDialog.dismiss();
                okButton.setVisibility(VISIBLE);
                clickButton.setVisibility(GONE);
                tryAgainButton.setVisibility(VISIBLE);
            }
        }
    };
    @Override
    public void onClick(View v) {

    }

    public void saveImage(){
        FileOutputStream outputStream;

        try{
            File file = new File(FaceActivity.working_Dir, FaceActivity.current_name + ".jpg");

            if (!file.exists())
                file.createNewFile();
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
