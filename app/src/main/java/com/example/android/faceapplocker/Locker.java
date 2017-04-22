package com.example.android.faceapplocker;

/**
 * Created by Adeogo on 4/17/2017.
 */

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

class Locker{
    String path;
    String pwd;
    Context context;
    final String separator="--*****--";

    Locker(Context context,String path,String pwd){
        this.path=path;
        this.pwd=pwd;
        this.context =context;
    }


    //The isTextFile method will be called to check whether the selected file is a text file.
    // To improve the performance of locking and unlocking processes, text file and other types
    // files will be encrypted differently .
    public boolean isTextFile(String file){

        boolean isText=false;
        String extension = MimeTypeMap.getFileExtensionFromUrl(file);
        String mimeType= MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if(mimeType.startsWith("text/"))
            isText=true;
        return isText;
    }

    //If the selected file is not a text file, the locking or unlocking process is performed
    // only at the header section of the file. So large audio, video, or image files can be
    // locked or unlocked very fast. The size of header block is 1024 bytes.

    public void lock(){
        boolean isHead=true;
        boolean isBody=false;
        int blockSize=0;

        try{
            File f=new File(path);
            //get previous pwd
            if(f.exists()){
                byte[] ppwd=getPwd();
                if(ppwd!=null){
                    MessageAlert.showAlert("Alreadly locked",context);
                    return;
                }
                FileInputStream fis=new FileInputStream(f);
                File tempfile=new File(context.getFilesDir(),"temp.temp");
                FileOutputStream fos=new FileOutputStream(tempfile);
                FileChannel fc=fis.getChannel();
                int pwdInt=bytearrayToInt(pwd.getBytes());
                int nRead;
                boolean isText=isTextFile(path);
                if(isText){ //encrypting two parts of the text file
                    blockSize=(int)f.length()/4; //25 percent of the file content
                    ByteBuffer bb=ByteBuffer.allocate(blockSize);

                    while ( (nRead=fc.read( bb )) != -1 )
                    {
                        bb.position(0);
                        bb.limit(nRead);

                        //encrypt the head section of the file
                        if(isHead){
                            while ( bb.hasRemaining())
                                fos.write(bb.get()+pwdInt);
                            isHead=false;
                            isBody=true;
                        }
                        else if(isBody){
                            //do not decrypt the body section of the file
                            fos.write(bb.array());
                            isBody=false;
                        }
                        else{//encrypt the footer section of the file
                            while ( bb.hasRemaining())
                                fos.write(bb.get()+pwdInt);
                        }

                        bb.clear();

                    }
                }

                else{
                    blockSize=1024; //encrypt the first 1kb of the file for non-text file
                    ByteBuffer bb=ByteBuffer.allocate(blockSize);

                    while ( (nRead=fc.read( bb )) != -1 )
                    {
                        bb.position(0);
                        bb.limit(nRead);
                        //encrypt only the head section of the file
                        if(isHead){
                            while ( bb.hasRemaining())
                                fos.write(bb.get()+pwdInt);
                            isHead=false;

                        }
                        else{
                            fos.write(bb.array());
                        }
                        bb.clear();

                    }
                }

                fis.close();
                fos.flush();
                fos.close();
                //replacing the file content
                f.delete();
                File lockedFile=new File(path);
                copyFile(tempfile,lockedFile);
                //delete the temp file
                tempfile.delete();
                //save the password
                saveInfo(pwd,blockSize);
                //make the file read only
                lockedFile.setReadOnly();

            }

        }catch(IOException e){e.printStackTrace();}
    }

   // The unlock method is called to unlock the locked file by decrypting parts of the file
    // that were encrypted in the locking process. In the unlocking process every byte of each of
    // the file content is subtracted by the sum of bytes previously added to every byte.

