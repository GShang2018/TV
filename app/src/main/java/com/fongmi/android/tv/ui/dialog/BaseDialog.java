package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;

public abstract class BaseDialog extends BottomSheetDialogFragment {

    // Activity 重建（旋转 / recreate / 配置变更）恢复出来的空壳实例标记：外部传入的数据全丢，不能再绑定视图
    private boolean restored;

    protected abstract ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // DialogFragment 会被 FragmentManager 原样恢复，但外部传入的数据（标题资源 id、列表等）不会恢复；
        // 此时继续绑定视图会因字段为空崩溃（典型：setText(0) → Resources$NotFoundException），故直接关闭
        restored = savedInstanceState != null;
        if (restored) dismissAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 重建的空壳不再 inflate / 绑定：dismiss 事务要等下一轮才生效，本帧仍会走完视图创建流程
        if (restored) return new View(requireContext());
        return wrapWithDragHandle(getBinding(inflater, container).getRoot());
    }

    private View wrapWithDragHandle(View content) {
        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);

        BottomSheetDragHandleView dragHandle = new BottomSheetDragHandleView(requireContext());
        // 设置固定高度（例如 16dp）
        int heightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        // 移除默认内边距，进一步压缩视觉高度
        dragHandle.setPadding(0, 0, 0, 0);
        dragHandle.setMinimumHeight(0);
        wrapper.addView(dragHandle, handleParams);

        ViewGroup.LayoutParams contentParams = content.getLayoutParams();
        int height = (contentParams != null && contentParams.height == ViewGroup.LayoutParams.MATCH_PARENT)
                ? ViewGroup.LayoutParams.MATCH_PARENT
                : ViewGroup.LayoutParams.WRAP_CONTENT;
        wrapper.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        return wrapper;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (restored) return;
        initView();
        initEvent();
    }

    protected void initView() {
    }

    protected void initEvent() {
    }

    protected boolean transparent() {
        return false;
    }

    protected void setDimAmount(float amount) {
        getDialog().getWindow().setDimAmount(amount);
        getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    /**
     * 弹窗展开前测量内容区（列表）可用高度：maxHeight 扣掉标题 / 拖动条等固定部分。
     * 必须在展开前调用——展开后再改高度，BottomSheetBehavior 仍按旧高度缓存的展开偏移定位，
     * 弹窗底部会离屏底一段距离，只有拖拽触发重新布局才恢复。测量后可直接读 content.getMeasuredHeight()。
     */
    protected int getContentMaxHeight(@NonNull View content, int maxHeight) {
        View root = getView();
        if (root == null) return maxHeight;
        int widthSpec = View.MeasureSpec.makeMeasureSpec(ResUtil.getScreenWidth(), View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        root.measure(widthSpec, heightSpec);
        int fixed = Math.max(root.getMeasuredHeight() - content.getMeasuredHeight(), 0);
        return Math.max(maxHeight - fixed, ResUtil.getScreenHeight() / 4);
    }

    /**
     * 内容高度在弹窗展开后发生变化（如异步数据到达）时强制重新定位，贴回屏幕底部。
     * requestLayout 是必须的：Behavior 的展开偏移只在布局时按内容高度重算。
     */
    protected void relayoutSheet() {
        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) return;
        FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) return;
        sheet.requestLayout();
        BottomSheetBehavior.from(sheet).setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setOnShowListener((DialogInterface f) -> setBehavior(dialog));
        return dialog;
    }

    private void setBehavior(BottomSheetDialog dialog) {
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setFitsSystemWindows(false);
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }
        if (dialog.getWindow() != null) {
            WindowCompat.setDecorFitsSystemWindows(dialog.getWindow(), false);
            dialog.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }
    }
}
