package com.fongmi.android.tv.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fongmi.android.tv.bean.Reminder;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.utils.ReminderManager;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    // 设备重启 / 应用升级后 AlarmManager 闹钟全部丢失，需按库中记录重新调度未到期的预约
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? "" : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        List<Reminder> reminders = AppDatabase.get().getReminderDao().getAll();
        long now = System.currentTimeMillis();
        for (Reminder reminder : reminders) {
            if (reminder.getStartTime() > now) ReminderManager.schedule(context, reminder);
        }
    }
}
