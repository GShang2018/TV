package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.DialogSourceChooseBinding;
import com.fongmi.android.tv.ui.adapter.SourceChooseAdapter;

import java.util.List;

public class SourceChooseDialog extends BaseDialog implements SourceChooseAdapter.OnClickListener {

    private DialogSourceChooseBinding binding;
    private SourceChooseAdapter adapter;
    private OnClickListener listener;
    private List<Vod> items;
    private int selected = -1;

    public static SourceChooseDialog create() {
        return new SourceChooseDialog();
    }

    public SourceChooseDialog items(List<Vod> items) {
        this.items = items;
        return this;
    }

    public SourceChooseDialog selected(int selected) {
        this.selected = selected;
        return this;
    }

    public SourceChooseDialog listener(OnClickListener listener) {
        this.listener = listener;
        return this;
    }

    public void show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSourceChooseBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setActivated();
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setAdapter(adapter = new SourceChooseAdapter(this));
        adapter.addAll(items);
    }

    private void setActivated() {
        if (selected == -1 || selected >= items.size()) return;
        adapter.setActivated(selected);
        binding.recycler.scrollToPosition(selected);
    }

    @Override
    public void onItemClick(Vod item) {
        if (listener != null) listener.onItemClick(item);
        dismiss();
    }

    public interface OnClickListener {

        void onItemClick(Vod item);
    }
}
