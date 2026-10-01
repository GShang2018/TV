package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Group;
import com.fongmi.android.tv.databinding.DialogTypeBinding;
import com.fongmi.android.tv.ui.adapter.GroupTabAdapter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class GroupDialog implements GroupTabAdapter.OnClickListener {

    private final GroupTabAdapter.OnClickListener listener;
    private final Fragment fragment;
    private DialogTypeBinding binding;
    private BottomSheetDialog dialog;
    private GroupTabAdapter adapter;

    public static GroupDialog create(List<Group> items, int position, Fragment fragment) {
        return new GroupDialog(items, position, fragment);
    }

    public GroupDialog(List<Group> items, int position, Fragment fragment) {
        this.listener = (GroupTabAdapter.OnClickListener) fragment;
        this.fragment = fragment;
        init(fragment, items, position);
    }

    private void init(Fragment fragment, List<Group> items, int position) {
        this.binding = DialogTypeBinding.inflate(LayoutInflater.from(fragment.getContext()));
        this.dialog = new BottomSheetDialog(fragment.requireContext());
        this.dialog.setContentView(binding.getRoot());
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
        // 底部弹出（BottomSheet 自带从底部滑入动画，无需自定义窗口动画）
        capScrollHeight();
        dialog.show();
        expandSheet();
    }

    // 大屏横屏等场景下 BottomSheet 可能默认以 collapsed 高度出现，需要拖拽才能展开；
    // 这里强制 fitToContents 并直接展开，保证一次性以完整内容高度弹出；
    // skipCollapsed=true：向下拖拽时不再停留在 collapsed 高度，而是直接一次性滑出关闭
    private void expandSheet() {
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) return;
        BottomSheetBehavior behavior = BottomSheetBehavior.from(sheet);
        behavior.setFitToContents(true);
        behavior.setSkipCollapsed(true);
        // 先重新布局再展开：Behavior 的展开偏移是按内容高度缓存的，直接 setState 可能沿用旧高度，
        // 弹窗底部会离屏底一段距离（拖拽后才恢复）。requestLayout 后再 setState 会等布局完成才 settle
        sheet.post(() -> {
            sheet.requestLayout();
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });
    }

    // 全部分类太多导致内容超出屏幕时，wrap_content 的 ScrollView 没有滚动余量，最后一行会被窗口裁掉且无法露出；
    // 弹窗上沿不越过直播页 tab：可用高度 = 屏高 - tab 顶部，再扣掉弹窗自身把手/内边距等固定高度才是滚动区高度。
    // 必须在 show() 之前完成测量与压缩，让 BottomSheet 一次性以最终尺寸弹出，避免弹出后再跳变
    private void capScrollHeight() {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(ResUtil.getScreenWidth(), View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        binding.scroll.measure(widthSpec, heightSpec);
        binding.getRoot().measure(widthSpec, heightSpec);
        int fixed = binding.getRoot().getMeasuredHeight() - binding.scroll.getMeasuredHeight();
        int max = Math.max(getAvailableHeight() - fixed, ResUtil.getScreenHeight() / 4);
        if (binding.scroll.getMeasuredHeight() <= max) return;
        ViewGroup.LayoutParams params = binding.scroll.getLayoutParams();
        params.height = max;
        binding.scroll.setLayoutParams(params);
    }

    // 以 tab 顶部为界的可用高度；取不到 tab（非直播页调用）时退回屏高 65%，并保证至少 1/3 屏高
    private int getAvailableHeight() {
        int screen = ResUtil.getScreenHeight();
        if (fragment.getActivity() == null) return screen * 65 / 100;
        View tab = fragment.getActivity().findViewById(R.id.tabLayout);
        if (tab == null) return screen * 65 / 100;
        int[] location = new int[2];
        tab.getLocationOnScreen(location);
        return Math.max(screen - location[1], screen / 3);
    }

    @Override
    public void onItemClick(Group item) {
        listener.onItemClick(item);
        dialog.dismiss();
    }
}
