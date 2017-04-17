package com.example.android.faceapplocker;

/**
 * Created by Adeogo on 4/17/2017.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class MessageAlert {
    //This method will be invoked to display alert dialog
    public static void showAlert(String message,Context context){

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", new OnClickListener(){
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });
        AlertDialog dialog = builder.create();
        dialog.show();

    }
}