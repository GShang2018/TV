package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogKeepAddedBinding;

public class KeepAddedDialog extends BaseDialog {

    private static final long AUTO_DISMISS_DELAY = 3000;

    private DialogKeepAddedBinding binding;
    private String folderName;
    private OnModifyListener listener;
    private final Runnable autoDismiss = this::dismiss;

    public static KeepAddedDialog create() {
        return new KeepAddedDialog();
    }

    public KeepAddedDialog folderName(String folderName) {
        this.folderName = folderName;
        return this;
    }

    public KeepAddedDialog listener(OnModifyListener listener) {
        this.listener = listener;
        return this;
    }

    public KeepAddedDialog show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogKeepAddedBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.message.setText(getString(R.string.keep_added_to, folderName));
    }

    @Override
    protected void initEvent() {
        binding.modify.setOnClickListener(view -> {
            if (listener != null) listener.onModify();
            dismiss();
        });
        App.post(autoDismiss, AUTO_DISMISS_DELAY);
    }

    @Override
    public void onDestroyView() {
        App.removeCallbacks(autoDismiss);
        super.onDestroyView();
    }

    public interface OnModifyListener {
        void onModify();
    }
}
