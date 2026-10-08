package com.fongmi.android.tv.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fongmi.android.tv.utils.DownloadManager;

public class DownloadReceiver extends BroadcastReceiver {

    public static final String PAUSE_ALL = "com.fongmi.android.tv.DOWNLOAD_PAUSE_ALL";
    public static final String RESUME_ALL = "com.fongmi.android.tv.DOWNLOAD_RESUME_ALL";

    public static PendingIntent getPendingIntent(Context context, String action) {
        return PendingIntent.getBroadcast(context, action.hashCode(), new Intent(action).setPackage(context.getPackageName()), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (PAUSE_ALL.equals(action)) DownloadManager.get().pauseAll();
        else if (RESUME_ALL.equals(action)) DownloadManager.get().resumeAll();
    }
}
