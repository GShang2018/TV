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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineSelectDialog implements LineSelectAdapter.OnClickListener {

    private final DialogLineSelectBinding binding;
    private final LineSelectAdapter adapter;
    private final Fragment fragment;
    private final Config config;
    private final AlertDialog dialog;
    private final Map<String, Config> mapping = new HashMap<>();
    private final boolean all;

    public static LineSelectDialog create(Fragment fragment, Config config) {
        return new LineSelectDialog(fragment, config, false);
    }

    public static LineSelectDialog createAll(Fragment fragment) {
        return new LineSelectDialog(fragment, VodConfig.get().getConfig(), true);
    }

    public LineSelectDialog(Fragment fragment, Config config, boolean all) {
        this.fragment = fragment;
        this.config = config;
        this.all = all;
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
        binding.recycler.setAdapter(adapter.addAll(getLines(), getSelected()));
    }

    private List<Depot> getLines() {
        if (!all) return config.getLineList();
        List<Depot> lines = new ArrayList<>();
        mapping.clear();
        List<Config> items = Config.getAll(config.getType());
        if (!containsCustom(items)) items.add(0, Config.custom());
        for (Config item : items) {
            if (item.isDepot()) {
                for (Depot depot : item.getLineList()) {
                    lines.add(new Depot(depot.getUrl(), depot.getName()));
                    mapping.put(depot.getUrl(), item);
                }
            } else {
                lines.add(new Depot(item.getUrl(), item.getDesc()));
                mapping.put(item.getUrl(), item);
            }
        }
        return lines;
    }

    private boolean containsCustom(List<Config> items) {
        for (Config item : items) if (item.isCustom()) return true;
        return false;
    }

    private String getSelected() {
        if (!all) return config.getLine();
        Config active = VodConfig.get().getConfig();
        if (active.isDepot()) return active.getLine();
        return active.getUrl();
    }

    @Override
    public void onLineClick(Depot item) {
        dialog.dismiss();
        Config target = all ? mapping.get(item.getUrl()) : config;
        if (target == null) return;
        Notify.progress(fragment.getContext());
        if (target.isDepot()) target.line(item.getUrl()).save();
        else target.update();
        if (target.getType() == 0) VodConfig.load(target, getCallback());
        else LiveConfig.load(target, getCallback());
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
