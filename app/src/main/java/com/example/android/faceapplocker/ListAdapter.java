package com.example.android.faceapplocker;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

/**
 * Created by Adeogo on 4/17/2017.
 */

public class ListAdapter extends ArrayAdapter<String> {
    int groupid;
    String[] names;
    Context context;
    String path;
    public ListAdapter(Context context, int vg, int id, String[] names, String parentPath) {
        super(context,vg, id, names);
        this.context=context;
        groupid=vg;
        this.names=names;
        this.path=parentPath;
    }


    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View itemView = inflater.inflate(groupid, parent, false);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.icon);
        TextView textView = (TextView) itemView.findViewById(R.id.label);
        String item = names[position];
        textView.setText(item);
        File lockedfile = new File(context.getFilesDir(),item);
        if(lockedfile.exists()){
            //set the locked icon to the folder that was already locked.
            imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.folder_lock));
        }
        else{//set the directory and file icon to the unlocked files and folders
            File f = new File(path+"/"+item);
            if(f.isDirectory())
                imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.diricon));
            else
                imageView.setImageDrawable(context.getResources().getDrawable(R.drawable.fileicon));
        }
        return itemView;
    }
}
