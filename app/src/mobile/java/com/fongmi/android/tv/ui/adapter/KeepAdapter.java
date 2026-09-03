package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.databinding.AdapterVodListBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeepAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Keep> mItems;
    private final Set<Integer> mChecked = new HashSet<>();
    private int width, height;
    private int viewType = ViewType.GRID;
    private boolean select;

    public KeepAdapter(OnClickListener listener) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
    }

    public interface OnClickListener {

        void onItemClick(Keep item);

        void onSelectChanged(int count);
    }

    public void setSize(int[] size) {
        this.width = size[0];
        this.height = size[1];
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
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

    public void setChecked(int position, boolean checked) {
        if (checked) mChecked.add(position);
        else mChecked.remove(position);
        notifyItemChanged(position);
        mListener.onSelectChanged(mChecked.size());
    }

    public boolean isAllChecked() {
        return mItems.size() > 0 && mChecked.size() == mItems.size();
    }

    public void setAll(boolean checked) {
        if (checked) {
            for (int i = 0; i < mItems.size(); i++) mChecked.add(i);
        } else {
            mChecked.clear();
        }
        notifyDataSetChanged();
        mListener.onSelectChanged(mChecked.size());
    }

    public int getSelectCount() {
        return mChecked.size();
    }

    public List<Keep> getSelected() {
        List<Keep> items = new ArrayList<>();
        for (int i = 0; i < mItems.size(); i++) {
            if (mChecked.contains(i)) items.add(mItems.get(i));
        }
        return items;
    }

    public void addAll(List<Keep> items) {
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

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.LIST) {
            return new ListHolder(AdapterVodListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.image.getLayoutParams().width = width;
        holder.binding.image.getLayoutParams().height = height;
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Keep item = mItems.get(position);
        if (holder instanceof ListHolder) {
            ListHolder listHolder = (ListHolder) holder;
            listHolder.binding.name.setText(item.getVodName());
            listHolder.binding.site.setText(item.getSiteName());
            listHolder.binding.site.setVisibility(View.VISIBLE);
            listHolder.binding.remark.setText(item.getSiteName());
            bindCheck(listHolder.binding.check, position);
            ImgUtil.loadPoster(item.getVodName(), item.getVodPic(), listHolder.binding.image);
            setClickListener(listHolder.binding.getRoot(), position, item);
        } else {
            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.binding.image.getLayoutParams().width = width;
            viewHolder.binding.image.getLayoutParams().height = height;
            viewHolder.binding.name.setText(item.getVodName());
            viewHolder.binding.remark.setVisibility(View.GONE);
            viewHolder.binding.site.setVisibility(select ? View.GONE : View.VISIBLE);
            viewHolder.binding.site.setText(item.getSiteName());
            bindCheck(viewHolder.binding.check, position);
            ImgUtil.loadVod(item.getVodName(), item.getVodPic(), viewHolder.binding.image);
            BaseVodHolder.setTagMaxWidth(viewHolder.binding.image, 12, viewHolder.binding.site);
            setClickListener(viewHolder.binding.getRoot(), position, item);
        }
    }

    private void bindCheck(CheckBox check, int position) {
        boolean checked = mChecked.contains(position);
        check.setVisibility(select ? View.VISIBLE : View.GONE);
        check.setChecked(checked);
    }

    private void setClickListener(View root, int position, Keep item) {
        root.setOnLongClickListener(view -> {
            if (!select) {
                setSelect(true);
                setChecked(position, true);
            }
            return true;
        });
        root.setOnClickListener(view -> {
            if (select) setChecked(position, !isChecked(position));
            else mListener.onItemClick(item);
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterVodBinding binding;

        ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ListHolder extends RecyclerView.ViewHolder {

        private final AdapterVodListBinding binding;

        ListHolder(@NonNull AdapterVodListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
