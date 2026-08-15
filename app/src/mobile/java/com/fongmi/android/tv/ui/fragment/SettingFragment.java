package com.fongmi.android.tv.ui.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.FragmentSettingBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.BackupCallback;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.ProxyCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.ui.activity.MainActivity;
import com.fongmi.android.tv.ui.activity.SubscriptionActivity;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.BackupDialog;
import com.fongmi.android.tv.ui.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.dialog.ProxyDialog;
import com.fongmi.android.tv.ui.dialog.TransmitActionDialog;
import com.fongmi.android.tv.ui.dialog.TransmitDialog;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.permissionx.guolindev.PermissionX;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends BaseFragment implements BackupCallback, ConfigCallback, SiteCallback, LiveCallback, ProxyCallback {

    private FragmentSettingBinding mBinding;
    private String[] backup;
    private int type;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }

    private int getDohIndex() {
        return Math.max(0, VodConfig.get().getDoh().indexOf(Doh.objectFrom(Setting.getDoh())));
    }

    private String[] getDohList() {
        List<String> list = new ArrayList<>();
        for (Doh item : VodConfig.get().getDoh()) list.add(item.getName());
        return list.toArray(new String[0]);
    }

    private MainActivity getRoot() {
        return (MainActivity) getActivity();
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        mBinding.backupText.setText((backup = ResUtil.getStringArray(R.array.select_backup))[Setting.getBackupMode()]);
        mBinding.aboutText.setText(BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + "-" + BuildConfig.FLAVOR_abi);
        mBinding.proxyText.setText(UrlUtil.scheme(Setting.getProxy()));
        setCacheText();
    }

    private void setCacheText() {
        FileUtil.getCacheSize(new Callback() {
            @Override
            public void success(String result) {
                mBinding.cacheText.setText(result);
            }
        });
    }

    @Override
    protected void initEvent() {
        mBinding.subscribeVod.setOnClickListener(v -> SubscriptionActivity.start(getActivity(), 0, false));
        mBinding.subscribeLive.setOnClickListener(v -> SubscriptionActivity.start(getActivity(), 1, false));
        mBinding.wall.setOnClickListener(this::onWall);
        mBinding.proxy.setOnClickListener(this::onProxy);
        mBinding.cache.setOnClickListener(this::onCache);
        mBinding.cache.setOnLongClickListener(this::onCacheLongClick);
        mBinding.transmit.setOnClickListener(this::onTransmit);
        mBinding.pull.setOnClickListener(this::onPull);
        mBinding.backup.setOnClickListener(this::onBackup);
        mBinding.backupNow.setOnClickListener(this::onBackupNow);
        mBinding.restore.setOnClickListener(this::onRestore);
        mBinding.player.setOnClickListener(this::onPlayer);
        mBinding.version.setOnClickListener(this::onVersion);
        mBinding.wall.setOnLongClickListener(this::onWallEdit);
        mBinding.version.setOnLongClickListener(this::onVersionDev);
        mBinding.wallDefault.setOnClickListener(this::setWallDefault);
        mBinding.wallRefresh.setOnClickListener(this::setWallRefresh);
        mBinding.doh.setOnClickListener(this::setDoh);
        mBinding.custom.setOnClickListener(this::onCustom);
        mBinding.about.setOnClickListener(this::onAbout);
    }

    @Override
    public void setConfig(Config config) {
        if (config.getUrl().startsWith("file") && !PermissionX.isGranted(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> load(config));
        } else {
            load(config);
        }
    }

    private void load(Config config) {
        switch (config.getType()) {
            case 0:
                Notify.progress(getActivity());
                VodConfig.load(config, getCallback());
                break;
            case 1:
                Notify.progress(getActivity());
                LiveConfig.load(config, getCallback());
                break;
            case 2:
                Notify.progress(getActivity());
                WallConfig.load(config, getCallback());
                mBinding.wallUrl.setText(config.getDesc());
                break;
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success(String result) {
                Notify.show(result);
            }

            @Override
            public void success() {
                setConfig();
            }

            @Override
            public void error(String msg) {
                Notify.show(msg);
                setConfig();
            }
        };
    }

    private void setConfig() {
        switch (type) {
            case 0:
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.config();
                // 若 JSON 配置中配置了 wallpaper 字段，则自动下载并应用该壁纸
                WallConfig.get().loadIfNeeded(new Callback() {
                    @Override
                    public void success() {
                        RefreshEvent.wall();
                    }

                    @Override
                    public void error(String msg) {
                    }
                });
                break;
            case 1:
                Notify.dismiss();
                RefreshEvent.config();
                break;
            case 2:
                Notify.dismiss();
                RefreshEvent.config();
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        VodConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void onChanged() {
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private void onWall(View view) {
        ConfigDialog.create(this).type(type = 2).show();
    }

    private boolean onWallEdit(View view) {
        ConfigDialog.create(this).type(type = 2).edit().show();
        return true;
    }

    private void onPlayer(View view) {
        getRoot().change(2);
    }

    private void onCustom(View view) {
        getRoot().change(3);
    }

    private void onAbout(View view) {
        mBinding.aboutText.setText(BuildConfig.FLAVOR_mode + "-" + BuildConfig.FLAVOR_api + "-" + BuildConfig.FLAVOR_abi);
    }

    private void onVersion(View view) {
        Updater.get().force().release().start(getActivity());
    }

    private boolean onVersionDev(View view) {
        Updater.get().force().dev().start(getActivity());
        return true;
    }

    private void setWallDefault(View view) {
        WallConfig.refresh(Setting.getWall() == 4 ? 1 : Setting.getWall() + 1);
    }

    private void setWallRefresh(View view) {
        Notify.progress(getActivity());
        WallConfig.get().load(new Callback() {
            @Override
            public void success() {
                Notify.dismiss();
                setCacheText();
            }
        });
    }

    private void setDoh(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.setting_doh).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(getDohList(), getDohIndex(), (dialog, which) -> {
            setDoh(VodConfig.get().getDoh().get(which));
            dialog.dismiss();
        }).show();
    }

    private void setDoh(Doh doh) {
        Source.get().stop();
        OkHttp.get().setDoh(doh);
        Notify.progress(getActivity());
        Setting.putDoh(doh.toString());
        mBinding.dohText.setText(doh.getName());
        VodConfig.load(Config.vod(), getCallback());
    }

    private void onProxy(View view) {
        ProxyDialog.create(this).show();
    }

    @Override
    public void setProxy(String proxy) {
        Source.get().stop();
        Setting.putProxy(proxy);
        OkHttp.selector().clear();
        OkHttp.get().setProxy(proxy);
        Notify.progress(getActivity());
        VodConfig.load(Config.vod(), getCallback());
        mBinding.proxyText.setText(UrlUtil.scheme(proxy));
    }

    private void onCache(View view) {
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.setting_cache_clear)
                .setMessage(R.string.setting_cache_clear_msg)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    FileUtil.clearCache(new Callback() {
                        @Override
                        public void success() {
                            VodConfig.get().getConfig().json("").save();
                            setCacheText();
                        }
                    });
                })
                .show();
    }

    private boolean onCacheLongClick(View view) {
        FileUtil.clearCache(new Callback() {
            @Override
            public void success() {
                setCacheText();
                Config config = VodConfig.get().getConfig().json("").save();
                if (!config.isEmpty()) setConfig(config);
            }
        });
        return true;
    }

    private void onRestore(View view) {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> {
            if (allGranted) BackupDialog.create(this).show();
        });
    }

    private void onTransmit(View view) {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> {
            if (allGranted) TransmitActionDialog.create(this).show();
        });
    }

    private void onPull(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.transmit_pull_restore).setMessage(R.string.transmit_pull_restore_desc).setNegativeButton(R.string.dialog_negative, null).setCancelable(true).setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
            TransmitDialog.create().pullRetore().show(this);
            dialog.dismiss();
        }).show();
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init().load();
        VodConfig.get().init().load(getCallback());
    }

    @Override
    public void restore(File file) {
        PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> AppDatabase.restore(file, new Callback() {
            @Override
            public void success() {
                if (allGranted) {
                    Notify.progress(getActivity());
                    App.post(() -> {
                        AppDatabase.reset();
                        initConfig();
                    }, 3000);
                }
            }
        }));
    }

    private void onBackup(View view) {
        new MaterialAlertDialogBuilder(getActivity())
                .setTitle(R.string.setting_backup)
                .setSingleChoiceItems(backup, Setting.getBackupMode(), (dialog, which) -> {
                    Setting.putBackupMode(which);
                    mBinding.backupText.setText(backup[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_negative, null)
                .show();
    }

    private void onBackupNow(View view) {
        Toast.makeText(getActivity(), R.string.backup_in_progress, Toast.LENGTH_SHORT).show();
        AppDatabase.backup(new Callback() {
            @Override
            public void success(String path) {
                Toast.makeText(getActivity(), ResUtil.getString(R.string.backup_to, path), Toast.LENGTH_LONG).show();
            }

            @Override
            public void error(String msg) {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) return;
        mBinding.wallUrl.setText(WallConfig.getDesc());
        mBinding.dohText.setText(getDohList()[getDohIndex()]);
        setCacheText();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != FileChooser.REQUEST_PICK_FILE) return;
        String path = FileChooser.getPathFromUri(getContext(), data.getData());
        if (FileChooser.type() == FileChooser.TYPE_APK) TransmitDialog.create().apk(path).show(getActivity());
        else if (FileChooser.type() == FileChooser.TYPE_PUSH_WALLPAPER) TransmitDialog.create().wallConfig(path).show(getActivity());
        else setConfig(Config.find("file:/" + path.replace(Path.rootPath(), ""), type));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        switch (event.getType()) {
            case CONFIG:
                setCacheText();
                mBinding.wallUrl.setText(WallConfig.getDesc());
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

}
