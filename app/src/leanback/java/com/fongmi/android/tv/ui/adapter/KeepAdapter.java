package com.fongmi.android.tv.ui.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Keep;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.databinding.AdapterVodListBinding;
import com.fongmi.android.tv.ui.base.BaseVodHolder;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeepAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final OnClickListener mListener;
    private final List<Keep> mItems;
    private final Set<Integer> mChecked = new HashSet<>();
    private int width, height;
    private boolean select;
    private Style mStyle;

    public KeepAdapter(OnClickListener listener) {
        this(listener, Style.land());
    }

    public KeepAdapter(OnClickListener listener, Style style) {
        this.mItems = new ArrayList<>();
        this.mListener = listener;
        this.mStyle = style;
        setLayoutSize();
    }

    public void setStyle(Style style) {
        this.mStyle = style;
        setLayoutSize();
    }

    private void setLayoutSize() {
        int column = Product.getColumn(mStyle);
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (column - 1));
        int base = ResUtil.getScreenWidth() - space;
        width = base / column;
        height = (int) (width / mStyle.getRatio());
    }

    public interface OnClickListener {

        void onItemClick(Keep item);

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
        return mStyle.isList() ? ViewType.LIST : ViewType.GRID;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ViewType.LIST) {
            return new ListHolder(AdapterVodListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        ViewHolder holder = new ViewHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        holder.binding.getRoot().getLayoutParams().width = width;
        holder.binding.getRoot().getLayoutParams().height = height + ResUtil.dp2px(32);
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
            listHolder.binding.remark.setText(item.getSiteName());
            listHolder.binding.remark.setVisibility(View.VISIBLE);
            setFocusListener(listHolder.binding.getRoot());
            setClickListener(listHolder.binding.getRoot(), position, item);
            bindCheck(listHolder.binding.check, position);
            ImgUtil.loadVod(item.getVodName(), item.getVodPic(), listHolder.binding.image);
            return;
        }
        ViewHolder vh = (ViewHolder) holder;
        // 每次绑定都更新 LayoutParams，确保切换布局后封面大小正确
        vh.binding.getRoot().getLayoutParams().width = width;
        vh.binding.getRoot().getLayoutParams().height = height + ResUtil.dp2px(32);
        vh.binding.image.getLayoutParams().width = width;
        vh.binding.image.getLayoutParams().height = height;
        setFocusListener(vh.binding.getRoot());
        setClickListener(vh.itemView, position, item);
        vh.binding.name.setText(item.getVodName());
        vh.binding.remark.setVisibility(View.GONE);
        vh.binding.site.setVisibility(select ? View.GONE : View.VISIBLE);
        vh.binding.site.setText(item.getSiteName());
        bindCheck(vh.binding.check, position);
        ImgUtil.loadVod(item.getVodName(), item.getVodPic(), vh.binding.image);
        BaseVodHolder.setTagMaxWidth(vh.binding.image, 8, vh.binding.site);
    }

    private void bindCheck(CheckBox check, int position) {
        boolean checked = mChecked.contains(position);
        check.setVisibility(select ? View.VISIBLE : View.GONE);
        check.setChecked(checked);
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

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterVodBinding binding;

        public ViewHolder(@NonNull AdapterVodBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class ListHolder extends RecyclerView.ViewHolder {

        private final AdapterVodListBinding binding;

        public ListHolder(@NonNull AdapterVodListBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
