package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.databinding.DialogChannelChooseBinding;
import com.fongmi.android.tv.ui.adapter.ChannelChooseAdapter;

import java.util.List;

public class ChannelChooseDialog extends BaseDialog implements ChannelChooseAdapter.OnClickListener {

    private DialogChannelChooseBinding binding;
    private ChannelChooseAdapter adapter;
    private OnClickListener listener;
    private List<Channel> items;
    private int selected = -1;

    public static ChannelChooseDialog create() {
        return new ChannelChooseDialog();
    }

    public ChannelChooseDialog items(List<Channel> items) {
        this.items = items;
        return this;
    }

    public ChannelChooseDialog selected(int selected) {
        this.selected = selected;
        return this;
    }

    public ChannelChooseDialog listener(OnClickListener listener) {
        this.listener = listener;
        return this;
    }

    public ChannelChooseDialog show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogChannelChooseBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setAdapter(adapter = new ChannelChooseAdapter(this));
        if (items != null) adapter.addAll(items);
        setActivated();
        updateTitle();
    }

    private void setActivated() {
        if (selected == -1 || items == null || selected >= items.size()) return;
        adapter.setActivated(selected);
        binding.recycler.scrollToPosition(selected);
    }

    private void updateTitle() {
        int count = items == null ? 0 : items.size();
        binding.title.setText(count > 0 ? getString(R.string.live_channel) + " (" + count + ")" : getString(R.string.live_channel));
    }

    @Override
    public void onItemClick(Channel item) {
        if (listener != null) listener.onItemClick(item);
        dismiss();
    }

    public interface OnClickListener {

        void onItemClick(Channel item);
    }
}
