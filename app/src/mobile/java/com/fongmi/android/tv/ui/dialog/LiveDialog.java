package com.fongmi.android.tv.ui.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.databinding.DialogLiveBinding;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.ui.adapter.LiveHomeAdapter;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class LiveDialog extends BaseDialog implements LiveHomeAdapter.OnClickListener {

    private DialogLiveBinding binding;
    private LiveCallback callback;
    private LiveHomeAdapter adapter;

    public static LiveDialog create() {
        return new LiveDialog();
    }

    public void show(Fragment fragment) {
        for (Fragment f : fragment.getChildFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(fragment.getChildFragmentManager(), null);
        if (fragment instanceof LiveCallback) callback = (LiveCallback) fragment;
    }

    public void show(FragmentActivity activity) {
        for (Fragment f : activity.getSupportFragmentManager().getFragments()) if (f instanceof BottomSheetDialogFragment) return;
        show(activity.getSupportFragmentManager(), null);
        if (activity instanceof LiveCallback) callback = (LiveCallback) activity;
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return binding = DialogLiveBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        adapter = new LiveHomeAdapter(this);
        binding.recycler.setAdapter(adapter);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.recycler.addItemDecoration(new SpaceItemDecoration(2, 8));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(LiveConfig.getHomeIndex()));
        setSearchView();
    }

    @Override
    protected void initEvent() {
    }

    private void setSearchView() {
        binding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) searchLive();
            return true;
        });
        binding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(android.text.Editable s) {
                searchLive();
            }
        });
        if (LiveConfig.get().getLives().size() < 10) binding.searchInput.setVisibility(View.GONE);
    }

    private void searchLive() {
        adapter.keyword(binding.keyword.getText().toString().trim());
    }

    @Override
    public void onTextClick(Live item) {
        if (callback != null) callback.setLive(item);
        dismiss();
    }
}
