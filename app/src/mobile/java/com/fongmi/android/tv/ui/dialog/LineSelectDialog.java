package com.fongmi.android.tv.ui.dialog;

import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.databinding.DialogLineSelectBinding;
import com.fongmi.android.tv.databinding.DialogSubscribeBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.LineSelectAdapter;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.fragment.SubscribeFragment;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineSelectDialog extends BaseDialog implements LineSelectAdapter.OnClickListener {

    private DialogLineSelectBinding binding;
    private DialogSubscribeBinding editBinding;
    private AlertDialog editDialog;
    private LineSelectAdapter adapter;
    private Fragment fragment;
    private Config config;
    private boolean all;
    private boolean append;
    private final Map<String, Config> mapping = new HashMap<>();

    public static LineSelectDialog create(Fragment fragment, Config config) {
        LineSelectDialog dialog = new LineSelectDialog();
        dialog.config = config;
        dialog.all = false;
        return dialog;
    }

    public static LineSelectDialog createAll(Fragment fragment) {
        LineSelectDialog dialog = new LineSelectDialog();
        dialog.config = VodConfig.get().getConfig();
        dialog.all = true;
        return dialog;
    }

    public void show(Fragment fragment) {
        this.fragment = fragment;
        for (Fragment f : fragment.getChildFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(fragment.getChildFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogLineSelectBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        adapter = new LineSelectAdapter(this);
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

    private Config getParent(Depot item) {
        return all ? mapping.get(item.getUrl()) : config;
    }

    @Override
    public void onLineClick(Depot item) {
        dismiss();
        Config target = getParent(item);
        if (target == null) return;
        Notify.progress(requireContext());
        if (target.isDepot()) target.line(item.getUrl()).save();
        else target.update();
        if (target.getType() == 0) VodConfig.load(target, getCallback());
        else LiveConfig.load(target, getCallback());
    }

    @Override
    public void onEdit(Depot item) {
        Config parent = getParent(item);
        if (parent == null || !parent.isDepot()) return;
        append = true;
        editBinding = DialogSubscribeBinding.inflate(LayoutInflater.from(requireContext()));
        editBinding.epgInput.setVisibility(View.GONE);
        editBinding.use.setVisibility(View.GONE);
        editBinding.name.setText(item.getName());
        editBinding.url.setText(item.getUrl());
        editBinding.choose.setEndIconOnClickListener(v -> FileChooser.from(fragment).show());
        editBinding.url.addTextChangedListener(new CustomTextListener() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detect(s.toString());
            }
        });
        editBinding.url.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) editDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
            return true;
        });
        editDialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.subscribe_line_edit_title).setView(editBinding.getRoot()).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_edit, (dialog, which) -> {
            String name = editBinding.name.getText().toString().trim();
            String url = UrlUtil.fixUrl(editBinding.url.getText().toString().trim());
            if (url.isEmpty()) {
                Notify.show(R.string.subscribe_edit_empty);
                return;
            }
            Util.hideKeyboard(editBinding.url);
            editDialog.dismiss();
            updateLine(parent, item, name, url);
        }).create();
        editDialog.getWindow().setDimAmount(0);
        editDialog.show();
    }

    public void setUrl(String url) {
        if (editBinding != null) editBinding.url.setText(url);
    }

    public boolean isEditShowing() {
        return editDialog != null && editDialog.isShowing();
    }

    private void detect(String s) {
        if (append && "h".equalsIgnoreCase(s)) {
            append = false;
            editBinding.url.append("ttp://");
        } else if (append && "f".equalsIgnoreCase(s)) {
            append = false;
            editBinding.url.append("ile://");
        } else if (append && "a".equalsIgnoreCase(s)) {
            append = false;
            editBinding.url.append("ssets://");
        } else if (s.length() > 1) {
            append = false;
        } else if (s.length() == 0) {
            append = true;
        }
    }

    @Override
    public void onCopy(Depot item) {
        Util.copy(item.getUrl());
    }

    @Override
    public void onDelete(Depot item) {
        Config parent = getParent(item);
        if (parent == null || !parent.isDepot()) return;
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.subscribe_line_delete_title).setMessage(R.string.subscribe_line_delete).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> deleteLine(parent, item)).show();
    }

    private void updateLine(Config parent, Depot target, String newName, String newUrl) {
        String oldUrl = target.getUrl();
        List<Depot> lines = parent.getLineList();
        for (Depot depot : lines) {
            if (depot.getUrl().equals(oldUrl)) {
                if (!TextUtils.isEmpty(newName)) depot.setName(newName);
                if (!newUrl.equals(oldUrl)) depot.setUrl(newUrl);
                break;
            }
        }
        if (!newUrl.equals(oldUrl) && parent.getLine() != null && parent.getLine().equals(oldUrl)) {
            parent.line(newUrl);
        }
        parent.lines(lines).save();
        Notify.show(R.string.subscribe_line_updated);
        refreshLines();
    }

    private void deleteLine(Config parent, Depot target) {
        List<Depot> lines = parent.getLineList();
        List<Depot> remaining = new ArrayList<>();
        for (Depot depot : lines) {
            if (!depot.getUrl().equals(target.getUrl())) remaining.add(depot);
        }
        if (parent.getLine() != null && parent.getLine().equals(target.getUrl())) {
            parent.line(remaining.isEmpty() ? null : remaining.get(0).getUrl());
        }
        parent.lines(remaining).save();
        Notify.show(R.string.subscribe_line_deleted);
        refreshLines();
    }

    private void refreshLines() {
        adapter.addAll(getLines(), getSelected());
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
