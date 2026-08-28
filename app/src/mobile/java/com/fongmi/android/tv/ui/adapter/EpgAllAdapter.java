package com.fongmi.android.tv.ui.adapter;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Reminder;
import com.fongmi.android.tv.databinding.AdapterEpgAllBinding;

import java.util.ArrayList;
import java.util.List;

public class EpgAllAdapter extends RecyclerView.Adapter<EpgAllAdapter.ViewHolder> {

    private static final int COLOR_LIVE = Color.parseColor("#4CAF50");
    private static final int COLOR_RESERVE = Color.parseColor("#FFC107");
    private static final int COLOR_ENDED = Color.parseColor("#80FFFFFF");
    private static final int COLOR_REPLAY = Color.parseColor("#FF7043");

    private final OnClickListener mListener;
    private final List<EpgData> mItems;
    private Channel mChannel;
    private boolean mReplayable;
    private int mPrimaryColor = -1;
    private int mColorOnSurface = -1;
    private int mColorOnSurfaceVariant = -1;

    public EpgAllAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        // 回看 / 正在播放的节目
        void onItemClick(EpgData item);

        // 预约：调起系统日历新建事件
        void onReserve(EpgData item);
    }

    public void setChannel(Channel channel) {
        this.mChannel = channel;
        this.mReplayable = channel != null && channel.hasCatchup();
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<EpgData> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterEpgAllBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EpgData item = mItems.get(position);
        resolveThemeColors(holder);
        int primary = getPrimaryColor(holder.binding.getRoot());
        boolean selected = item.isSelected();
        holder.binding.time.setText(item.getStart());
        holder.binding.title.setText(item.getTitle());
        // 选中(正在播放)的节目：时间、节目名变主题色
        holder.binding.time.setTextColor(selected ? primary : mColorOnSurfaceVariant);
        holder.binding.title.setTextColor(selected ? primary : mColorOnSurface);
        // 正在播放 / 直播中 / 预约(已预约) / 回看 / 已结束
        if (selected) {
            // 当前播放的节目（直播或回看选中）
            setStatus(holder, R.string.live_epg_playing, primary, false);
        } else if (item.isInRange()) {
            setStatus(holder, R.string.live_epg_live, COLOR_LIVE, false);
        } else if (item.isFuture()) {
            boolean reserved = mChannel != null && Reminder.exist(mChannel.getName(), item.getStartTime());
            setStatus(holder, reserved ? R.string.live_reserved : R.string.live_epg_reserve, reserved ? primary : COLOR_RESERVE, true);
        } else if (mReplayable) {
            // 已结束且支持回看
            setStatus(holder, R.string.live_replay, COLOR_REPLAY, true);
        } else {
            setStatus(holder, R.string.live_epg_ended, COLOR_ENDED, false);
        }
        holder.binding.getRoot().setOnClickListener(view -> {
            // 未来节目点击切换预约，已结束且支持回看/正在播放的节目点击播放
            if (item.isFuture()) mListener.onReserve(item);
            else if (mReplayable || item.isInRange()) mListener.onItemClick(item);
        });
    }

    private void setStatus(ViewHolder holder, int res, int color, boolean pill) {
        holder.binding.status.setText(res);
        holder.binding.status.setTextColor(color);
        if (pill) {
            holder.binding.status.setBackgroundResource(R.drawable.shape_epg_status);
            holder.binding.status.setBackgroundTintList(ColorStateList.valueOf((color & 0x00FFFFFF) | 0x26000000));
        } else {
            holder.binding.status.setBackgroundResource(0);
        }
    }

    // 主题默认文字色：未选中时时间/节目标题使用主题色，避免复用 View 时残留选中色
    private void resolveThemeColors(ViewHolder holder) {
        if (mColorOnSurface != -1 && mColorOnSurfaceVariant != -1) return;
        android.util.TypedValue tv = new android.util.TypedValue();
        holder.itemView.getContext().getTheme().resolveAttribute(R.attr.colorOnSurface, tv, true);
        mColorOnSurface = tv.data;
        holder.itemView.getContext().getTheme().resolveAttribute(R.attr.colorOnSurfaceVariant, tv, true);
        mColorOnSurfaceVariant = tv.data;
    }

    private int getPrimaryColor(View view) {
        if (mPrimaryColor == -1) {
            TypedArray a = view.getContext().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorPrimary});
            mPrimaryColor = a.getColor(0, 0xFFFFFFFF);
            a.recycle();
        }
        return mPrimaryColor;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterEpgAllBinding binding;

        ViewHolder(@NonNull AdapterEpgAllBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
