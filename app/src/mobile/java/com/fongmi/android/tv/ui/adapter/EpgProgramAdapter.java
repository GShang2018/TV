package com.fongmi.android.tv.ui.adapter;

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
import com.fongmi.android.tv.databinding.AdapterEpgProgramBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class EpgProgramAdapter extends RecyclerView.Adapter<EpgProgramAdapter.ViewHolder> {

    private static final int COLOR_LIVE = Color.parseColor("#4CAF50");
    private static final int COLOR_RESERVE = Color.parseColor("#FFFFFF");
    private static final int COLOR_ENDED = Color.parseColor("#99FFFFFF");
    private static final int COLOR_REPLAY = Color.parseColor("#FFFFFF");

    private final OnClickListener mListener;
    private final List<EpgData> mItems;
    private Channel mChannel;
    private boolean mReplayable;
    private int mPrimaryColor = -1;

    public EpgProgramAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        // 回看 / 正在播放的节目
        void onItemClick(EpgData item);

        // 预约 / 取消预约
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

    // 点击回看后高亮该节目卡片
    public void setSelected(EpgData item) {
        for (EpgData it : mItems) it.setSelected(it.equals(item));
        notifyDataSetChanged();
    }

    // 查找节目在列表中的位置，供播放页滚动定位
    public int indexOf(EpgData item) {
        return mItems.indexOf(item);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterEpgProgramBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EpgData item = mItems.get(position);
        String today = ResUtil.getString(R.string.live_epg_today_label);
        String start = item.getStart();
        holder.binding.title.setText(item.getTitle());
        holder.binding.time.setText(start.isEmpty() ? today : today + " " + start);
        boolean selected = item.isSelected();
        int primary = getPrimaryColor(holder.binding.getRoot());
        // 正在播放 / 直播中 / 预约(已预约) / 回看 / 已结束
        if (selected) {
            // 当前播放的节目（直播或回看选中），卡片选中为纯主题色底，文字固定白色保证可读
            holder.binding.status.setText(R.string.live_epg_playing);
            holder.binding.status.setTextColor(Color.WHITE);
        } else if (item.isInRange()) {
            holder.binding.status.setText(R.string.live_epg_live);
            holder.binding.status.setTextColor(COLOR_LIVE);
        } else if (item.isFuture()) {
            boolean reserved = mChannel != null && Reminder.exist(mChannel.getName(), item.getStartTime());
            holder.binding.status.setText(reserved ? R.string.live_reserved : R.string.live_epg_reserve);
            holder.binding.status.setTextColor(reserved ? primary : COLOR_RESERVE);
        } else if (mReplayable) {
            // 已结束且支持回看
            holder.binding.status.setText(R.string.live_replay);
            holder.binding.status.setTextColor(COLOR_REPLAY);
        } else {
            holder.binding.status.setText(R.string.live_epg_ended);
            holder.binding.status.setTextColor(COLOR_ENDED);
        }
        holder.binding.getRoot().setSelected(selected);
        holder.binding.getRoot().setOnClickListener(view -> {
            // 未来节目点击切换预约，已结束且支持回看/正在播放的节目点击播放
            if (item.isFuture()) mListener.onReserve(item);
            else if (mReplayable || item.isInRange()) mListener.onItemClick(item);
        });
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

        private final AdapterEpgProgramBinding binding;

        ViewHolder(@NonNull AdapterEpgProgramBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
