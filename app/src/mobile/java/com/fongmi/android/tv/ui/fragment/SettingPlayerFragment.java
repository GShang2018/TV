package com.fongmi.android.tv.ui.fragment;

import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.FragmentSettingPlayerBinding;
import com.fongmi.android.tv.impl.CacheTimeCallback;
import com.fongmi.android.tv.impl.UaCallback;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.extractor.BtEngine;
import com.fongmi.android.tv.setting.ExoPerformanceSetting;
import com.fongmi.android.tv.ui.base.BaseFragment;
import com.fongmi.android.tv.ui.dialog.CacheTimeDialog;
import com.fongmi.android.tv.ui.dialog.UaDialog;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingPlayerFragment extends BaseFragment implements UaCallback, CacheTimeCallback {

    private FragmentSettingPlayerBinding mBinding;
    private String[] background;
    private String[] caption;
    private String[] player;
    private String[] decode;
    private String[] render;
    private String[] scale;
    private String[] http;
    private String[] flag;
    private String[] rtsp;

    public static SettingPlayerFragment newInstance() {
        return new SettingPlayerFragment();
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return mBinding = FragmentSettingPlayerBinding.inflate(inflater, container, false);
    }

    @Override
    protected void initView() {
        setVisible();
        mBinding.uaText.setText(Setting.getUa());
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
        mBinding.captionText.setText(getSwitch(Setting.isCaption()));
        mBinding.cacheTimeText.setText(String.valueOf(Setting.getCacheTime()));
        mBinding.bufferText.setText(ExoPerformanceSetting.getBufferText());
        mBinding.startBufferText.setText(ExoPerformanceSetting.getStartBufferText());
        mBinding.rebufferText.setText(ExoPerformanceSetting.getRebufferText());
        mBinding.btEngineText.setText(getSwitch(Setting.isBtEngineEnabled()));
        mBinding.trackerText.setText(getTrackerSummary());
        mBinding.playWithOthersText.setText(getSwitch(Setting.isPlayWithOthers()));
        mBinding.danmuLoadText.setText(getSwitch(Setting.isDanmuLoad()));
        mBinding.rtspText.setText((rtsp = ResUtil.getStringArray(R.array.select_rtsp))[Setting.getRtsp()]);
        mBinding.flagText.setText((flag = ResUtil.getStringArray(R.array.select_flag))[Setting.getFlag()]);
        mBinding.httpText.setText((http = ResUtil.getStringArray(R.array.select_exo_http))[Setting.getHttp()]);
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[Setting.getScale()]);
        mBinding.playerText.setText((player = ResUtil.getStringArray(R.array.select_player))[Setting.getPlayer()]);
        mBinding.decodeText.setText((decode = ResUtil.getStringArray(R.array.select_decode))[Setting.getDecode(Setting.getPlayer())]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.select_render))[Setting.getRender()]);
        mBinding.captionText.setText((caption = ResUtil.getStringArray(R.array.select_caption))[Setting.isCaption() ? 1 : 0]);
        mBinding.backgroundText.setText((background = ResUtil.getStringArray(R.array.select_background))[Setting.getBackground()]);
    }

    @Override
    protected void initEvent() {
        mBinding.ua.setOnClickListener(this::onUa);
        mBinding.rtsp.setOnClickListener(this::setRtsp);
        mBinding.http.setOnClickListener(this::setHttp);
        mBinding.flag.setOnClickListener(this::setFlag);
        mBinding.scale.setOnClickListener(this::onScale);
        mBinding.cacheTime.setOnClickListener(this::onCacheTime);
        mBinding.buffer.setOnClickListener(this::setBuffer);
        mBinding.startBuffer.setOnClickListener(this::setStartBuffer);
        mBinding.rebuffer.setOnClickListener(this::setRebuffer);
        mBinding.player.setOnClickListener(this::setPlayer);
        mBinding.decode.setOnClickListener(this::setDecode);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.caption.setOnClickListener(this::setCaption);
        mBinding.caption.setOnLongClickListener(this::onCaption);
        mBinding.playWithOthers.setOnClickListener(this::setPlayWithOthers);
        mBinding.danmuLoad.setOnClickListener(this::setDanmuLoad);
        mBinding.background.setOnClickListener(this::onBackground);
        mBinding.btEngine.setOnClickListener(this::setBtEngine);
        mBinding.tracker.setOnClickListener(this::setTracker);
    }

    private void setVisible() {
        mBinding.caption.setVisibility(Setting.hasCaption() ? View.VISIBLE : View.GONE);
        mBinding.http.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.cacheTime.setVisibility(Players.isExo(Setting.getPlayer()) ? View.GONE : View.VISIBLE);
        mBinding.buffer.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.startBuffer.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.rebuffer.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.tunnel.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.playWithOthers.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
    }

    private void onUa(View view) {
        UaDialog.create(this).show();
    }

    @Override
    public void setUa(String ua) {
        mBinding.uaText.setText(ua);
        Setting.putUa(ua);
    }

    private void setRtsp(View view) {
        int index = Setting.getRtsp();
        Setting.putRtsp(index = index == rtsp.length - 1 ? 0 : ++index);
        mBinding.rtspText.setText(rtsp[index]);
    }

    private void setHttp(View view) {
        int index = Setting.getHttp();
        Setting.putHttp(index = index == http.length - 1 ? 0 : ++index);
        mBinding.httpText.setText(http[index]);
    }

    private void setFlag(View view) {
        int index = Setting.getFlag();
        Setting.putFlag(index = index == flag.length - 1 ? 0 : ++index);
        mBinding.flagText.setText(flag[index]);
    }

    private void onScale(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.player_scale).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(scale, Setting.getScale(), (dialog, which) -> {
            mBinding.scaleText.setText(scale[which]);
            Setting.putScale(which);
            dialog.dismiss();
        }).show();
    }

    private void onCacheTime(View view) {
        CacheTimeDialog.create(this).show();
    }

    @Override
    public void setCacheTime(int seconds) {
        mBinding.cacheTimeText.setText(String.valueOf(seconds));
        Setting.putCacheTime(seconds);
    }

    private void setBuffer(View view) {
        ExoPerformanceSetting.nextBuffer();
        mBinding.bufferText.setText(ExoPerformanceSetting.getBufferText());
    }

    private void setStartBuffer(View view) {
        ExoPerformanceSetting.nextStartBuffer();
        mBinding.startBufferText.setText(ExoPerformanceSetting.getStartBufferText());
    }

    private void setRebuffer(View view) {
        ExoPerformanceSetting.nextRebuffer();
        mBinding.rebufferText.setText(ExoPerformanceSetting.getRebufferText());
    }

    private void setPlayer(View view) {
        int index = Setting.getPlayer();
        Setting.putPlayer(index = index == player.length - 1 ? 0 : ++index);
        mBinding.playerText.setText(player[index]);
        mBinding.decodeText.setText(decode[Setting.getDecode(index)]);
        setVisible();
    }

    private void setDecode(View view) {
        int player = Setting.getPlayer();
        int index = Setting.getDecode(player);
        Setting.putDecode(player, index = index == decode.length - 1 ? 0 : ++index);
        mBinding.decodeText.setText(decode[index]);
    }

    private void setRender(View view) {
        int index = Setting.getRender();
        Setting.putRender(index = index == render.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(render[index]);
        if (Setting.isTunnel() && Setting.getRender() == 1) setTunnel(view);
    }

    private void setTunnel(View view) {
        Setting.putTunnel(!Setting.isTunnel());
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
        if (Setting.isTunnel() && Setting.getRender() == 1) setRender(view);
    }

    private void setCaption(View view) {
        Setting.putCaption(!Setting.isCaption());
        mBinding.captionText.setText(caption[Setting.isCaption() ? 1 : 0]);
    }

    private boolean onCaption(View view) {
        if (Setting.isCaption()) startActivity(new Intent(Settings.ACTION_CAPTIONING_SETTINGS));
        return Setting.isCaption();
    }

    private void setPlayWithOthers(View view) {
        Setting.putPlayWithOthers(!Setting.isPlayWithOthers());
        mBinding.playWithOthersText.setText(getSwitch(Setting.isPlayWithOthers()));
    }

    private void setDanmuLoad(View view) {
        Setting.putDanmuLoad(!Setting.isDanmuLoad());
        mBinding.danmuLoadText.setText(getSwitch(Setting.isDanmuLoad()));
    }

    private void onBackground(View view) {
        new MaterialAlertDialogBuilder(getActivity()).setTitle(R.string.player_background).setNegativeButton(R.string.dialog_negative, null).setSingleChoiceItems(background, Setting.getBackground(), (dialog, which) -> {
            mBinding.backgroundText.setText(background[which]);
            Setting.putBackground(which);
            dialog.dismiss();
        }).show();
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

    private void setBtEngine(View view) {
        boolean enabled = !Setting.isBtEngineEnabled();
        Setting.putBtEngineEnabled(enabled);
        mBinding.btEngineText.setText(getSwitch(enabled));
        if (enabled) {
            BtEngine.ensureRunning();
        } else {
            BtEngine.shutdown();
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

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) initView();
    }
}
