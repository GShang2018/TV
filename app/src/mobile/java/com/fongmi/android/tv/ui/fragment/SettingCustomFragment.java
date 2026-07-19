package com.fongmi.android.tv.ui.fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

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
        mBinding.speedText.setText(getSpeedText());
        mBinding.incognitoText.setText(getSwitch(Setting.isIncognito()));
        mBinding.aggregatedSearchText.setText(getSwitch(Setting.isAggregatedSearch()));
        mBinding.homeDisplayNameText.setText(getSwitch(Setting.isHomeDisplayName()));
        mBinding.siteSearchText.setText(getSwitch(Setting.isSiteSearch()));
        mBinding.removeAdText.setText(getSwitch(Setting.isRemoveAd()));
        mBinding.debugText.setText(getSwitch(Setting.isDebug()));
        mBinding.languageText.setText((lang = ResUtil.getStringArray(R.array.select_language))[Setting.getLanguage()]);
        mBinding.configCacheText.setText((configCache = ResUtil.getStringArray(R.array.select_config_cache))[Setting.getConfigCache()]);
        mBinding.btEngineText.setText(getSwitch(Setting.isBtEngineEnabled()));
        mBinding.trackerText.setText(getTrackerSummary());
    }

    private String getTrackerSummary() {
        String list = Setting.getTrackerList();
        if (TextUtils.isEmpty(list)) return getString(R.string.setting_off);
        String[] lines = list.split("\n");
        int count = 0;
        for (String line : lines) {
            if (!TextUtils.isEmpty(line.trim()) && !line.trim().startsWith("#")) count++;
        }
        return count + " trackers";
    }

    @Override
    protected void initEvent() {
        mBinding.size.setOnClickListener(this::setSize);
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
        mBinding.btEngine.setOnClickListener(this::setBtEngine);
        mBinding.tracker.setOnClickListener(this::setTracker);
        mBinding.reset.setOnClickListener(this::onReset);
    }

    private void setSize(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_size).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(size, Setting.getSize(), (dialog, which) -> {
            mBinding.sizeText.setText(size[which]);
            Setting.putSize(which);
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

    private void setBtEngine(View view) {
        boolean enabled = !Setting.isBtEngineEnabled();
        Setting.putBtEngineEnabled(enabled);
        mBinding.btEngineText.setText(getSwitch(enabled));
        if (enabled) {
            com.fongmi.android.tv.player.extractor.BtEngine.ensureRunning();
        } else {
            com.fongmi.android.tv.player.extractor.BtEngine.shutdown();
        }
    }

    private void setTracker(View view) {
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_tracker, null);
        android.widget.EditText editText = dialogView.findViewById(R.id.editText);
        String trackerList = Setting.getTrackerList();
        if (TextUtils.isEmpty(trackerList)) {
            trackerList = "udp://tracker.opentrackr.org:1337/announce\n" +
                    "udp://tracker.openbittorrent.com:6969/announce\n" +
                    "udp://tracker.torrent.eu.org:451/announce\n" +
                    "udp://tracker.moeking.me:6969/announce\n" +
                    "udp://exodus.desync.com:6969/announce\n" +
                    "udp://open.demonii.com:1337/announce\n" +
                    "udp://tracker.cyberia.is:6969/announce\n" +
                    "udp://tracker.dler.org:6969/announce\n" +
                    "https://tracker.nanoha.org:443/announce\n" +
                    "https://tracker.lilithraws.org:443/announce\n" +
                    "http://tracker.bt4g.com:2095/announce\n" +
                    "http://tracker.files.fm:6969/announce";
        }
        editText.setText(trackerList);
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.setting_tracker)
                .setMessage(R.string.setting_tracker_hint)
                .setView(dialogView)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    String text = editText.getText().toString().trim();
                    Setting.putTrackerList(text);
                    mBinding.trackerText.setText(getTrackerSummary());
                })
                .create().show();
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
