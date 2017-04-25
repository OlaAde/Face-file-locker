package com.example.android.faceapplocker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class FaceActivity extends AppCompatActivity implements View.OnClickListener{

    Button detect_face, add_name, snapshot;
    TextView nameTextView;
    EditText editText;
    private String Image_TAG = new String ("image_db");
    private String Name_TAG = new String ("name_db");
    public static Map<Integer, String> idToImage;
    public static Map<Integer, String> idToName;
    public static String current_name = new String ("temp");
    public static final String TAG = FaceActivity.class.getSimpleName();
    ImageView captured_image;
    public static boolean face_detected, pictureTaken, recognized;
    public static File working_Dir = new File(Environment.getExternalStorageDirectory()
    .getAbsolutePath() +"/Face Locker");
    //public CascadeClassifier haar_cascade;
    public static int ID;
    private String Name_obj;
    BufferedWriter bufferedWriter;
    static File file;
    static {
        working_Dir.mkdirs();
        file = new File(FaceActivity.working_Dir, "csv.txt");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Name_obj = getIntent().getStringExtra("Name");
        if (Name_obj != null){
            Log.i(TAG, "Detected Name: " + Name_obj);
        }

        setContentView(R.layout.activity_face);

        detect_face = (Button) findViewById(R.id.detect_face);
        detect_face.setOnClickListener(this);
        snapshot = (Button) findViewById(R.id.Snap);
        snapshot.setOnClickListener(this);
        add_name = (Button) findViewById(R.id.add_a_name);
        add_name.setOnClickListener(this);
        editText = (EditText) findViewById(R.id.name);
        editText.setVisibility(View.GONE);
        nameTextView = (TextView) findViewById(R.id.detected_face_name);
        nameTextView.setVisibility(View.GONE);
        captured_image = (ImageView) findViewById(R.id.snapshot);

        //Load from file
        File imageFile = new File(working_Dir,Image_TAG);
        File nameFile = new File(working_Dir, Name_TAG);
        if (imageFile.exists()){
            try{
                FileInputStream f = new FileInputStream(imageFile);
                ObjectInputStream s = new ObjectInputStream(f);
                idToImage = (Map<Integer, String>) s.readObject();
                FaceActivity.ID = idToImage.size();

                s.close();
                Log.i(TAG, "Database Exists");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        if (nameFile.exists()){
            try{
                FileInputStream f = new FileInputStream(nameFile);
                ObjectInputStream s = new ObjectInputStream(f);
                idToName = (Map<Integer, String>) s.readObject();

                s.close();
                Log.i(TAG, "Database Exists");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        if(!pictureTaken){
            captured_image.setVisibility(View.GONE);
            add_name.setVisibility(View.GONE);
            detect_face.setVisibility(View.GONE);
            snapshot.setVisibility(View.VISIBLE);
        }else if (face_detected){
            if(!recognized){
                detect_face.setVisibility(View.GONE);
                add_name.setVisibility(View.VISIBLE);
                snapshot.setVisibility(View.GONE);
            }else{
                nameTextView.setVisibility(View.VISIBLE);
                nameTextView.setText(Name_obj != null?Name_obj:"");
            }
        }else{
            detect_face.setVisibility(View.VISIBLE);
            captured_image.setVisibility(View.VISIBLE);

            snapshot.setVisibility(View.GONE);
            Bitmap bitmap = null;
            File f = new File(working_Dir, current_name + "_det.jpg");
            if (f.exists()){
                FaceActivity.face_detected = true;
                bitmap = BitmapFactory.decodeFile(working_Dir.getAbsolutePath() + "/"
                        + current_name
                        + "_det.jpg");
            }else {
                FaceActivity.face_detected = false;
                bitmap = BitmapFactory.decodeFile(working_Dir.getAbsolutePath() + "/"
                + current_name + ".jpg");
            }

            captured_image.setImageBitmap(bitmap);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()){
            case R.id.detect_face:
                new ModelView(getApplicationContext()).FindFaces(working_Dir.getAbsolutePath() +
                "/" + current_name, ModelView.mCascadeFile.getAbsolutePath());
                Log.d(TAG, "Image dir: " + working_Dir.getAbsolutePath() + "/"
                + current_name);
                File f = new File(FaceActivity.working_Dir, "csv.txt");
                int return_id = -1;

                if (f.exists() && FaceActivity.ID > 1){
                   return_id = new ModelView(getApplicationContext()).Find(working_Dir.getAbsolutePath()
                           + "/"  + current_name, ModelView.mCascadeFile.getAbsolutePath(),
                           working_Dir.getAbsolutePath() + "/csv.txt");
                }
                FaceActivity.face_detected = true;
                String Name = null;
                if (return_id != -1){
                    Name = idToName.get(return_id);
                    FaceActivity.recognized = true;
                }
                startActivity(getIntent().putExtra("Name", Name));
                break;
            case R.id.add_a_name:
                editText.setVisibility(View.VISIBLE);
                if (editText.getText().toString().equals("")){
                    return;
                }
                String new_name = editText.getText().toString();
                if (idToImage == null){
                    idToImage = new HashMap<Integer, String>();
                    FaceActivity.ID = 0;
                }
                if (idToName == null){
                    idToName = new HashMap<Integer, String>();
                }

                idToName.put(FaceActivity.ID, FaceActivity.working_Dir + "/" +
                        new_name + ".jpg" );
                idToImage.put(FaceActivity.ID, FaceActivity.working_Dir + "/" +
                        new_name + "1.jpg");
                idToName.put(FaceActivity.ID, new_name);

                File j = new File(FaceActivity.working_Dir, FaceActivity.current_name +
                "_det.jpg");
                if (!j.exists()){
                    try {
                        j.createNewFile();
                    }catch (IOException e){
                        e.printStackTrace();
                        return;
                    }
                }

                File j_new = new File(FaceActivity.working_Dir, new_name + ".jpg");
                if (!j_new.exists()){
                    try {
                        j_new.createNewFile();
                    }catch (IOException e){
                        e.printStackTrace();
                        return;
                    }
                }

                Log.i(TAG, FaceActivity.working_Dir+"");
                j.renameTo(j_new);
                j = new File(FaceActivity.working_Dir, new_name + "1.jpg");

                try{
                    InputStream inputStream = new FileInputStream(j_new);
                    OutputStream outputStream = new FileOutputStream(j);

                    byte[] buffer = new byte[1024];

                    int length;

                    while ((length = inputStream.read(buffer)) > 0){
                        outputStream.write(buffer, 0, length);
                    }
                    inputStream.close();
                    outputStream.close();
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }

                try{
                    File file = new File(FaceActivity.working_Dir, Image_TAG);
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    ObjectOutputStream s = new ObjectOutputStream(fileOutputStream);
                    s.writeObject(idToImage);
                    s.close();
                }catch(Exception e){
                    e.printStackTrace();
                    return;
                }

                try{
                    File file = new File(FaceActivity.working_Dir, Name_TAG);
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    ObjectOutputStream s = new ObjectOutputStream(fileOutputStream);
                    s.writeObject(idToName);
                    s.close();
                }catch(Exception e){
                    e.printStackTrace();
                    return;
                }

                try{

                    try{

                        bufferedWriter = new BufferedWriter(new FileWriter(file, true));
                    }catch (IOException e){
                        e.printStackTrace();
                        return;
                    }

                    bufferedWriter.append(FaceActivity.working_Dir + new_name + ".jpg;" + FaceActivity.ID);
                    bufferedWriter.newLine();
                    bufferedWriter.append(FaceActivity.working_Dir + "1.jpg;" + FaceActivity.ID);
                    bufferedWriter.newLine();
                    bufferedWriter.close();
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }

                Toast.makeText(getApplicationContext(), "Name added", Toast.LENGTH_SHORT).show();
                FaceActivity.ID++;
                startActivity(getIntent());
                FaceActivity.face_detected = false;
                FaceActivity.pictureTaken = false;
                FaceActivity.recognized = false;
                Log.i(TAG, "Current Name: "+ current_name);
                break;
            case R.id.snapshot:
                intent = new Intent(FaceActivity.this, ModelClass.class);
                startActivity(intent);
                break;
        }
    }
}
