package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.databinding.DialogLineSelectBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.LineSelectAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.fragment.SubscribeFragment;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LineSelectDialog implements LineSelectAdapter.OnClickListener {

    private final DialogLineSelectBinding binding;
    private final LineSelectAdapter adapter;
    private final Fragment fragment;
    private final Config config;
    private final AlertDialog dialog;

    public static LineSelectDialog create(Fragment fragment, Config config) {
        return new LineSelectDialog(fragment, config);
    }

    public LineSelectDialog(Fragment fragment, Config config) {
        this.fragment = fragment;
        this.config = config;
        this.binding = DialogLineSelectBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.adapter = new LineSelectAdapter(this);
        this.dialog = new MaterialAlertDialogBuilder(fragment.getActivity()).setTitle(R.string.dialog_site_line).setView(binding.getRoot()).setNegativeButton(R.string.dialog_negative, null).create();
    }

    public void show() {
        setRecyclerView();
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        binding.recycler.setAdapter(adapter.addAll(config.getLineList(), config.getLine()));
    }

    @Override
    public void onLineClick(Depot item) {
        dialog.dismiss();
        config.line(item.getUrl()).save();
        Notify.progress(fragment.getContext());
        if (config.getType() == 0) VodConfig.load(config, getCallback());
        else LiveConfig.load(config, getCallback());
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success(String result) {
                refresh();
            }

            @Override
            public void success() {
                refresh();
            }

            @Override
            public void error(String msg) {
                refresh();
            }
        };
    }

    private void refresh() {
        Notify.dismiss();
        if (fragment instanceof SubscribeFragment) ((SubscribeFragment) fragment).refresh();
        RefreshEvent.video();
        RefreshEvent.config();
    }
}
