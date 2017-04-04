package com.szsitc.jay.libsitcupdate.update;

import android.content.Context;
import android.util.Log;

public class UpdateChecker {


    public static void checkForDialog(Context context ,boolean isapk ,int fwver) {
        if (context != null) {
            new CheckUpdateTask(context, Constants.TYPE_DIALOG, true ,isapk,fwver).execute();
        } else {
            Log.e(Constants.TAG, "The arg context is null");
        }
    }


    public static void checkForNotification(Context context,boolean isapk ,int fwver) {
        if (context != null) {
            new CheckUpdateTask(context, Constants.TYPE_NOTIFICATION, false,isapk,fwver).execute();
        } else {
            Log.e(Constants.TAG, "The arg context is null");
        }

    }

    public static void checkForDialogBackground(Context context ,boolean isapk ,int fwver) {
        if (context != null) {
            new CheckUpdateTask(context, Constants.TYPE_DIALOG, false ,isapk,fwver).execute();
        } else {
            Log.e(Constants.TAG, "The arg context is null");
        }
    }
}
