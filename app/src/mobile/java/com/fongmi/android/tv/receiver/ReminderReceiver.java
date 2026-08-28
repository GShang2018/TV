package com.fongmi.android.tv.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Reminder;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.utils.Notify;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String channel = intent.getStringExtra("channelName");
        long startTime = intent.getLongExtra("startTime", 0);
        Reminder reminder = Reminder.find(channel, startTime);
        if (reminder == null) return;
        // 预约已触发，删除记录避免重复提醒
        AppDatabase.get().getReminderDao().delete(channel, startTime);
        showNotification(context, reminder);
    }

    // 通知点击跳转到直播播放页并切换到对应频道
    private void showNotification(Context context, Reminder reminder) {
        Intent play = new Intent(context, LiveActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("group", reminder.getGroupName())
                .putExtra("channel", reminder.getChannelName())
                .putExtra("empty", false);
        PendingIntent contentIntent = PendingIntent.getActivity(context, requestCode(reminder), play, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Notify.DEFAULT)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(reminder.getProgramTitle())
                .setContentText(context.getString(R.string.live_reminder, reminder.getChannelName()))
                .setContentIntent(contentIntent)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify(requestCode(reminder), builder.build());
    }

    private static int requestCode(Reminder reminder) {
        return (int) (reminder.getStartTime() & 0x7FFFFFFF);
    }
}
