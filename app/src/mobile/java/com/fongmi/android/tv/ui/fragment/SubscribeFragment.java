package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.CustomLine;
import com.fongmi.android.tv.databinding.FragmentSubscribeBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.activity.CustomSiteActivity;
import com.fongmi.android.tv.ui.activity.LineListActivity;
import com.fongmi.android.tv.ui.activity.SiteListActivity;
import com.fongmi.android.tv.ui.activity.SubscriptionActivity;
import com.fongmi.android.tv.ui.adapter.SubscribeAdapter;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.LineSelectDialog;
import com.fongmi.android.tv.ui.dialog.SubscribeDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SubscribeFragment extends BaseFragment implements SubscribeAdapter.OnClickListener {

    private FragmentSubscribeBinding mBinding;
    private SubscribeAdapter mAdapter;
    private SubscribeDialog mDialog;
    private LineSelectDialog mLineDialog;

    public static SubscribeFragment newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt("type", type);
        SubscribeFragment fragment = new SubscribeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private int getType() {
        return getArguments().getInt("type");
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSubscribeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mAdapter = new SubscribeAdapter(this);
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        mBinding.recycler.setAdapter(mAdapter);
    }

    @Override
    protected void initData() {
        refresh();
    }

    public void onAdd() {
        mDialog = SubscribeDialog.create(this, getType());
        mDialog.show();
    }

    public void refresh() {
        List<Config> items = new ArrayList<>();
        if (getType() == 0) {
            // 自定义入口：传统自定义 + 用户自建的多条自定义线路
            items.add(Config.custom());
            for (CustomLine line : CustomLine.getAll()) items.add(toConfig(line));
            for (Config config : Config.getAll(getType())) {
                if (config.isCustom()) continue;
                items.add(config);
            }
        } else {
            items.addAll(Config.getAll(getType()));
        }
        String active = Prefers.getString("config_" + getType(), null);
        mBinding.empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        mAdapter.addAll(items, active);
    }

    private Config toConfig(CustomLine line) {
        return new Config().type(0).url(CustomLine.getUrl(line.getId())).name(line.getName());
    }

    @Override
    public void onSelect(Config item) {
        // 自定义线路直接作为当前点播源使用（不入库），避免与列表重复
        if (item.isCustom() && !item.isCustomSites()) {
            Prefers.put("config_" + getType(), item.getUrl());
            setConfig(item);
            return;
        }
        item.update();
        setConfig(item);
    }

    @Override
    public void onView(Config item) {
        if (item.isDepot()) {
            LineListActivity.start(getActivity(), item.getUrl(), getType(), item.getDesc());
        } else {
            SiteListActivity.start(getActivity(), item.getUrl(), getType(), item.getDesc());
        }
    }

    @Override
    public void onCustom(Config item) {
        if (item.isCustomSites()) CustomSiteActivity.start(getActivity());
        else CustomSiteActivity.start(getActivity(), item.getCustomLineId(), item.getName());
    }

    @Override
    public void onEdit(Config item) {
        if (item.isCustom() && !item.isCustomSites()) {
            mDialog = SubscribeDialog.edit(this, getType(), item);
            mDialog.show();
            return;
        }
        mDialog = SubscribeDialog.edit(this, getType(), item);
        mDialog.show();
    }

    @Override
    public void onCopy(Config item) {
        if (item.isCustom() && !item.isCustomSites()) {
            copyCustomLine(item);
            return;
        }
        Util.copy(item.getUrl());
    }

    private void copyCustomLine(Config item) {
        CustomLine source = CustomLine.find(item.getCustomLineId());
        if (source == null) return;
        CustomLine copy = new CustomLine();
        copy.setId("line_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        copy.setName(source.getName());
        copy.setUrl(source.getUrl());
        copy.setSites(new ArrayList<>(source.getSites()));
        copy.save();
        Notify.show(R.string.custom_line_copied);
        refresh();
    }

    @Override
    public void onDelete(Config item) {
        if (item.isCustom() && !item.isCustomSites()) {
            new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.custom_line_delete_title).setMessage(R.string.custom_line_delete_msg).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                CustomLine line = CustomLine.find(item.getCustomLineId());
                if (line != null) line.delete();
                // 同步清除曾作为源加载时写入数据库的 custom://<id> 记录
                Config.delete(item.getUrl(), getType());
                Notify.show(R.string.custom_line_deleted);
                refresh();
            }).show();
            return;
        }
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.subscribe_delete_title).setMessage(R.string.subscribe_delete).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
            Config.delete(item.getUrl(), getType());
            if (getType() == 0) Config.delete(item.getUrl(), 1);
            Notify.show(R.string.subscribe_deleted);
            refresh();
        }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        String path = FileChooser.getPathFromUri(getContext(), data.getData());
        if (path == null) {
            Notify.show(R.string.subscribe_file_error);
            return;
        }
        String url = "file:/" + path.replace(Path.rootPath(), "");
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.setUrl(url);
        } else if (mLineDialog != null && mLineDialog.isEditShowing()) {
            mLineDialog.setUrl(url);
        } else {
            importConfig(path);
        }
    }

    private void importConfig(String path) {
        String url = "file:/" + path.replace(Path.rootPath(), "");
        String name = new File(path).getName();
        if (name.toLowerCase().endsWith(".json")) name = name.substring(0, name.length() - 5);
        setConfig(Config.find(url, name, getType()));
    }

    private void setConfig(Config config) {
        if (config.getUrl().startsWith("file") && !PermissionX.isGranted(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        config.update();
        Notify.progress(getContext());
        if (getType() == 0) VodConfig.load(config, getCallback());
        else LiveConfig.load(config, getCallback());
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success(String result) {
                onLoaded();
            }

            @Override
            public void success() {
                onLoaded();
            }

            @Override
            public void error(String msg) {
                refreshEvent();
            }
        };
    }

    private void onLoaded() {
        refreshEvent();
        if (getType() == 0 && isSelect()) getActivity().finish();
    }

    private boolean isSelect() {
        return getActivity() instanceof SubscriptionActivity && ((SubscriptionActivity) getActivity()).isSelect();
    }

    private void refreshEvent() {
        Notify.dismiss();
        refresh();
        RefreshEvent.video();
        RefreshEvent.config();
    }
}
