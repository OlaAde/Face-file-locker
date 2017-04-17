package com.example.android.faceapplocker;

import java.io.File;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private String path="";
    private String selectedFile="";
    private Context context;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.context=this;


    }

    protected void onStart(){
        super.onStart();
        ListView lv=(ListView) findViewById(R.id.files_list);
        if(lv!=null){
            lv.setSelector(R.drawable.selection_style);
            lv.setOnItemClickListener(new ClickListener());
        }
        path="/mnt";
        listDirContents();
    }

    public void onBackPressed(){
        goBack();
    }

    public void goBack(){
        if(path.length()>1){ //up one level of directory structure
            File f=new File(path);
            path=f.getParent();
            listDirContents();
        }
        else{
            refreshThumbnails();
            System.exit(0); //exit app

        }
    }


    private void refreshThumbnails(){
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    private class ClickListener implements OnItemClickListener{
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //selected item
            ViewGroup vg=(ViewGroup)view;
            String selectedItem = ((TextView) vg.findViewById(R.id.label)).getText().toString();
            path = path + "/" + selectedItem;
            Toast.makeText(MainActivity.this,path,Toast.LENGTH_SHORT).show();
            //et.setText(path);
            listDirContents();
        }


    }



    private void listDirContents(){
        ListView listView=(ListView) findViewById(R.id.files_list);
        if(path!=null){
            Toast.makeText(MainActivity.this,path + "mmmd",Toast.LENGTH_SHORT).show();

            try{
                File f = new File(path);
                if(f!=null){
                    if(f.isDirectory()){
                        String[] contents=f.list();
                        if(contents.length>0){
                            selectedFile = path;
                            //create the data source for the list
                            ListAdapter listAdapter = new ListAdapter(this,R.layout.list_layout,R.id.label,contents,path);
                            //supply the data source to the list so that they are ready to display
                            listView.setAdapter(listAdapter);
                        }
                        else
                        {
                            //keep track the parent directory of empty directory
                            path=f.getParent();
                        }
                    }
                    else{
                        //capture the selected file path
                        selectedFile=path;
                        //keep track the parent directory of the selected file
                        path=f.getParent();

                    }
                }
            }catch(Exception e){}
        }


    }

    public void lockFolder(View view){
        EditText txtpwd=(EditText)findViewById(R.id.txt_input);
        String pwd=txtpwd.getText().toString();
        File f=new File(selectedFile);
        if(pwd.length()>0){

            if(f.isDirectory()){
                BackTaskLock btlock=new BackTaskLock();
                btlock.execute(pwd,null,null);

            }
            else{
                MessageAlert.showAlert("It is not a folder.",context);
            }
        }
        else{
            MessageAlert.showAlert("Please enter password",context);
        }
    }

    public void startLock(String pwd){
        Locker locker=new Locker(context,selectedFile,pwd);
        locker.lock();
    }

    public void unlockFolder(View view){
        EditText txtpwd=(EditText)findViewById(R.id.txt_input);
        String pwd=txtpwd.getText().toString();
        File f=new File(selectedFile);
        if(pwd.length()>0){

            if(f.isFile()){

                if(isMatched(pwd)){
                    BackTaskUnlock btunlock=new BackTaskUnlock();
                    btunlock.execute(pwd,null,null);
                }
                else{
                    MessageAlert.showAlert("Invalid password or folder not locked",context);
                }

            }

            else{
                MessageAlert.showAlert("Please select a locked folder to unlock",context);
            }
        }
        else{
            MessageAlert.showAlert("Please enter password",context);
        }

    }

    public boolean isMatched(String pwd){
        boolean mat=false;
        Locker locker=new Locker(context, selectedFile, pwd);
        byte[] pas=locker.getPwd();
        int pwdRead=locker.bytearrayToInt(pas);
        int pwdInput=locker.bytearrayToInt(pwd.getBytes());
        if(pwdRead==pwdInput) mat=true;
        return mat;
    }

    private class BackTaskLock extends AsyncTask<String,Void,Void>{
        ProgressDialog pd;
        protected void onPreExecute(){
            super.onPreExecute();
            //show process dialog
            pd = new ProgressDialog(context);
            pd.setTitle("Locking the folder");
            pd.setMessage("Please wait.");
            pd.setCancelable(true);
            pd.setIndeterminate(true);
            pd.show();


        }
        protected Void doInBackground(String...params){
            try{

                startLock(params[0]);

            }catch(Exception e){
                pd.dismiss();   //close the dialog if error occurs
            }
            return null;

        }
        protected void onPostExecute(Void result){
            pd.dismiss();
            goBack();
        }


    }


    public void startUnlock(String pwd){
        Locker locker=new Locker(context,selectedFile,pwd);
        locker.unlock();
    }


    private class BackTaskUnlock extends AsyncTask<String,Void,Void>{
        ProgressDialog pd;
        protected void onPreExecute(){
            super.onPreExecute();
            //show process dialog
            pd = new ProgressDialog(context);
            pd.setTitle("Unlocking the folder");
            pd.setMessage("Please wait.");
            pd.setCancelable(true);
            pd.setIndeterminate(true);
            pd.show();


        }
        protected Void doInBackground(String...params){
            try{

                startUnlock(params[0]);

            }catch(Exception e){
                pd.dismiss();   //close the dialog if error occurs

            }
            return null;

        }
        protected void onPostExecute(Void result){
            pd.dismiss();
            listDirContents();//refresh the list
        }


    }


}