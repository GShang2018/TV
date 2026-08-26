package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.DialogLineChooseBinding;
import com.fongmi.android.tv.ui.adapter.LineChooseAdapter;

import java.util.List;

public class LineChooseDialog extends BaseDialog implements LineChooseAdapter.OnClickListener {

    private DialogLineChooseBinding binding;
    private LineChooseAdapter adapter;
    private OnClickListener listener;
    private List<String> items;
    private int selected = -1;

    public static LineChooseDialog create() {
        return new LineChooseDialog();
    }

    public LineChooseDialog items(List<String> items) {
        this.items = items;
        return this;
    }

    public LineChooseDialog selected(int selected) {
        this.selected = selected;
        return this;
    }

    public LineChooseDialog listener(OnClickListener listener) {
        this.listener = listener;
        return this;
    }

    public LineChooseDialog show(FragmentActivity activity) {
        show(activity.getSupportFragmentManager(), null);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogLineChooseBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setAdapter(adapter = new LineChooseAdapter(this));
        if (items != null) adapter.addAll(items);
        setActivated();
        updateTitle();
    }

    private void setActivated() {
        if (selected == -1 || items == null || selected >= items.size()) return;
        adapter.setActivated(selected);
        scrollToSelected(selected);
    }

    private void scrollToSelected(int position) {
        // 与频道分类弹窗一致：post 到弹窗显示完成后滚动到选中项，布局未完成时直接滚动会被丢弃
        binding.recycler.post(() -> binding.recycler.scrollToPosition(position));
    }

    private void updateTitle() {
        int count = items == null ? 0 : items.size();
        binding.title.setText(count > 0 ? getString(R.string.live_line_title) + " (" + count + ")" : getString(R.string.live_line_title));
    }

    @Override
    public void onItemClick(int position) {
        if (listener != null) listener.onItemClick(position);
        dismiss();
    }

    public interface OnClickListener {

        void onItemClick(int position);
    }
}
