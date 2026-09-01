package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.DialogGroupChooseBinding;
import com.fongmi.android.tv.ui.adapter.GroupChooseAdapter;

import java.util.List;

public class GroupChooseDialog extends BaseDialog implements GroupChooseAdapter.OnClickListener {

    private DialogGroupChooseBinding binding;
    private GroupChooseAdapter adapter;
    private OnClickListener listener;
    private List<Group> items;
    private Group selected;

    public static GroupChooseDialog create() {
        return new GroupChooseDialog();
    }

    public GroupChooseDialog items(List<Group> items) {
        this.items = items;
        return this;
    }

    public GroupChooseDialog selected(Group selected) {
        this.selected = selected;
        return this;
    }

    public GroupChooseDialog listener(OnClickListener listener) {
        this.listener = listener;
        return this;
    }

    public GroupChooseDialog show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        String tag = getClass().getName();
        // 防抖：弹窗已存在（含关闭动画中）时不重复叠加，避免快速连点出现两层弹窗
        if (manager.findFragmentByTag(tag) != null) return this;
        show(manager, tag);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogGroupChooseBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setAdapter(adapter = new GroupChooseAdapter(this));
        if (items != null) adapter.addAll(items);
        setActivated();
        updateTitle();
    }

    private void setActivated() {
        if (selected == null || items == null || !items.contains(selected)) return;
        adapter.setActivated(selected);
        scrollToSelected(items.indexOf(selected));
    }

    private void scrollToSelected(int position) {
        // 与点播首页站源列表一致：post 到弹窗显示完成后滚动到选中项，布局未完成时直接滚动会被丢弃
        binding.recycler.post(() -> binding.recycler.scrollToPosition(position));
    }

    private void updateTitle() {
        int count = items == null ? 0 : items.size();
        binding.title.setText(count > 0 ? getString(R.string.live_group) + " (" + count + ")" : getString(R.string.live_group));
    }

    @Override
    public void onItemClick(Group item) {
        if (listener != null) listener.onItemClick(item);
        dismiss();
    }

    public interface OnClickListener {

        void onItemClick(Group item);
    }
}
