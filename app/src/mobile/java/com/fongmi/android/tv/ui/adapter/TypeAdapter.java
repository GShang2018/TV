package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.AdapterTypeBinding;
import com.fongmi.android.tv.databinding.AdapterTypeDialogBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public class TypeAdapter extends RecyclerView.Adapter<TypeAdapter.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Class> mItems;
    private final boolean dialog;

    public TypeAdapter(OnClickListener listener) {
        this(listener, false);
    }

    public TypeAdapter(OnClickListener listener, boolean dialog) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
        this.dialog = dialog;
    }

    public interface OnClickListener {

        void onItemClick(int position, Class item);
    }

    private Class home() {
        Class type = new Class();
        type.setTypeName(ResUtil.getString(R.string.vod_home));
        type.setTypeId("home");
        return type;
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    public void addAll(Result result) {
        mItems.addAll(result.getTypes());
        if (result.getList().size() > 0) mItems.add(0, home());
        if (mItems.size() > 0) mItems.get(0).setActivated(true);
        notifyDataSetChanged();
    }

    public void addAll(List<Class> items) {
        mItems.clear();
        mItems.addAll(items);
        if (mItems.size() > 0) mItems.get(0).setActivated(true);
        notifyDataSetChanged();
    }

    public List<Class> getItems() {
        return mItems;
    }

    public void setActivated(int position) {
        for (Class item : mItems) item.setActivated(false);
        mItems.get(position).setActivated(true);
        notifyItemRangeChanged(0, mItems.size());
    }

    public Class get(int position) {
        return mItems.get(position);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (dialog) {
            AdapterTypeDialogBinding b = AdapterTypeDialogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(b.getRoot(), b.text);
        }
        AdapterTypeBinding b = AdapterTypeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(b.getRoot(), b.text);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Class item = mItems.get(position);
        holder.text.setText(item.getTypeName());
        holder.text.setActivated(item.isActivated());
        holder.text.setOnClickListener(v -> mListener.onItemClick(position, item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final android.widget.TextView text;

        ViewHolder(@NonNull android.view.View root, android.widget.TextView text) {
            super(root);
            this.text = text;
        }
    }
}