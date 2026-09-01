package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.ItemPersonRowBinding;
import com.fongmi.android.tv.ui.adapter.PersonAdapter.Person;

import java.util.ArrayList;
import java.util.List;

public class PersonRowAdapter extends RecyclerView.Adapter<PersonRowAdapter.ViewHolder> {

    private final List<Person> mItems;
    private final OnClickListener mListener;

    public PersonRowAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(Result result);
    }

    public void addAll(List<Person> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonRowBinding binding = ItemPersonRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Person person = mItems.get(position);
        holder.binding.avatar.setText(getInitial(person.name));
        holder.binding.name.setText(person.name);
        if (person.result != null) {
            holder.binding.getRoot().setClickable(true);
            holder.binding.getRoot().setOnClickListener(v -> mListener.onItemClick(person.result));
        } else {
            holder.binding.getRoot().setClickable(false);
            holder.binding.getRoot().setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    private String getInitial(String name) {
        if (name == null || name.isEmpty()) return "?";
        return String.valueOf(name.charAt(0));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPersonRowBinding binding;

        ViewHolder(@NonNull ItemPersonRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
