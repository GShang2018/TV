package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.DialogSiteBinding;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.ui.custom.CustomTextListener;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class SiteDialog {

    private final SiteCallback callback;
    private DialogSiteBinding binding;
    private AlertDialog dialog;
    private List<Site> allSites;
    private List<Site> filteredSites;
    private List<Site> customSites;
    private boolean change;

    public static SiteDialog create(Activity activity) {
        return new SiteDialog(activity);
    }

    public static SiteDialog create(Fragment fragment) {
        return new SiteDialog(fragment);
    }

    public SiteDialog(Activity activity) {
        this.callback = (SiteCallback) activity;
        init(activity);
    }

    public SiteDialog(Fragment fragment) {
        this.callback = (SiteCallback) fragment;
        init(fragment.getActivity());
    }

    private void init(Activity activity) {
        this.binding = DialogSiteBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
        this.allSites = VodConfig.get().getSites();
        this.filteredSites = new ArrayList<>(allSites);
    }

    public SiteDialog sites(List<Site> sites) {
        this.customSites = sites;
        return this;
    }

    public SiteDialog search() {
        return this;
    }

    public SiteDialog change() {
        this.change = true;
        return this;
    }

    public SiteDialog all() {
        this.change = true;
        return this;
    }

    public void show() {
        if (customSites != null) {
            this.allSites = customSites;
            this.filteredSites = new ArrayList<>(allSites);
            binding.title.setVisibility(View.VISIBLE);
        }
        setupFlexbox();
        setSearchView();
        setDialog();
    }

    private void setupFlexbox() {
        FlexboxLayout flexbox = binding.flexbox;
        flexbox.removeAllViews();
        for (int i = 0; i < filteredSites.size(); i++) {
            Site item = filteredSites.get(i);
            android.widget.TextView textView = (android.widget.TextView) LayoutInflater.from(flexbox.getContext()).inflate(
                    R.layout.adapter_type_dialog, flexbox, false);
            textView.setText(item.getName());
            textView.setSelected(item.isActivated());
            textView.setTextColor(androidx.core.content.res.ResourcesCompat.getColorStateList(flexbox.getContext().getResources(), com.fongmi.android.tv.R.color.selector_site_text, flexbox.getContext().getTheme()));
            textView.setOnClickListener(v -> onTextClick(item));
            if (change) {
                textView.setOnLongClickListener(v -> {
                    onChangeLongClick(item);
                    return true;
                });
            }
            flexbox.addView(textView);
        }
    }

    private void setDialog() {
        if (filteredSites.size() == 0) return;
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    private void setSearchView() {
        binding.keyword.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) searchSite();
            return true;
        });
        binding.keyword.addTextChangedListener(new CustomTextListener() {
            @Override
            public void afterTextChanged(Editable s) {
                searchSite();
            }
        });
        binding.search.setOnClickListener(v -> searchSite());
        if (allSites.size() < 10 || !Setting.isSiteSearch()) binding.searchInput.setVisibility(View.GONE);
    }

    private void searchSite() {
        String keyword = binding.keyword.getText().toString().trim();
        filteredSites.clear();
        if (TextUtils.isEmpty(keyword)) {
            filteredSites.addAll(allSites);
        } else {
            for (Site site : allSites) {
                if (site.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    filteredSites.add(site);
                }
            }
        }
        setupFlexbox();
    }

    private void onTextClick(Site item) {
        if (callback == null) return;
        callback.setSite(item);
        dialog.dismiss();
    }

    private void onChangeLongClick(Site item) {
        boolean result = !item.isChangeable();
        for (Site site : VodConfig.get().getSites()) site.setChangeable(result).save();
        filteredSites.clear();
        filteredSites.addAll(allSites);
        setupFlexbox();
    }
}
