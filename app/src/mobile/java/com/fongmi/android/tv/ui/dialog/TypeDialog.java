package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.DialogTypeBinding;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class TypeDialog implements TypeAdapter.OnClickListener {

    private final TypeAdapter.OnClickListener listener;
    private DialogTypeBinding binding;
    private AlertDialog dialog;
    private TypeAdapter adapter;

    public static TypeDialog create(List<Class> items, int position, Fragment fragment) {
        return new TypeDialog(items, position, fragment);
    }

    public TypeDialog(List<Class> items, int position, Fragment fragment) {
        this.listener = (TypeAdapter.OnClickListener) fragment;
        init(fragment, items, position);
    }

    private void init(Fragment fragment, List<Class> items, int position) {
        this.binding = DialogTypeBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.dialog = new MaterialAlertDialogBuilder(fragment.getContext()).setView(binding.getRoot()).create();
        this.adapter = new TypeAdapter(this, true);
        this.adapter.addAll(items);
        this.adapter.setActivated(position);
    }

    public void show(FragmentManager manager, String tag) {
        setupFlexbox();
        setDialog();
    }

    private void setupFlexbox() {
        FlexboxLayout flexbox = binding.flexbox;
        flexbox.removeAllViews();
        for (int i = 0; i < adapter.getItemCount(); i++) {
            Class item = adapter.get(i);
            android.widget.TextView textView = (android.widget.TextView) LayoutInflater.from(flexbox.getContext()).inflate(
                    com.fongmi.android.tv.R.layout.adapter_type_dialog, flexbox, false);
            textView.setText(item.getTypeName());
            textView.setActivated(item.isActivated());
            int position = i;
            textView.setOnClickListener(v -> {
                listener.onItemClick(position, item);
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
    public void onItemClick(int position, Class item) {
        listener.onItemClick(position, item);
        dialog.dismiss();
    }
}
