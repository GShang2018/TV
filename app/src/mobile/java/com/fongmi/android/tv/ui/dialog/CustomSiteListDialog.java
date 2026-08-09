package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.CustomSite;
import com.fongmi.android.tv.databinding.DialogCustomSiteListBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.ui.adapter.CustomSiteAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.Notify;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CustomSiteListDialog implements CustomSiteAdapter.OnClickListener {

    private final DialogCustomSiteListBinding binding;
    private final Fragment fragment;
    private final CustomSiteAdapter adapter;
    private final AlertDialog dialog;

    public static CustomSiteListDialog create(Fragment fragment) {
        return new CustomSiteListDialog(fragment);
    }

    public CustomSiteListDialog(Fragment fragment) {
        this.fragment = fragment;
        this.binding = DialogCustomSiteListBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.adapter = new CustomSiteAdapter(this);
        this.dialog = new MaterialAlertDialogBuilder(fragment.getActivity()).setTitle(R.string.setting_manage_custom_site).setView(binding.getRoot()).setNegativeButton(R.string.dialog_negative, null).create();
    }

    public void show() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(1, 8));
        binding.recycler.setAdapter(adapter.addAll(CustomSite.getAll()));
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) {
            Notify.show(R.string.custom_site_empty);
            return;
        }
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onEditClick(CustomSite item) {
        dialog.dismiss();
        CustomSiteDialog.create(fragment, item).show();
    }

    @Override
    public void onDeleteClick(CustomSite item) {
        new MaterialAlertDialogBuilder(fragment.getActivity()).setTitle(R.string.dialog_delete_custom_site_title).setMessage(R.string.dialog_delete_custom_site).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
            item.delete();
            Notify.show(R.string.custom_site_deleted);
            refresh();
            if (adapter.remove(item) == 0) this.dialog.dismiss();
        }).show();
    }

    private void refresh() {
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
