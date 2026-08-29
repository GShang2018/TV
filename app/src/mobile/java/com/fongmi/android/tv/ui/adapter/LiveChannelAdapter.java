package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.databinding.AdapterChannelGridBinding;
import com.fongmi.android.tv.databinding.AdapterChannelListBinding;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.List;

// 直播收藏/历史页频道列表：复用直播首页频道的横版与列表条目，副标题显示分组名
public class LiveChannelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final OnClickListener mListener;
	private final List<Channel> mItems;
	private int[] size;
	private int viewType = ViewType.GRID;
	private boolean delete;

	public LiveChannelAdapter(OnClickListener listener) {
		this.mListener = listener;
		this.mItems = new ArrayList<>();
	}

	public interface OnClickListener {

		void onItemClick(Channel item);

		void onItemDelete(Channel item);

		boolean onLongClick();
	}

	public void setSize(int[] size) {
		this.size = size;
	}

	public void setViewType(int viewType) {
		this.viewType = viewType;
	}

	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
		notifyItemRangeChanged(0, mItems.size());
	}

	public void addAll(List<Channel> items) {
		mItems.clear();
		mItems.addAll(items);
		notifyDataSetChanged();
	}

	public void clear() {
		mItems.clear();
		setDelete(false);
		notifyDataSetChanged();
	}

	public void remove(Channel item) {
		int index = mItems.indexOf(item);
		if (index == -1) return;
		mItems.remove(index);
		notifyItemRemoved(index);
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
			return new ListHolder(AdapterChannelListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
		}
		return new GridHolder(AdapterChannelGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Channel item = mItems.get(position);
		if (holder instanceof ListHolder) bindList((ListHolder) holder, item);
		else bindGrid((GridHolder) holder, item);
	}

	private void bindGrid(GridHolder holder, Channel item) {
		if (size != null) {
			holder.binding.image.getLayoutParams().width = size[0];
			holder.binding.image.getLayoutParams().height = size[1];
		}
		holder.binding.name.setText(item.getName());
		holder.binding.number.setVisibility(View.GONE);
		// 删除模式下隐藏副标题、显示右上角删除图标，与点播收藏/历史页一致
		holder.binding.remark.setVisibility(delete ? View.GONE : View.VISIBLE);
		holder.binding.delete.setVisibility(delete ? View.VISIBLE : View.GONE);
		String remark = item.getGroup() == null ? "" : item.getGroup().getName();
		holder.binding.remark.setText(remark);
		if (remark.isEmpty()) holder.binding.remark.setVisibility(View.GONE);
		ImgUtil.loadLogo(item.getName(), item.getLogo(), holder.binding.image);
		setClickListener(holder.binding.getRoot(), item);
	}

	private void bindList(ListHolder holder, Channel item) {
		holder.binding.name.setText(item.getName());
		holder.binding.number.setVisibility(View.GONE);
		holder.binding.remark.setVisibility(delete ? View.GONE : View.VISIBLE);
		holder.binding.delete.setVisibility(delete ? View.VISIBLE : View.GONE);
		String remark = item.getGroup() == null ? "" : item.getGroup().getName();
		holder.binding.remark.setText(remark);
		if (remark.isEmpty()) holder.binding.remark.setVisibility(View.GONE);
		ImgUtil.loadLogo(item.getName(), item.getLogo(), holder.binding.image);
		setClickListener(holder.binding.getRoot(), item);
	}

	private void setClickListener(View root, Channel item) {
		root.setOnLongClickListener(view -> mListener.onLongClick());
		root.setOnClickListener(view -> {
			if (isDelete()) mListener.onItemDelete(item);
			else mListener.onItemClick(item);
		});
	}

	static class GridHolder extends RecyclerView.ViewHolder {

		private final AdapterChannelGridBinding binding;

		GridHolder(@NonNull AdapterChannelGridBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}

	static class ListHolder extends RecyclerView.ViewHolder {

		private final AdapterChannelListBinding binding;

		ListHolder(@NonNull AdapterChannelListBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}
}
