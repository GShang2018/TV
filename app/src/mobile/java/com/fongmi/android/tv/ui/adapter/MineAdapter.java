package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.CustomVod;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.databinding.ItemMineBinding;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final OnClickListener mListener;
	private final List<CustomVod> mItems;
	private final Set<Integer> mChecked = new HashSet<>();
	private int width, height;
	private int viewType;
	private boolean select;

	public MineAdapter(OnClickListener listener) {
		this.mItems = new ArrayList<>();
		this.mListener = listener;
	}

	public interface OnClickListener {

		void onItemClick(CustomVod item);

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

	public List<CustomVod> getSelected() {
		List<CustomVod> items = new ArrayList<>();
		for (int i = 0; i < mItems.size(); i++) {
			if (mChecked.contains(i)) items.add(mItems.get(i));
		}
		return items;
	}

	public void addAll(List<CustomVod> items) {
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
		if (viewType == ViewType.LIST) return new ListHolder(ItemMineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
		GridHolder holder = new GridHolder(AdapterVodBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
		holder.binding.image.getLayoutParams().width = width;
		holder.binding.image.getLayoutParams().height = height;
		return holder;
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		CustomVod item = mItems.get(position);
		if (holder instanceof ListHolder) bindList((ListHolder) holder, item, position);
		else bindGrid((GridHolder) holder, item, position);
	}

	private void bindList(ListHolder holder, CustomVod item, int position) {
		holder.binding.name.setText(item.getVodName());
		holder.binding.year.setText(item.getVodYear());
		holder.binding.year.setVisibility(TextUtils.isEmpty(item.getVodYear()) ? View.GONE : View.VISIBLE);
		holder.binding.remark.setText(item.getVodRemarks());
		holder.binding.remark.setVisibility(TextUtils.isEmpty(item.getVodRemarks()) ? View.GONE : View.VISIBLE);
		holder.binding.site.setText(getLineText(holder, item));
		bindCheck(holder.binding.check, position);
		ImgUtil.loadVod(item.getVodName(), item.getVodPic(), holder.binding.image);
		setClickListener(holder.binding.getRoot(), position, item);
	}

	private void bindGrid(GridHolder holder, CustomVod item, int position) {
		holder.binding.image.getLayoutParams().width = width;
		holder.binding.image.getLayoutParams().height = height;
		holder.binding.name.setText(item.getVodName());
		holder.binding.remark.setText(item.getVodRemarks());
		holder.binding.remark.setVisibility(TextUtils.isEmpty(item.getVodRemarks()) ? View.GONE : View.VISIBLE);
		holder.binding.site.setText(getLineText(holder, item));
		// 选择模式下隐藏左侧站点标签，避免遮挡左上角 CheckBox
		holder.binding.site.setVisibility(select ? View.GONE : (TextUtils.isEmpty(item.getVodPlayFrom()) ? View.GONE : View.VISIBLE));
		holder.binding.progress.setVisibility(View.GONE);
		bindCheck(holder.binding.check, position);
		ImgUtil.loadVod(item.getVodName(), item.getVodPic(), holder.binding.image);
		setClickListener(holder.binding.getRoot(), position, item);
	}

	private void bindCheck(CheckBox check, int position) {
		boolean checked = mChecked.contains(position);
		check.setVisibility(select ? View.VISIBLE : View.GONE);
		check.setChecked(checked);
	}

	private void setClickListener(View root, int position, CustomVod item) {
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

	private String getLineText(RecyclerView.ViewHolder holder, CustomVod item) {
		if (!item.hasPlayUrl()) return holder.itemView.getContext().getString(R.string.mine_play_url_empty);
		String from = item.getVodPlayFrom();
		if (!TextUtils.isEmpty(from)) return from.split("\\$\\$\\$")[0].trim();
		return item.getVodName();
	}

	static class ListHolder extends RecyclerView.ViewHolder {

		private final ItemMineBinding binding;

		ListHolder(@NonNull ItemMineBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}

	static class GridHolder extends RecyclerView.ViewHolder {

		private final AdapterVodBinding binding;

		GridHolder(@NonNull AdapterVodBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
