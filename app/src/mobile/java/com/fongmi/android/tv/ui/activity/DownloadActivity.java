package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Download;
import com.fongmi.android.tv.databinding.ActivityDownloadBinding;
import com.fongmi.android.tv.event.DownloadEvent;
import com.fongmi.android.tv.ui.adapter.DownloadAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.DownloadManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadActivity extends BaseActivity implements DownloadAdapter.OnClickListener {

    private ActivityDownloadBinding mBinding;
    private DownloadAdapter mAdapter;
    private int mTab;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, DownloadActivity.class));
    }

    public static PendingIntent getPendingIntent(Context context) {
        return PendingIntent.getActivity(context, 0, new Intent(context, DownloadActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityDownloadBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        setTabs();
        getData();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPressed());
    }

    private void setRecyclerView() {
        mBinding.recycler.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recycler.setAdapter(mAdapter = new DownloadAdapter(this));
    }

    private void setTabs() {
        mBinding.tabs.addTab(mBinding.tabs.newTab().setText(R.string.download_tab_downloading));
        mBinding.tabs.addTab(mBinding.tabs.newTab().setText(R.string.download_tab_done));
        mBinding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mTab = tab.getPosition();
                getData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void getData() {
        List<Download> list = new ArrayList<>();
        if (mTab == 1) {
            list.addAll(Download.getByStatus(Download.STATUS_FINISHED));
        } else {
            list.addAll(Download.getByStatus(Download.STATUS_QUEUED));
            list.addAll(Download.getByStatus(Download.STATUS_DOWNLOADING));
            list.addAll(Download.getByStatus(Download.STATUS_PAUSED));
            list.addAll(Download.getByStatus(Download.STATUS_FAILED));
            Collections.sort(list, (a, b) -> Long.compare(b.getCreateTime(), a.getCreateTime()));
        }
        mAdapter.addAll(list);
    }

    @Override
    public void onItemClick(Download item) {
        if (item.isFinished()) play(item);
    }

    @Override
    public void onAction(Download item) {
        switch (item.getStatus()) {
            case Download.STATUS_QUEUED:
            case Download.STATUS_DOWNLOADING:
                DownloadManager.get().pause(item.getId());
                break;
            case Download.STATUS_PAUSED:
                DownloadManager.get().resume(item.getId());
                break;
            case Download.STATUS_FAILED:
                DownloadManager.get().retry(item.getId());
                break;
            default:
                play(item);
                break;
        }
    }

    @Override
    public void onDelete(Download item) {
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.download_delete_confirm, item.getFileName()))
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.select_delete, (dialog, which) -> DownloadManager.get().delete(item.getId()))
                .show();
    }

    private void play(Download item) {
        VideoActivity.start(this, "file://" + item.getSavePath());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadEvent(DownloadEvent event) {
        getData();
    }
}
