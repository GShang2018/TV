package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.databinding.DialogTypeBinding;
import com.fongmi.android.tv.ui.adapter.TypeAdapter;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class TypeDialog implements TypeAdapter.OnClickListener {

    private final TypeAdapter.OnClickListener listener;
    private DialogTypeBinding binding;
    private BottomSheetDialog dialog;
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
        this.dialog = new BottomSheetDialog(fragment.requireContext());
        this.dialog.setContentView(binding.getRoot());
        this.adapter = new TypeAdapter(this, true);
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
            Class item = adapter.get(i);
            android.widget.TextView textView = (android.widget.TextView) LayoutInflater.from(flexbox.getContext()).inflate(
                    com.fongmi.android.tv.R.layout.adapter_type_dialog, flexbox, false);
            textView.setText(item.getTypeName());
            textView.setSelected(item.isSelected());
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
        sheet.post(() -> behavior.setState(BottomSheetBehavior.STATE_EXPANDED));
    }

    // 全部分类太多导致内容超出屏幕时，wrap_content 的 ScrollView 没有滚动余量，最后一行会被窗口裁掉且无法露出；
    // 内容高于屏幕 65% 时将滚动区压到该高度内，保证可以滚动到底部（参考 DetailAllDialog 的高度约定）。
    // 必须在 show() 之前完成测量与压缩，让 BottomSheet 一次性以最终尺寸弹出，避免弹出后再跳变
    private void capScrollHeight() {
        int max = ResUtil.getScreenHeight() * 65 / 100;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(ResUtil.getScreenWidth(), View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        binding.scroll.measure(widthSpec, heightSpec);
        if (binding.scroll.getMeasuredHeight() <= max) return;
        ViewGroup.LayoutParams params = binding.scroll.getLayoutParams();
        params.height = max;
        binding.scroll.setLayoutParams(params);
    }

    @Override
    public void onItemClick(int position, Class item) {
        listener.onItemClick(position, item);
        dialog.dismiss();
    }
}
