package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.adapter.SiteAdapter;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class SiteDialog implements SiteAdapter.OnClickListener {

    private final SiteCallback callback;
    private final SiteAdapter adapter;
    private final DialogSiteBinding binding;
    private final AlertDialog dialog;
    private List<Site> customSites;

    public static SiteDialog create(Activity activity) {
        return new SiteDialog(activity);
    }

    public static SiteDialog create(Fragment fragment) {
        return new SiteDialog(fragment);
    }

    public SiteDialog(Activity activity) {
        this.adapter = new SiteAdapter(this);
        this.callback = (SiteCallback) activity;
        this.binding = DialogSiteBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public SiteDialog(Fragment fragment) {
        this.adapter = new SiteAdapter(this);
        this.callback = (SiteCallback) fragment;
        this.binding = DialogSiteBinding.inflate(LayoutInflater.from(fragment.getActivity()));
        this.dialog = new MaterialAlertDialogBuilder(fragment.getActivity()).setView(binding.getRoot()).create();
    }

    public SiteDialog sites(List<Site> sites) {
        this.customSites = sites;
        return this;
    }

    public SiteDialog search() {
        adapter.search(true);
        return this;
    }

    public SiteDialog change() {
        adapter.change(true);
        return this;
    }

    public SiteDialog all() {
        adapter.change(true);
        return this;
    }

    public void show() {
        if (customSites != null) {
            adapter.setSites(customSites);
        }
        setRecyclerView();
        setSearchView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.setLayoutManager(new LinearLayoutManager(dialog.getContext()));
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
        binding.search.setOnClickListener(v -> searchSite());
        if (VodConfig.get().getSites().size() < 10) binding.searchInput.setVisibility(View.GONE);
    }

    private void searchSite() {
        adapter.keyword(binding.keyword.getText().toString().trim());
    }

    private void setDialog() {
        if (adapter.getItemCount() == 0) return;
        dialog.getWindow().setDimAmount(0);
        dialog.show();
        Window window = dialog.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.CENTER;
        window.setAttributes(params);
    }

    @Override
    public void onTextClick(Site item) {
        if (callback == null) return;
        callback.setSite(item);
        dialog.dismiss();
    }

    @Override
    public void onSearchClick(int position, Site item) {
        item.setSearchable(!item.isSearchable()).save();
        adapter.notifyItemChanged(position);
    }

    @Override
    public void onChangeClick(int position, Site item) {
        item.setChangeable(!item.isChangeable()).save();
        adapter.notifyItemChanged(position);
    }

    @Override
    public boolean onSearchLongClick(Site item) {
        boolean result = !item.isSearchable();
        for (Site site : VodConfig.get().getSites()) site.setSearchable(result).save();
        adapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public boolean onChangeLongClick(Site item) {
        boolean result = !item.isChangeable();
        for (Site site : VodConfig.get().getSites()) site.setChangeable(result).save();
        adapter.notifyDataSetChanged();
        return true;
    }
}
