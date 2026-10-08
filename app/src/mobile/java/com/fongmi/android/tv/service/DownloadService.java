package com.fongmi.android.tv.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Download;
import com.fongmi.android.tv.event.DownloadEvent;
import com.fongmi.android.tv.receiver.DownloadReceiver;
import com.fongmi.android.tv.ui.activity.DownloadActivity;
import com.fongmi.android.tv.utils.DownloadManager;
import com.fongmi.android.tv.utils.Notify;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class DownloadService extends Service {

    private static final int NOTIFICATION_ID = 9528;

    public static void start(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, DownloadService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, DownloadService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_NOT_STICKY;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadEvent(DownloadEvent event) {
        if (!DownloadManager.get().hasActive()) {
            stopSelf();
            NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
        } else {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private List<Download> getActive() {
        List<Download> result = new ArrayList<>();
        for (Download item : Download.get()) {
            if (item.getStatus() == Download.STATUS_QUEUED || item.getStatus() == Download.STATUS_DOWNLOADING || item.getStatus() == Download.STATUS_PAUSED) result.add(item);
        }
        return result;
    }

    private Notification buildNotification() {
        List<Download> active = getActive();
        long total = 0;
        long progress = 0;
        boolean paused = !active.isEmpty();
        for (Download item : active) {
            if (item.getStatus() != Download.STATUS_PAUSED) paused = false;
            if (item.getTotal() > 0) total += item.getTotal();
            progress += item.getProgress();
        }
        int percent = total > 0 ? (int) Math.min(100, progress * 100 / total) : 0;
        String title = active.isEmpty() ? getString(R.string.download_title) : active.get(0).getFileName();
        String text = getString(R.string.download_notify_text, active.size(), percent);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Notify.DEFAULT)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(DownloadActivity.getPendingIntent(this));
        if (total > 0) builder.setProgress(100, percent, false);
        else builder.setProgress(0, 0, true);
        if (paused) builder.addAction(R.drawable.ic_notify_play, getString(R.string.download_resume_all), DownloadReceiver.getPendingIntent(this, DownloadReceiver.RESUME_ALL));
        else builder.addAction(R.drawable.ic_notify_pause, getString(R.string.download_pause_all), DownloadReceiver.getPendingIntent(this, DownloadReceiver.PAUSE_ALL));
        return builder.build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (!DownloadManager.get().hasActive()) stopSelf();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
