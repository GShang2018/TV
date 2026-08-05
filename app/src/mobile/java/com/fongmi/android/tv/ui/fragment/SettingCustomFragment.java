package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.FragmentSettingCustomBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.utils.LanguageUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.github.catvod.utils.Shell;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class SettingCustomFragment extends BaseFragment {

    private FragmentSettingCustomBinding mBinding;
    private String[] size;
    private String[] lang;
    private String[] configCache;
    private String[] themeColors;
    private String[] posterCrop;

    public static SettingCustomFragment newInstance() {
        return new SettingCustomFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingCustomBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        mBinding.sizeText.setText((size = ResUtil.getStringArray(R.array.select_size))[Setting.getSize()]);
        mBinding.posterCropText.setText((posterCrop = ResUtil.getStringArray(R.array.select_poster_crop))[Setting.getPosterCrop()]);
        mBinding.speedText.setText(getSpeedText());
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
        mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch()));
        mBinding.homeDisplayNameText.setText(getSwitch(Setting.isHomeDisplayName()));
        mBinding.siteSearchText.setText(getSwitch(Setting.isSiteSearch()));
        mBinding.removeAdText.setText(getSwitch(Setting.isRemoveAd()));
        mBinding.debugText.setText(getSwitch(Setting.isDebug()));
        mBinding.languageText.setText((lang = ResUtil.getStringArray(R.array.select_language))[Setting.getLanguage()]);
        mBinding.configCacheText.setText((configCache = ResUtil.getStringArray(R.array.select_config_cache))[Setting.getConfigCache()]);
        themeColors = ResUtil.getStringArray(R.array.select_theme_color);
        mBinding.themeColorText.setText(themeColors[getThemeColorIndex()]);
    }


    @Override
    protected void initEvent() {
        mBinding.size.setOnClickListener(this::setSize);
        mBinding.posterCrop.setOnClickListener(this::setPosterCrop);
        mBinding.speed.setOnClickListener(this::setSpeed);
        mBinding.speed.setOnLongClickListener(this::resetSpeed);
        mBinding.incognito.setOnClickListener(this::setIncognito);
        mBinding.aggregatedSearch.setOnClickListener(this::setAggregatedSearch);
        mBinding.homeDisplayName.setOnClickListener(this::setHomeDisplayName);
        mBinding.siteSearch.setOnClickListener(this::setSiteSearch);
        mBinding.removeAd.setOnClickListener(this::setRemoveAd);
        mBinding.debug.setOnClickListener(this::setDebug);
        mBinding.language.setOnClickListener(this::setLanguage);
        mBinding.configCache.setOnClickListener(this::setConfigCache);
        mBinding.themeColor.setOnClickListener(this::setThemeColor);
        mBinding.reset.setOnClickListener(this::onReset);
    }

    private int getThemeColorIndex() {
        String[] all = {"green", "blue", "red", "purple", "orange", "teal", "pink"};
        String current = Setting.getThemeColor();
        for (int i = 0; i < all.length; i++) {
            if (all[i].equals(current)) return i;
        }
        return 0;
    }

    private void setSize(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(size, Setting.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(size[which]);
            Setting.putSize(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private void setPosterCrop(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_poster_crop).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(posterCrop, Setting.getPosterCrop(), (dialog, which) -> {
            mBinding.posterCropText.setText(posterCrop[which]);
            Setting.putPosterCrop(which);
            RefreshEvent.size();
            dialog.dismiss();
        }).show();
    }

    private String getSpeedText() {
        return String.format(Locale.getDefault(), "%.2f", Setting.getPlaySpeed());
    }

    private void setSpeed(View view) {
        float speed = Setting.getPlaySpeed();
        float addon = speed >= 2 ? 1.0f : 0.1f;
        speed = speed >= 5 ? 0.2f : Math.min(speed + addon, 5.0f);
        Setting.putPlaySpeed(speed);
        mBinding.speedText.setText(getSpeedText());
    }

    private boolean resetSpeed(View view) {
        Setting.putPlaySpeed(1.0f);
        mBinding.speedText.setText(getSpeedText());
        return true;
    }

    private void setIncognito(View view) {
        Setting.putIncognito(!Setting.isIncognito());
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
    }

    private void setAggregatedSearch(View view) {
        Setting.putAggregatedSearch(!Setting.isAggregatedSearch());
        mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch()));
    }

    private void setHomeDisplayName(View view) {
        Setting.putHomeDisplayName(!Setting.isHomeDisplayName());
        mBinding.homeDisplayNameText.setText(getSwitch(Setting.isHomeDisplayName()));
        RefreshEvent.config();
    }

    private void setSiteSearch(View view) {
        Setting.putSiteSearch(!Setting.isSiteSearch());
        mBinding.siteSearchText.setText(getSwitch(Setting.isSiteSearch()));
    }

    private void setRemoveAd(View view) {
        Setting.putRemoveAd(!Setting.isRemoveAd());
        mBinding.removeAdText.setText(getSwitch(Setting.isRemoveAd()));
    }

    private void setDebug(View view) {
        boolean debug = !Setting.isDebug();
        Setting.putDebug(debug);
        mBinding.debugText.setText(getSwitch(debug));
        if (debug) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Server.get().getAddress("/log.html")));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void setLanguage(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_language).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(lang, Setting.getLanguage(), (dialog, which) -> {
            mBinding.languageText.setText(lang[which]);
            Setting.putLanguage(which);
            LanguageUtil.setLocale(LanguageUtil.getLocale(Setting.getLanguage()));
            dialog.dismiss();
            App.post(() -> Util.restartApp(getActivity()), 1000);
        }).show();
    }

    private void setConfigCache(View view) {
        int index = Setting.getConfigCache();
        Setting.putConfigCache(index = index == configCache.length - 1 ? 0 : ++index);
        mBinding.configCacheText.setText(configCache[index]);
    }

    private final String[] THEME_COLOR_VALUES = {"green", "blue", "red", "purple", "orange", "teal", "pink"};
    private final String[] THEME_COLOR_HEX = {"#1DB954", "#2196F3", "#F44336", "#9C27B0", "#FF9800", "#009688", "#E91E63"};

    private void setThemeColor(View view) {
        AlertDialog dialog = (AlertDialog) new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.setting_theme_color)
                .setNegativeButton(R.string.dialog_negative, null)
                .create();

        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_theme_color, null);
        dialog.setView(contentView);

        ListView colorList = contentView.findViewById(R.id.colorList);

        int checkedIndex = getThemeColorIndex();
        String[] items = new String[themeColors.length];
        int[] colors = new int[themeColors.length];
        for (int i = 0; i < themeColors.length; i++) {
            items[i] = themeColors[i];
            colors[i] = Color.parseColor(THEME_COLOR_HEX[i]);
        }

        colorList.setAdapter(new android.widget.ArrayAdapter<String>(getActivity(), R.layout.item_theme_color, R.id.colorText, items) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row = super.getView(position, convertView, parent);
                View dot = row.findViewById(R.id.colorDot);
                GradientDrawable bg = (GradientDrawable) dot.getBackground();
                bg.setColor(colors[position]);
                dot.setBackground(bg);
                if (position == checkedIndex) {
                    row.setBackgroundColor(0x0FFFFFFF);
                } else {
                    row.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                }
                return row;
            }
        });
        colorList.setItemChecked(checkedIndex, true);

        colorList.setOnItemClickListener((parent, v, position, id) -> {
            mBinding.themeColorText.setText(themeColors[position]);
            Setting.putThemeColor(THEME_COLOR_VALUES[position]);
            dialog.dismiss();
            App.post(() -> Util.restartApp(getActivity()), 500);
        });

        dialog.show();
    }

    private void onReset(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.dialog_reset_app).setMessage(R.string.dialog_reset_app_data).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> reset()).show();
    }

    private void reset() {
        new Thread(() -> {
            Shell.exec("pm clear " + App.get().getPackageName());
        }).start();
    }

}
