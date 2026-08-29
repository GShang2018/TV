package com.fongmi.android.tv.ui.adapter;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Value;
import com.fongmi.android.tv.databinding.AdapterFilterBinding;
import com.fongmi.android.tv.impl.FilterCallback;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.flexbox.FlexboxLayout;

import java.util.List;

// 分类筛选行：左侧筛选名，右侧流式标签（FlexboxLayout 自动换行），整行随外层列表纵向滚动
public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {

    private final FilterCallback mListener;
    private final List<Filter> mItems;

    public FilterAdapter(FilterCallback listener, List<Filter> items) {
        this.mListener = listener;
        this.mItems = items;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterFilterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Filter item = mItems.get(position);
        holder.binding.name.setText(item.getName());
        // 复用行时先清空旧标签再重建，避免选中态错乱
        holder.binding.flow.removeAllViews();
        for (Value value : item.getValue()) holder.binding.flow.addView(createValue(holder, item, value));
    }

    private TextView createValue(ViewHolder holder, Filter filter, Value value) {
        TextView text = new TextView(holder.binding.flow.getContext());
        text.setText(value.getN());
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        text.setTextColor(AppCompatResources.getColorStateList(text.getContext(), R.color.filter_value_text));
        text.setBackgroundResource(R.drawable.selector_filter_value);
        text.setActivated(value.isActivated());
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = ResUtil.dp2px(8);
        params.setMargins(0, 0, margin, margin);
        text.setLayoutParams(params);
        text.setOnClickListener(v -> onItemClick(holder, filter, value));
        return text;
    }

    private void onItemClick(ViewHolder holder, Filter filter, Value value) {
        for (Value item : filter.getValue()) item.setActivated(value);
        int position = holder.getBindingAdapterPosition();
        if (position != RecyclerView.NO_POSITION) notifyItemChanged(position);
        mListener.setFilter(filter.getKey(), value);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterFilterBinding binding;

        ViewHolder(@NonNull AdapterFilterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
