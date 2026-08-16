package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.databinding.ActivityCustomSiteBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.CustomSiteListAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.CustomSiteDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class CustomSiteActivity extends BaseActivity implements CustomSiteListAdapter.OnClickListener {

    private ActivityCustomSiteBinding mBinding;
    private CustomSiteListAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, CustomSiteActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCustomSiteBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mAdapter = new CustomSiteListAdapter(this);
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        mBinding.recycler.setAdapter(mAdapter);
        refreshList();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPress());
        mBinding.add.setOnClickListener(v -> CustomSiteDialog.create(this).setOnSaved(this::refreshList).show());
    }

    @Override
    protected boolean handleBack() {
        return true;
    }

    @Override
    protected void onBackPress() {
        finish();
    }

    private void refreshList() {
        List<CustomSite> items = CustomSite.getAll();
        mBinding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        mAdapter.addAll(items);
    }

    @Override
    public void onToggle(CustomSite item, boolean enabled) {
        item.setEnabled(enabled);
        item.save();
        refreshConfig();
    }

    @Override
    public void onEdit(CustomSite item) {
        CustomSiteDialog.create(this, item).setOnSaved(this::refreshList).show();
    }

    @Override
    public void onCopy(CustomSite item) {
        Util.copy(item.getApi());
    }

    @Override
    public void onDelete(CustomSite item) {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_custom_site_title).setMessage(R.string.dialog_delete_custom_site).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
            item.delete();
            Notify.show(R.string.custom_site_deleted);
            refreshConfig();
            refreshList();
        }).show();
    }

    private void refreshConfig() {
        Config config = VodConfig.get().getConfig();
        if (config != null && config.isCustom()) {
            VodConfig.load(config, new Callback() {
                @Override
                public void success(String result) {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }

                @Override
                public void success() {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }

                @Override
                public void error(String msg) {
                    RefreshEvent.video();
                    RefreshEvent.config();
                }
            });
        } else {
            RefreshEvent.video();
            RefreshEvent.config();
        }
    }
}
