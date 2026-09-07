package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
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
import com.fongmi.android.tv.databinding.AdapterChannelLiveViewBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class ChannelLiveAdapter extends RecyclerView.Adapter<ChannelLiveAdapter.ViewHolder> {

    // 列表形态：0=台标在左(横排) 1=台标在上(大台标放大)
    public static final int VIEW_LIST = 0;
    public static final int VIEW_BIG = 1;

    private final OnClickListener mListener;
    private final List<Channel> mItems;
    private int mViewType;
    private int mSelectedPosition = -1;

    public ChannelLiveAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Channel item);

        void onKeepClick(Channel item);
    }

    public void setViewType(int viewType) {
        if (mViewType == viewType) return;
        mViewType = viewType;
        notifyDataSetChanged();
    }

    public void addAll(List<Channel> items) {
        mItems.clear();
        mItems.addAll(items);
        mSelectedPosition = -1;
        notifyDataSetChanged();
    }

    public void setSelected(int position) {
        if (position < 0 || position >= mItems.size()) return;
        for (int i = 0; i < mItems.size(); i++) mItems.get(i).setSelected(i == position);
        int old = mSelectedPosition;
        mSelectedPosition = position;
        if (old == position) return;
        // 只精确刷新新旧两个选中项：全列表 rebind 会让所有台标经 Glide 清空重载，整表闪一下
        if (old >= 0 && old < mItems.size()) notifyItemChanged(old);
        notifyItemChanged(position);
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
    public int getItemViewType(int position) {
        return mViewType;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_BIG) {
            return new ViewHolder(AdapterChannelLiveViewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        return new ViewHolder(AdapterChannelLiveBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel item = mItems.get(position);
        boolean big = mViewType == VIEW_BIG;
        if (big) {
            // 4:3 封面区由 AspectRatioImageView 在测量期直接定高（宽*3/4），首帧几何即最终几何；
            // 加载用显式 override（屏宽 × 4:3），请求尺寸固定、与布局时序解耦，首次解码即清晰（对齐点播封面逻辑）
            loadLogo(holder, item, holder.viewBinding.logo, true);
            holder.viewBinding.name.setText(item.getName());
            bindProgram(holder.viewBinding.program, holder.viewBinding.time, item);
            holder.viewBinding.keep.setImageResource(Keep.exist(item.getName()) ? R.drawable.ic_control_keep_on : R.drawable.ic_control_keep_off);
            holder.viewBinding.getRoot().setSelected(item.isSelected());
            holder.viewBinding.getRoot().setOnClickListener(v -> onClick(holder));
            holder.viewBinding.keep.setOnClickListener(v -> onKeep(holder));
        } else {
            loadLogo(holder, item, holder.binding.logo, false);
            holder.binding.name.setText(item.getName());
            bindProgram(holder.binding.program, holder.binding.time, item);
            holder.binding.keep.setImageResource(Keep.exist(item.getName()) ? R.drawable.ic_control_keep_on : R.drawable.ic_control_keep_off);
            holder.binding.getRoot().setSelected(item.isSelected());
            // 选中高亮由整卡背景承担，频道名保持默认色（不随 selected 变主题色）
            // 使用 bindingAdapterPosition，避免刷新后位置错位
            holder.binding.getRoot().setOnClickListener(v -> onClick(holder));
            // 收藏按钮点击不触发频道点击
            holder.binding.keep.setOnClickListener(v -> onKeep(holder));
        }
    }

    // 频道当前节目 + 开始结束时间
    private void bindProgram(android.widget.TextView program, android.widget.TextView time, Channel item) {
        String text = "";
        String range = "";
        int index = item.getData().getSelected();
        if (index >= 0 && index < item.getData().getList().size()) {
            EpgData epg = item.getData().getList().get(index);
            text = epg.getTitle();
            range = epg.getStart() + "~" + epg.getEnd();
        }
        if (text.isEmpty()) text = ResUtil.getString(R.string.live_epg_none);
        program.setText(text);
        time.setText(range);
        time.setVisibility(range.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // 台标加载去重：重绑时同 URL 且已有内容则跳过（Glide into 会先清空再异步回填，重复发起会造成整表闪烁）；
    // loadLive 的 skipMemoryCache(true) 会让每次请求都重新解码，放大该问题
    private void loadLogo(ViewHolder holder, Channel item, android.widget.ImageView view, boolean big) {
        if (TextUtils.equals(item.getLogo(), holder.logoUrl) && view.getDrawable() != null) return;
        holder.logoUrl = item.getLogo();
        if (big) {
            int width = view.getResources().getDisplayMetrics().widthPixels;
            ImgUtil.loadLive(item.getLogo(), view, width, width * 3 / 4);
        } else {
            ImgUtil.loadLive(item.getLogo(), view);
        }
    }

    private void onClick(ViewHolder holder) {
        int pos = holder.getBindingAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) mListener.onItemClick(mItems.get(pos));
    }

    private void onKeep(ViewHolder holder) {
        int pos = holder.getBindingAdapterPosition();
        if (pos != RecyclerView.NO_POSITION) mListener.onKeepClick(mItems.get(pos));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterChannelLiveBinding binding;
        private final AdapterChannelLiveViewBinding viewBinding;
        String logoUrl; // 该 holder 当前已加载的台标 URL，用于重绑去重

        ViewHolder(@NonNull AdapterChannelLiveBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.viewBinding = null;
        }

        ViewHolder(@NonNull AdapterChannelLiveViewBinding binding) {
            super(binding.getRoot());
            this.binding = null;
            this.viewBinding = binding;
        }
    }
}
