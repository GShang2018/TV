package com.fongmi.android.tv.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.fongmi.android.tv.bean.Reminder;
import com.fongmi.android.tv.receiver.ReminderReceiver;

public class ReminderManager {

    public static final String ACTION = "com.fongmi.android.tv.reminder";

    // 到点触发系统闹钟，通知用户节目开播
    public static void schedule(Context context, Reminder reminder) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = getPendingIntent(context, reminder);
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.getStartTime(), pi);
        } catch (SecurityException e) {
            // 无精确闹钟权限时降级为非精确窗口
            am.setWindow(AlarmManager.RTC_WAKEUP, reminder.getStartTime(), 60_000, pi);
        }
    }

    public static void cancel(Context context, Reminder reminder) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getPendingIntent(context, reminder));
    }

    private static PendingIntent getPendingIntent(Context context, Reminder reminder) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setAction(ACTION)
                .putExtra("channelName", reminder.getChannelName())
                .putExtra("startTime", reminder.getStartTime());
        return PendingIntent.getBroadcast(context, requestCode(reminder), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(Reminder reminder) {
        return (int) (reminder.getStartTime() & 0x7FFFFFFF);
    }
}