    public void unlock(){
        boolean isHead=true;
        boolean isBody=false;
        int pwdread=bytearrayToInt(getPwd());
        int pwdbyte=bytearrayToInt(pwd.getBytes());
        if(pwdbyte==pwdread){

            try{
                File f=new File(path);
                if(f.exists()){

                    FileInputStream fis=new FileInputStream(f);
                    File tempfile=new File(context.getFilesDir(),"temp.temp");
                    FileOutputStream fos=new FileOutputStream(tempfile);
                    FileChannel fc=fis.getChannel();
                    int pwdInt=bytearrayToInt(pwd.getBytes());
                    int blockSize=getBlockSize();
                    ByteBuffer bb=ByteBuffer.allocate(blockSize);
                    int nRead;
                    boolean isText=isTextFile(path);
                    if(isText){ //decoding two parts of the text file
                        while ( (nRead=fc.read( bb )) != -1 )
                        {
                            bb.position(0);
                            bb.limit(nRead);

                            //decrypt the head section of the file
                            if(isHead){
                                while ( bb.hasRemaining())
                                    fos.write(bb.get()-pwdInt);
                                isHead=false;
                                isBody=true;
                            }
                            else if(isBody){
                                //do not decrypt the body section of the file
                                fos.write(bb.array());
                                isBody=false;
                            }
                            else{//decrypt the footer section of the file
                                while ( bb.hasRemaining())
                                    fos.write(bb.get()-pwdInt);
                            }

                            bb.clear();

                        }
                    }

                    else{

                        while ( (nRead=fc.read( bb )) != -1 )
                        {
                            bb.position(0);
                            bb.limit(nRead);
                            //encrypting only the head section of the file
                            if(isHead){
                                while ( bb.hasRemaining())
                                    fos.write(bb.get()-pwdInt);
                                isHead=false;

                            }
                            else{
                                fos.write(bb.array());
                            }
                            bb.clear();

                        }
                    }

                    fis.close();
                    fos.flush();
                    fos.close();
                    //Replacing the file content
                    f.delete();
                    File unlockedFile=new File(path);
                    unlockedFile.setWritable(true);
                    copyFile(tempfile,unlockedFile);
                    //delete the temp file
                    tempfile.delete();
                    File filepwd=new File(context.getFilesDir(),getName(path));
                    //delete the password
                    filepwd.delete();



                }

            }catch(IOException e){e.printStackTrace();}
        }
        else{
            MessageAlert.showAlert("Invalid password or file is not locked.",context);
        }
    }

    // Thi method is invoked by the lock and unlock method to copy the content
    // of the temporary file to the destination file in file content replacement process.
    private void copyFile(File src, File dst) throws IOException
    {
        FileInputStream fis=new FileInputStream(src);
        FileOutputStream fos=new FileOutputStream(dst);
        FileChannel inChannel =fis.getChannel();
        FileChannel outChannel = fos.getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }catch(IOException e){e.printStackTrace();}
        finally
        {
            if (inChannel != null){
                fis.close();
                inChannel.close();
            }
            if (outChannel != null){
                fos.close();
                outChannel.close();
            }

        }
    }
    // This sums all bytes in the input array of bytes.
    // It is called to sum all bytes generated from the password text.
    public int bytearrayToInt(byte[] pwd){
        int b=0;
        if(pwd!=null)
            for(byte y:pwd){
                b=b+y;
            }
        return b;

    }

    // The getPwd method converts the password text in to an array of bytes.
    public byte[] getPwd(){
        byte[] b=null;
        try {
            File f=new File(context.getFilesDir(),getName(path));
            if(f.exists()){
                BufferedReader br=new BufferedReader(new FileReader(f));
                String info=br.readLine();
                b=info.substring(0,info.lastIndexOf(separator)).getBytes();
                br.close();
            }
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return b;

    }
    //The getBlockSize method is invoked by the unlocking process to read the size of the encrypted
    // block from the file that is saved in the locking process. So the unlocking process knows the
    // block of bytes to be read and decrypted. This file stores the password text and the
    // encrypted block size.

    private int getBlockSize(){
        int size=0;
        try {
            File f=new File(context.getFilesDir(),getName(path));
            if(f.exists()){
                BufferedReader br=new BufferedReader(new FileReader(f));
                String info=br.readLine();
                size=Integer.valueOf(info.substring(info.lastIndexOf(separator)+separator.length()));
                br.close();
            }
        } catch(Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return size;

    }

    //The saveInfo method is invoked by the locking process to save the password text
    // and the block size to a file. The password text and the block size is stored
    // in the file in the form as shown below.
    //password--*****--size
    private void saveInfo(String pwd,int blockSize){
        try {
            String fileName=getName(path);
            File f=new File(context.getFilesDir(),fileName);
            BufferedWriter bw=new BufferedWriter(new FileWriter(f));
            String info=pwd+separator+blockSize;
            bw.write(info);
            bw.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // This method simply returns the file name of the selected file path.
    private String getName(String path){
        return(path.substring(path.lastIndexOf("/")+1));
    }




}
