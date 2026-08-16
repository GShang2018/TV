package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.ItemPersonBinding;

import java.util.ArrayList;
import java.util.List;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.ViewHolder> {

    private final List<Person> mItems = new ArrayList<>();
    private final OnClickListener mListener;

    public PersonAdapter(OnClickListener listener) {
        this.mListener = listener;
    }

    public void setItems(List<Person> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonBinding binding = ItemPersonBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
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

    public interface OnClickListener {
        void onItemClick(Result result);
    }

    public static class Person {
        public final String name;
        public final Result result;

        public Person(String name, Result result) {
            this.name = name;
            this.result = result;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPersonBinding binding;

        ViewHolder(ItemPersonBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
