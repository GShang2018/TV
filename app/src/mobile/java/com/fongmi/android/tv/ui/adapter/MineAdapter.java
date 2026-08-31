package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.CustomVod;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.databinding.ItemMineBinding;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

public class MineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final OnClickListener mListener;
	private final List<CustomVod> mItems;
	private int width, height;
	private int viewType;
	// 编辑模式：点击=修改、长按=删除；预览模式：纯浏览播放
	private boolean edit;

	public MineAdapter(OnClickListener listener) {
		this.mItems = new ArrayList<>();
		this.mListener = listener;
	}

	public interface OnClickListener {

		void onItemClick(CustomVod item);

		void onEdit(CustomVod item);

		void onItemDelete(CustomVod item);
	}

	public boolean isEdit() {
		return edit;
	}

	public void setEdit(boolean edit) {
		this.edit = edit;
		notifyItemRangeChanged(0, mItems.size());
	}

	public void setSize(int[] size) {
		this.width = size[0];
		this.height = size[1];
	}

	public void setViewType(int viewType) {
		this.viewType = viewType;
	}

	public void addAll(List<CustomVod> items) {
		mItems.clear();
		mItems.addAll(items);
		notifyDataSetChanged();
	}

	public void remove(CustomVod item) {
		int index = mItems.indexOf(item);
		if (index == -1) return;
		mItems.remove(index);
		notifyItemRemoved(index);
	}

	public void clear() {
		mItems.clear();
		setEdit(false);
		notifyDataSetChanged();
		CustomVod.deleteAll();
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
		if (holder instanceof ListHolder) bindList((ListHolder) holder, item);
		else bindGrid((GridHolder) holder, item);
	}

	private void bindList(ListHolder holder, CustomVod item) {
		holder.binding.name.setText(item.getVodName());
		holder.binding.year.setText(item.getVodYear());
		holder.binding.year.setVisibility(TextUtils.isEmpty(item.getVodYear()) ? View.GONE : View.VISIBLE);
		holder.binding.remark.setText(item.getVodRemarks());
		holder.binding.remark.setVisibility(TextUtils.isEmpty(item.getVodRemarks()) ? View.GONE : View.VISIBLE);
		holder.binding.site.setText(getLineText(holder, item));
		ImgUtil.loadVod(item.getVodName(), item.getVodPic(), holder.binding.image);
		holder.binding.getRoot().setOnClickListener(view -> {
			if (edit) mListener.onEdit(item);
			else mListener.onItemClick(item);
		});
		holder.binding.getRoot().setOnLongClickListener(view -> {
			if (!edit) return false;
			mListener.onItemDelete(item);
			return true;
		});
	}

	private void bindGrid(GridHolder holder, CustomVod item) {
		holder.binding.image.getLayoutParams().width = width;
		holder.binding.image.getLayoutParams().height = height;
		holder.binding.name.setText(item.getVodName());
		holder.binding.remark.setText(item.getVodRemarks());
		holder.binding.remark.setVisibility(TextUtils.isEmpty(item.getVodRemarks()) ? View.GONE : View.VISIBLE);
		holder.binding.site.setText(getLineText(holder, item));
		holder.binding.site.setVisibility(TextUtils.isEmpty(item.getVodPlayFrom()) ? View.GONE : View.VISIBLE);
		holder.binding.progress.setVisibility(View.GONE);
		holder.binding.delete.setVisibility(View.GONE);
		ImgUtil.loadVod(item.getVodName(), item.getVodPic(), holder.binding.image);
		holder.binding.getRoot().setOnClickListener(view -> {
			if (edit) mListener.onEdit(item);
			else mListener.onItemClick(item);
		});
		holder.binding.getRoot().setOnLongClickListener(view -> {
			if (!edit) return false;
			mListener.onItemDelete(item);
			return true;
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
