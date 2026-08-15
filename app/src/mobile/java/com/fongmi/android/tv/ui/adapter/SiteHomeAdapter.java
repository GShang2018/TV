package com.fongmi.android.tv.ui.adapter;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.AdapterSiteHomeBinding;

import java.util.ArrayList;
import java.util.List;

public class SiteHomeAdapter extends RecyclerView.Adapter<SiteHomeAdapter.ViewHolder> {

    private final OnClickListener listener;
    private final List<Site> mItems;
    private List<Site> allItems;

    public SiteHomeAdapter(OnClickListener listener) {
        this.listener = listener;
        this.mItems = new ArrayList<>();
        this.allItems = new ArrayList<>();
        this.addAll();
    }

    public interface OnClickListener {
        void onTextClick(Site item);
    }

    private void addAll() {
        allItems.addAll(VodConfig.get().getSites());
        mItems.addAll(allItems);
    }

    public List<Site> getItems() {
        return mItems;
    }

    public void keyword(String keyword) {
        mItems.clear();
        if (TextUtils.isEmpty(keyword)) {
            mItems.addAll(allItems);
        } else {
            String lower = keyword.toLowerCase();
            for (Site site : allItems) {
                if (site.getName().toLowerCase().contains(lower)) mItems.add(site);
            }
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
        return new ViewHolder(AdapterSiteHomeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Site item = mItems.get(position);
        boolean selected = item.isActivated();
        int primaryColor = getPrimaryColor(holder.itemView);
        int defaultColor = getDefaultTextColor(holder.itemView);

        holder.binding.text.setText(item.getName());
        holder.binding.radio.setChecked(selected);
        holder.binding.text.setSelected(selected);
        holder.binding.text.setActivated(selected);
        holder.binding.root.setActivated(selected);
        holder.binding.root.setSelected(selected);

        // 设置 radio button 颜色：选中时主题色，未选中时默认色
        int[][] states = {{android.R.attr.state_checked}, {}};
        int[] colors = {primaryColor, defaultColor};
        holder.binding.radio.setButtonTintList(new ColorStateList(states, colors));

        if (selected) {
            GradientDrawable selectedBg = new GradientDrawable();
            selectedBg.setShape(GradientDrawable.RECTANGLE);
            selectedBg.setStroke(dpToPx(1, holder.itemView), primaryColor);
            selectedBg.setCornerRadius(dpToPx(4, holder.itemView));
            RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(getColorControlHighlight(holder.itemView)),
                selectedBg, null);
            holder.binding.root.setBackground(ripple);
            holder.binding.text.setTextColor(primaryColor);
        } else {
            holder.binding.root.setBackgroundResource(R.drawable.shape_item_border);
            holder.binding.text.setTextColor(defaultColor);
        }

        holder.binding.root.setOnClickListener(v -> listener.onTextClick(item));
    }

    private int getPrimaryColor(View view) {
        TypedArray a = view.getContext().obtainStyledAttributes(new int[]{com.google.android.material.R.attr.colorPrimary});
        try {
            return a.getColor(0, Color.WHITE);
        } finally {
            a.recycle();
        }
    }

    private int getDefaultTextColor(View view) {
        TypedArray a = view.getContext().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        try {
            return a.getColor(0, Color.WHITE);
        } finally {
            a.recycle();
        }
    }

    private int getColorControlHighlight(View view) {
        TypedArray a = view.getContext().obtainStyledAttributes(new int[]{android.R.attr.colorControlHighlight});
        try {
            return a.getColor(0, 0x33FFFFFF);
        } finally {
            a.recycle();
        }
    }

    private int dpToPx(int dp, View view) {
        float density = view.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final AdapterSiteHomeBinding binding;

        ViewHolder(@NonNull AdapterSiteHomeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
