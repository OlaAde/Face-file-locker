package com.example.android.faceapplocker;

/**
 * Created by Adeogo on 4/17/2017.
 */

import android.content.Context;
import android.os.Environment;
import android.util.Log;

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

class Locker {
    String dirpath;
    String pwd;
    Context context;

    Locker(Context context, String path, String pwd) {
        this.dirpath = path;
        this.pwd = pwd;
        this.context = context;


    }


    public void lock() {
        boolean isHead = true;
        doCompression(dirpath);
        try {
            File f = new File(Environment.getExternalStorageDirectory().toString() + "/lockedfile.zip");
            if (f.exists()) {

                FileInputStream fis = new FileInputStream(f);
                File tempfile = new File(context.getFilesDir(), "temp.temp");
                FileOutputStream fos = new FileOutputStream(tempfile);
                FileChannel fc = fis.getChannel();
                int pwdInt = bytearrayToInt(pwd.getBytes());
                int nRead;
                int blockSize = 1024; //encrypt the first 1kb of the package
                ByteBuffer bb = ByteBuffer.allocate(blockSize);

                while ((nRead = fc.read(bb)) != -1) {
                    bb.position(0);
                    bb.limit(nRead);
                    //encrypt only the head section of the file
                    if (isHead) {
                        while (bb.hasRemaining())
                            fos.write(bb.get() + pwdInt);
                        isHead = false;

                    } else {
                        fos.write(bb.array());
                    }
                    bb.clear();

                }


                fis.close();
                fos.flush();
                fos.close();
                //replacing the file content
                f.delete();
                File file = new File(dirpath);
                FileUtils.deleteQuietly(file);
                File lockedFile = new File(getParentDir(dirpath) + File.separator + getName(dirpath) + ".locked");
                copyFile(tempfile, lockedFile);
                //delete the temp file
                tempfile.delete();
                //save the password
                savePwd(pwd);


            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void unlock() {
        boolean isHead = true;

        try {
            File f = new File(dirpath);
            if (f.isFile()) {
                FileInputStream fis = new FileInputStream(f);
                File tempfile = new File(context.getFilesDir(), "temp.zip");
                FileOutputStream fos = new FileOutputStream(tempfile);
                FileChannel fc = fis.getChannel();
                int blockSize = 1024;
                ByteBuffer bb = ByteBuffer.allocate(blockSize);
                int pwdInput = bytearrayToInt(pwd.getBytes());
                int nRead;
                while ((nRead = fc.read(bb)) != -1) {
                    bb.position(0);
                    bb.limit(nRead);
                    //decrypt the head section of the file
                    if (isHead) {
                        while (bb.hasRemaining())
                            fos.write(bb.get() - pwdInput);
                        isHead = false;

                    } else

                        fos.write(bb.array());

                    bb.clear();

                }


                fis.close();
                fos.flush();
                fos.close();

                //Replacing the file content
                String dirParent = f.getParent();
                f.delete();
                File unlockedFile = new File(dirParent + "/unloacked.zip");
                copyFile(tempfile, unlockedFile);
                extractFile(unlockedFile.getPath(), dirParent);
                unlockedFile.delete();
                //delete the temp file
                tempfile.delete();
                //delete the password
                File filepwd = new File(context.getFilesDir(), getName(dirpath));
                filepwd.delete();


            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void copyFile(File src, File dst) throws IOException {
        FileInputStream fis = new FileInputStream(src);
        FileOutputStream fos = new FileOutputStream(dst);
        FileChannel inChannel = fis.getChannel();
        FileChannel outChannel = fos.getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inChannel != null) {
                fis.close();
                inChannel.close();
            }
            if (outChannel != null) {
                fos.close();
                outChannel.close();
            }

        }
    }

    public int bytearrayToInt(byte[] pwd) {
        int b = 0;
        if (pwd != null)
            for (byte y : pwd) {
                b = b + y;
            }
        return b;

    }

    public byte[] getPwd() {
        byte[] password = null;
        try {
            File f = new File(context.getFilesDir(), getName(dirpath));
            if (f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                password = br.readLine().getBytes();
                br.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return password;

    }


    private void savePwd(String pwd) {
        try {

            File f = new File(context.getFilesDir(), getName(dirpath) + ".locked");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(pwd);
            bw.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private String getName(String dirpath) {
        return (dirpath.substring(dirpath.lastIndexOf("/") + 1));
    }

    private String getParentDir(String dirpath) {
        File file = new File(dirpath);
        return (file.getParent());
    }

    public File doCompression(String src) {
        File f = new File(src);
        File fout = null;
        ZipOutputStream zos = null;
        try {
            fout = new File(Environment.getExternalStorageDirectory().toString() + "/lockedfile.zip");
            zos = new ZipOutputStream(new FileOutputStream(fout));
            if (f.exists()) {
                String path = getPath(f.getPath());
                if (f.isDirectory()) {

                    File[] files = f.listFiles();
                    for (File sf : files) {
                        compressDir(sf.getPath(), path, zos);
                    }
                }


            } else {
                Log.e("Error", "Soure not found!");
            }
            zos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return fout;

    }

    public String getPath(String srcpath) {

        String path = "";
        if (srcpath.endsWith(File.separator)) {
            path = srcpath.substring(0, srcpath.length() - 1);
            path = path.substring(path.lastIndexOf(File.separator) + 1);
        } else
            path = srcpath.substring(srcpath.lastIndexOf(File.separator) + 1);
        return path;
    }

    public void compressDir(String srcpath, String path, ZipOutputStream zos) {
        File fsrcdir = new File(srcpath);
        String rpath = getPath(srcpath);

        if (fsrcdir.isDirectory()) {
            try {

                rpath = path + File.separator + rpath;
                zos.putNextEntry(new ZipEntry(rpath + File.separator));
                zos.closeEntry();
                File[] files = fsrcdir.listFiles();
                for (File f : files) {
                    if (f.isDirectory()) {
                        compressDir(f.getPath(), rpath, zos);
                    } else {
                        compressFile(f.getPath(), rpath, zos);
                    }
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            compressFile(srcpath, path, zos);
        }
    }

    public void compressFile(String srcfile, String path, ZipOutputStream zos) {
        //write a new entry to the zip file
        String rpath = getPath(srcfile);
        try {
            FileInputStream fis = new FileInputStream(srcfile);
            int content = 0;
            zos.putNextEntry(new ZipEntry(path + File.separator + rpath));
            while ((content = fis.read()) != -1) {
                zos.write(content);
            }
            zos.closeEntry();
            fis.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void extractFile(String srcfile, String despath) {
        try {
            ZipFile zf = new ZipFile(srcfile); //create  a zip file object
            if (zf.size() > 0) { //read through the zip file
                Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zf.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && !entry.getName().endsWith("/")) {
                        //start extracting the files
                        extract(zf.getInputStream(entry), entry.getName(), despath);

                    }

                }

            }
            zf.close();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void extract(InputStream is, String fname, String storeDir) {

        FileOutputStream fos;
        //if(fname.endsWith(".locked")) fname=fname.substring(0, fname.lastIndexOf(".locked"));
        File fi = new File(storeDir + File.separator + fname); //output file
        File fparent = new File(fi.getParent());
        fparent.mkdirs();//create parent directories for output files

        try {

            fos = new FileOutputStream(fi);
            int content = 0;
            while ((content = is.read()) != -1) {
                fos.write(content);
            }
            is.close();
            fos.close();
        } catch (Exception e) {

            e.printStackTrace();
        }

    }


}