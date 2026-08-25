package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.DialogTypeBinding;
import com.fongmi.android.tv.ui.adapter.GroupTabAdapter;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class GroupDialog implements GroupTabAdapter.OnClickListener {

    private final GroupTabAdapter.OnClickListener listener;
    private DialogTypeBinding binding;
    private AlertDialog dialog;
    private GroupTabAdapter adapter;

    public static GroupDialog create(List<Group> items, int position, Fragment fragment) {
        return new GroupDialog(items, position, fragment);
    }

    public GroupDialog(List<Group> items, int position, Fragment fragment) {
        this.listener = (GroupTabAdapter.OnClickListener) fragment;
        init(fragment, items, position);
    }

    private void init(Fragment fragment, List<Group> items, int position) {
        this.binding = DialogTypeBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.dialog = new MaterialAlertDialogBuilder(fragment.getContext()).setView(binding.getRoot()).create();
        this.adapter = new GroupTabAdapter(this, true);
        this.adapter.addAll(items);
        this.adapter.setSelected(position);
    }

    public void show(FragmentManager manager, String tag) {
        setupFlexbox();
        setDialog();
    }

    private void setupFlexbox() {
        FlexboxLayout flexbox = binding.flexbox;
        flexbox.removeAllViews();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            Group item = adapter.get(i);
            android.widget.TextView textView = (android.widget.TextView) LayoutInflater.from(flexbox.getContext()).inflate(
                    com.fongmi.android.tv.R.layout.adapter_type_dialog, flexbox, false);
            textView.setText(item.getName());
            textView.setSelected(item.isSelected());
            textView.setOnClickListener(v -> {
                listener.onItemClick(item);
                dialog.dismiss();
            });
            flexbox.addView(textView);
        }
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onItemClick(Group item) {
        listener.onItemClick(item);
        dialog.dismiss();
    }
}
