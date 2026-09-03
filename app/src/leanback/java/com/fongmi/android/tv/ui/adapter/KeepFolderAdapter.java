package com.fongmi.android.tv.ui.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.KeepFolder;
import com.fongmi.android.tv.databinding.AdapterKeepFolderBinding;
import com.fongmi.android.tv.utils.ImgUtil;

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
        return getSelectableCount() > 0 && mChecked.size() == getSelectableCount();
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
        return new ViewHolder(AdapterKeepFolderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        KeepFolder item = mItems.get(position);
        boolean selectable = isSelectable(position);
        boolean checked = mChecked.contains(position);
        holder.binding.name.setText(item.getName());
        holder.binding.count.setText(holder.itemView.getContext().getString(R.string.keep_folder_count, item.getCount()));
        bindCheck(holder.binding.check, select && selectable, checked);
        bindCovers(holder, item);
        setFocusListener(holder.binding.getRoot());
        holder.binding.getRoot().setOnLongClickListener(view -> {
            if (!select && selectable) {
                setSelect(true);
                setChecked(position, true);
            }
            return true;
        });
        holder.binding.getRoot().setOnClickListener(view -> {
            if (select && selectable) setChecked(position, !isChecked(position));
            else mListener.onItemClick(item);
        });
    }

    private void bindCheck(CheckBox check, boolean show, boolean checked) {
        check.setVisibility(show ? View.VISIBLE : View.GONE);
        check.setChecked(checked);
    }

    private void bindCovers(ViewHolder holder, KeepFolder item) {
        List<Keep> keeps = Keep.getVod(item.getId());
        if (keeps.isEmpty()) {
            // 无视频：三个封面都显示收藏夹首字 + 随机背景色，保持堆叠
            ImgUtil.loadPoster(item.getName(), "", holder.binding.cover1);
            ImgUtil.loadPoster(item.getName(), "", holder.binding.cover2);
            ImgUtil.loadPoster(item.getName(), "", holder.binding.cover3);
            holder.binding.cover2.setVisibility(View.VISIBLE);
            holder.binding.cover3.setVisibility(View.VISIBLE);
        } else {
            ImgUtil.loadPoster(keeps.get(0).getVodName(), keeps.get(0).getVodPic(), holder.binding.cover1);
            holder.binding.cover2.setVisibility(keeps.size() > 1 ? View.VISIBLE : View.GONE);
            holder.binding.cover3.setVisibility(keeps.size() > 2 ? View.VISIBLE : View.GONE);
            if (keeps.size() > 1) ImgUtil.loadPoster(keeps.get(1).getVodName(), keeps.get(1).getVodPic(), holder.binding.cover2);
            if (keeps.size() > 2) ImgUtil.loadPoster(keeps.get(2).getVodName(), keeps.get(2).getVodPic(), holder.binding.cover3);
        }
    }

    private void setFocusListener(View root) {
        root.setOnFocusChangeListener((v, hasFocus) -> {
            v.setSelected(hasFocus);
            if (hasFocus) {
                AnimatorSet animator = new AnimatorSet();
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.0f, 1.02f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.0f, 1.02f);
                animator.setDuration(200);
                animator.playTogether(scaleX, scaleY);
                animator.start();
                v.bringToFront();
            } else {
                AnimatorSet animator = new AnimatorSet();
                ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1.02f, 1.0f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1.02f, 1.0f);
                animator.setDuration(200);
                animator.playTogether(scaleX, scaleY);
                animator.start();
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterKeepFolderBinding binding;

        ViewHolder(@NonNull AdapterKeepFolderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
