package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.bean.EpgData;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.databinding.AdapterChannelLiveBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class ChannelLiveAdapter extends RecyclerView.Adapter<ChannelLiveAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Channel> mItems;

    public ChannelLiveAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Channel item);

        void onKeepClick(Channel item);
    }

    public void addAll(List<Channel> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setSelected(int position) {
        if (position < 0 || position >= mItems.size()) return;
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setSelected(i == position);
        notifyItemRangeChanged(0, getItemCount());
    }

    public void changed(Channel item) {
        int position = mItems.indexOf(item);
        if (position == -1) return;
        notifyItemChanged(position);
    }

    public void remove(Channel item) {
        int position = mItems.indexOf(item);
        if (position == -1) return;
        mItems.remove(position);
        notifyItemRemoved(position);
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterChannelLiveBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel item = mItems.get(position);
        ImgUtil.loadLive(item.getLogo(), holder.binding.logo);
        holder.binding.name.setText(item.getName());
        // 频道当前节目 + 开始结束时间
        String program = "";
        String time = "";
        int index = item.getData().getSelected();
        if (index >= 0 && index < item.getData().getList().size()) {
            EpgData epg = item.getData().getList().get(index);
            program = epg.getTitle();
            time = epg.getStart() + "~" + epg.getEnd();
        }
        if (program.isEmpty()) program = ResUtil.getString(R.string.live_epg_none);
        holder.binding.program.setText(program);
        holder.binding.time.setText(time);
        holder.binding.time.setVisibility(time.isEmpty() ? View.GONE : View.VISIBLE);
        holder.binding.keep.setImageResource(Keep.exist(item.getName()) ? R.drawable.ic_control_keep_on : R.drawable.ic_control_keep_off);
        holder.binding.getRoot().setSelected(item.isSelected());
        // 选中频道时名称文字切换为主题色
        holder.binding.name.setSelected(item.isSelected());
        // 使用 bindingAdapterPosition，避免刷新后位置错位
        holder.binding.getRoot().setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) mListener.onItemClick(mItems.get(pos));
        });
        // 收藏按钮点击不触发频道点击
        holder.binding.keep.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) mListener.onKeepClick(mItems.get(pos));
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterChannelLiveBinding binding;

        ViewHolder(@NonNull AdapterChannelLiveBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
