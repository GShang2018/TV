package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterEpgDateBinding;
import com.fongmi.android.tv.utils.ResUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EpgDateAdapter extends RecyclerView.Adapter<EpgDateAdapter.ViewHolder> {

    private final SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat formatWeekDay = new SimpleDateFormat("EEE", Locale.getDefault());
    private final SimpleDateFormat formatDay = new SimpleDateFormat("MM-dd", Locale.getDefault());
    private final OnClickListener mListener;
    private final List<String> mItems;
    private int mSelected;

    public EpgDateAdapter(OnClickListener listener) {
        this.mListener = listener;
        this.mItems = new ArrayList<>();
    }

    public interface OnClickListener {

        void onItemClick(String date);
    }

    public void addAll(List<String> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    public void setSelected(int position) {
        mSelected = position;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterEpgDateBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String date = mItems.get(position);
        String today = formatDate.format(new Date());
        // 今天 / 周几 + 日期
        holder.binding.week.setText(date.equals(today) ? ResUtil.getString(R.string.live_epg_today_label) : formatWeek(date));
        holder.binding.date.setText(formatDay(date));
        holder.binding.getRoot().setSelected(position == mSelected);
        // 使用 bindingAdapterPosition，避免刷新后位置错位
        holder.binding.getRoot().setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) mListener.onItemClick(mItems.get(pos));
        });
    }

    private String formatWeek(String date) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(formatDate.parse(date));
            return formatWeekDay.format(calendar.getTime());
        } catch (Exception e) {
            return date;
        }
    }

    private String formatDay(String date) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(formatDate.parse(date));
            return formatDay.format(calendar.getTime());
        } catch (Exception e) {
            return date;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterEpgDateBinding binding;

        ViewHolder(@NonNull AdapterEpgDateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
