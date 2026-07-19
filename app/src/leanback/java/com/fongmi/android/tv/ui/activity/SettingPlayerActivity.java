package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.ActivitySettingPlayerBinding;
import com.fongmi.android.tv.impl.TrackerCallback;
import com.fongmi.android.tv.impl.UaCallback;
import com.fongmi.android.tv.player.Players;
import com.fongmi.android.tv.player.extractor.BtEngine;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.TrackerDialog;
import com.fongmi.android.tv.ui.dialog.UaDialog;
import com.fongmi.android.tv.utils.ResUtil;

public class SettingPlayerActivity extends BaseActivity implements UaCallback, TrackerCallback {

    private ActivitySettingPlayerBinding mBinding;
    private String[] caption;
    private String[] player;
    private String[] decode;
    private String[] render;
    private String[] scale;
    private String[] http;
    private String[] flag;
    private String[] rtsp;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingPlayerActivity.class));
    }

    private String getSwitch(boolean value) {
        return getString(value ? R.string.setting_on : R.string.setting_off);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingPlayerBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setVisible();
        mBinding.player.requestFocus();
        mBinding.uaText.setText(Setting.getUa());
        mBinding.btEngineText.setText(getSwitch(Setting.isBtEngineEnabled()));
        mBinding.trackerText.setText(getTrackerSummary());
        mBinding.tunnelText.setText(getSwitch(Setting.isTunnel()));
        mBinding.rtspText.setText((rtsp = ResUtil.getStringArray(R.array.select_rtsp))[Setting.getRtsp()]);
        mBinding.flagText.setText((flag = ResUtil.getStringArray(R.array.select_flag))[Setting.getFlag()]);
        mBinding.httpText.setText((http = ResUtil.getStringArray(R.array.select_exo_http))[Setting.getHttp()]);
        mBinding.scaleText.setText((scale = ResUtil.getStringArray(R.array.select_scale))[Setting.getScale()]);
        mBinding.playerText.setText((player = ResUtil.getStringArray(R.array.select_player))[Setting.getPlayer()]);
        mBinding.decodeText.setText((decode = ResUtil.getStringArray(R.array.select_decode))[Setting.getDecode(Setting.getPlayer())]);
        mBinding.renderText.setText((render = ResUtil.getStringArray(R.array.select_render))[Setting.getRender()]);
        mBinding.captionText.setText((caption = ResUtil.getStringArray(R.array.select_caption))[Setting.isCaption() ? 1 : 0]);
    }

    @Override
    protected void initEvent() {
        mBinding.ua.setOnClickListener(this::onUa);
        mBinding.rtsp.setOnClickListener(this::setRtsp);
        mBinding.http.setOnClickListener(this::setHttp);
        mBinding.flag.setOnClickListener(this::setFlag);
        mBinding.scale.setOnClickListener(this::setScale);
        mBinding.player.setOnClickListener(this::setPlayer);
        mBinding.decode.setOnClickListener(this::setDecode);
        mBinding.render.setOnClickListener(this::setRender);
        mBinding.tunnel.setOnClickListener(this::setTunnel);
        mBinding.caption.setOnClickListener(this::setCaption);
        mBinding.caption.setOnLongClickListener(this::onCaption);
        mBinding.btEngine.setOnClickListener(this::setBtEngine);
        mBinding.tracker.setOnClickListener(this::setTracker);
    }

    private void setVisible() {
        mBinding.caption.setVisibility(Setting.hasCaption() ? View.VISIBLE : View.GONE);
        mBinding.http.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
        mBinding.tunnel.setVisibility(Players.isExo(Setting.getPlayer()) ? View.VISIBLE : View.GONE);
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

    private void setScale(View view) {
        int index = Setting.getScale();
        Setting.putScale(index = index == scale.length - 1 ? 0 : ++index);
        mBinding.scaleText.setText(scale[index]);
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
        TrackerDialog.create(this).show();
    }

    @Override
    public void setTracker(String text) {
        mBinding.trackerText.setText(getTrackerSummary());
    }

}
