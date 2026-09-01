package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
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

    public SourceChooseDialog show(FragmentActivity activity) {
        FragmentManager manager = activity.getSupportFragmentManager();
        String tag = getClass().getName();
        // 防抖：弹窗已存在（含关闭动画中）时不重复叠加，避免快速连点出现两层弹窗
        if (manager.findFragmentByTag(tag) != null) return this;
        show(manager, tag);
        return this;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSourceChooseBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setRecyclerView();
        // 初始无结果时展示加载动画，结果实时回填后由 refresh 切换
        setLoading(items == null || items.isEmpty());
        setActivated();
        updateTitle();
    }

    /**
     * 检索结果实时回填：更新列表数据与选中项。
     * 有数据时关闭加载动画；空结果保持当前加载态，由 finish() 在检索结束时统一关闭。
     */
    public void refresh(List<Vod> items, int selected) {
        if (adapter == null) return;
        this.items = items;
        this.selected = selected;
        adapter.addAll(items);
        setActivated();
        if (items != null && !items.isEmpty()) setLoading(false);
        updateTitle();
    }

    /**
     * 检索结束：关闭加载动画（空态提示由布局展示）。
     * 由 VideoActivity 在静默期结束 / 兜底超时时调用。
     */
    public void finish() {
        if (!isVisible()) return;
        setLoading(false);
    }

    private void setRecyclerView() {
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setAdapter(adapter = new SourceChooseAdapter(this));
        if (items != null) adapter.addAll(items);
    }

    private void setActivated() {
        if (selected == -1 || items == null || selected >= items.size()) return;
        adapter.setActivated(selected);
        binding.recycler.scrollToPosition(selected);
    }

    private void setLoading(boolean loading) {
        if (binding == null) return;
        boolean empty = !loading && (items == null || items.isEmpty());
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recycler.setVisibility(loading || empty ? View.GONE : View.VISIBLE);
    }

    private void updateTitle() {
        if (binding == null) return;
        int count = items == null ? 0 : items.size();
        if (count > 0) {
            binding.title.setText(getString(R.string.dialog_source_count, count));
        } else {
            binding.title.setText(R.string.dialog_source_choose);
        }
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
