package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.databinding.DialogSiteHomeBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.SiteHomeAdapter;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class SiteHomeDialog extends BaseDialog implements SiteHomeAdapter.OnClickListener {

    private DialogSiteHomeBinding binding;
    private SiteCallback callback;
    private SiteHomeAdapter adapter;

    public static SiteHomeDialog create() {
        return new SiteHomeDialog();
    }

    public void show(Fragment fragment) {
        for (Fragment f : fragment.getChildFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(fragment.getChildFragmentManager(), null);
        if (fragment instanceof SiteCallback) callback = (SiteCallback) fragment;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        FragmentManager manager = activity.getSupportFragmentManager();
        String tag = getClass().getName();
        // 防抖：弹窗已存在（含关闭动画中）时不重复叠加，避免快速连点出现两层弹窗
        if (manager.findFragmentByTag(tag) != null) return;
        show(manager, tag);
        if (activity instanceof SiteCallback) callback = (SiteCallback) activity;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogSiteHomeBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        adapter = new SiteHomeAdapter(this);
        binding.recycler.setAdapter(adapter);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(2, 8));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(VodConfig.getHomeIndex()));
        setSearchView();
    }

    @Override
    protected void initEvent() {
    }

    private void setSearchView() {
        binding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) searchSite();
            return true;
        });
        binding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                searchSite();
            }
        });
        if (VodConfig.get().getSites().size() < 10) binding.searchInput.setVisibility(View.GONE);
    }

    private void searchSite() {
        adapter.keyword(binding.keyword.getText().toString().trim());
    }

    @Override
    public void onTextClick(com.fongmi.android.tv.bean.Site item) {
        if (callback != null) callback.setSite(item);
        dismiss();
    }
}
