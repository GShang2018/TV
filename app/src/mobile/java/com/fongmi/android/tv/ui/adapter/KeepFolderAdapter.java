package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.databinding.AdapterKeepFolderBinding;
import com.fongmi.android.tv.databinding.AdapterVodRectBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeepFolderAdapter extends RecyclerView.Adapter<KeepFolderAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<KeepFolder> mItems;
    private final Set<Integer> mChecked = new HashSet<>();
    private boolean select;

    public KeepFolderAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Keep item);

        void onItemClick(KeepFolder item);

        void onSelectChanged(int count);
    }

    public boolean isSelect() {
        return select;
    }

    public void setSelect(boolean select) {
        this.select = select;
        if (!select) mChecked.clear();
        notifyItemRangeChanged(0, mItems.size());
        mListener.onSelectChanged(mChecked.size());
    }

    public boolean isChecked(int position) {
        return mChecked.contains(position);
    }

    private boolean isSelectable(int position) {
        return position >= 0 && position < mItems.size() && mItems.get(position).getId() != 0;
    }

    public void setChecked(int position, boolean checked) {
        if (!isSelectable(position)) return;
        if (checked) mChecked.add(position);
        else mChecked.remove(position);
        notifyItemChanged(position);
        mListener.onSelectChanged(mChecked.size());
    }

    public boolean isAllChecked() {
        int selectable = getSelectableCount();
        return selectable > 0 && mChecked.size() == selectable;
    }

    public void setAll(boolean checked) {
        if (checked) {
            for (int i = 0; i < mItems.size(); i++) {
                if (isSelectable(i)) mChecked.add(i);
            }
        } else {
            mChecked.clear();
        }
        notifyDataSetChanged();
        mListener.onSelectChanged(mChecked.size());
    }

    private int getSelectableCount() {
        int count = 0;
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i).getId() != 0) count++;
        }
        return count;
    }

    public int getSelectCount() {
        return mChecked.size();
    }

    public List<KeepFolder> getSelected() {
        List<KeepFolder> items = new ArrayList<>();
        for (int i = 0; i < mItems.size(); i++) {
            if (mChecked.contains(i)) items.add(mItems.get(i));
        }
        return items;
    }

    public void addAll(List<KeepFolder> items) {
        mItems.clear();
        mItems.addAll(items);
        mChecked.clear();
        if (select) {
            select = false;
            mListener.onSelectChanged(0);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterKeepFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), new VodAdapter(this));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KeepFolder item = mItems.get(position);
        boolean selectable = isSelectable(position);
        boolean checked = mChecked.contains(position);
        holder.binding.name.setText(item.getName());
        holder.binding.all.setText(holder.itemView.getContext().getString(R.string.keep_folder_all, item.getCount()));
        // 选择模式：可删除的收藏夹隐藏“全部”，显示勾选圆圈；默认收藏夹保持原样
        holder.binding.all.setVisibility(select && selectable ? View.GONE : View.VISIBLE);
        bindCheck(holder.binding.check, select && selectable, checked);
        holder.vodAdapter.setData(item);
        holder.binding.all.setOnClickListener(view -> {
            if (select && selectable) setChecked(position, !isChecked(position));
            else mListener.onItemClick(item);
        });
        holder.binding.getRoot().setOnLongClickListener(view -> {
            if (!select && selectable) {
                setSelect(true);
                setChecked(position, true);
            }
            return true;
        });
        holder.binding.getRoot().setOnClickListener(view -> {
            if (select && selectable) setChecked(position, !isChecked(position));
            else if (!select) mListener.onItemClick(item);
        });
    }

    private void bindCheck(CheckBox check, boolean show, boolean checked) {
        check.setVisibility(show ? View.VISIBLE : View.GONE);
        check.setChecked(checked);
    }

    private void onPosterClick(KeepFolder folder, Keep item) {
        int position = mItems.indexOf(folder);
        if (select && isSelectable(position)) setChecked(position, !isChecked(position));
        else mListener.onItemClick(item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterKeepFolderBinding binding;
        private final VodAdapter vodAdapter;

        ViewHolder(@NonNull AdapterKeepFolderBinding binding, VodAdapter vodAdapter) {
            super(binding.getRoot());
            this.binding = binding;
            this.vodAdapter = vodAdapter;
            this.binding.recycler.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext(), LinearLayoutManager.HORIZONTAL, false));
            this.binding.recycler.setAdapter(vodAdapter);
        }
    }

    private static class VodAdapter extends RecyclerView.Adapter<VodAdapter.VodHolder> {

        private final List<Keep> mItems = new ArrayList<>();
        private final KeepFolderAdapter mAdapter;
        private KeepFolder mFolder;

        VodAdapter(KeepFolderAdapter adapter) {
            this.mAdapter = adapter;
        }

        void setData(KeepFolder folder) {
            mFolder = folder;
            mItems.clear();
            List<Keep> items = Keep.getVod(folder.getId());
            if (items.size() > 10) items = items.subList(0, 10);
            mItems.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VodHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            VodHolder holder = new VodHolder(AdapterVodRectBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            // 3:4 竖版封面尺寸
            int width = ResUtil.dp2px(90);
            int height = ResUtil.dp2px(120);
            holder.binding.image.getLayoutParams().width = width;
            holder.binding.image.getLayoutParams().height = height;
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull VodHolder holder, int position) {
            Keep item = mItems.get(position);
            holder.binding.name.setText(item.getVodName());
            holder.binding.site.setText(item.getSiteName());
            holder.binding.site.setVisibility(View.GONE);
            holder.binding.year.setVisibility(View.GONE);
            holder.binding.remark.setVisibility(View.GONE);
            ImgUtil.loadPoster(item.getVodName(), item.getVodPic(), holder.binding.image);
            holder.binding.getRoot().setOnClickListener(view -> mAdapter.onPosterClick(mFolder, item));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        static class VodHolder extends RecyclerView.ViewHolder {

            private final AdapterVodRectBinding binding;

            VodHolder(@NonNull AdapterVodRectBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
