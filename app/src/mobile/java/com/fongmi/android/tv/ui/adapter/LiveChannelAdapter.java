package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Channel;
import com.fongmi.android.tv.databinding.AdapterChannelGridBinding;
import com.fongmi.android.tv.databinding.AdapterChannelListBinding;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ImgUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 直播收藏/历史页频道列表：复用直播首页频道的横版与列表条目，副标题显示分组名
public class LiveChannelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private final OnClickListener mListener;
	private final List<Channel> mItems;
	private final Set<Integer> mChecked = new HashSet<>();
	private int[] size;
	private int viewType = ViewType.GRID;
	private boolean select;

	public LiveChannelAdapter(OnClickListener listener) {
		this.mListener = listener;
		this.mItems = new ArrayList<>();
	}

	public interface OnClickListener {

		void onItemClick(Channel item);

		void onSelectChanged(int count);
	}

	public void setSize(int[] size) {
		this.size = size;
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

	public List<Channel> getSelected() {
		List<Channel> items = new ArrayList<>();
		for (int i = 0; i < mItems.size(); i++) {
			if (mChecked.contains(i)) items.add(mItems.get(i));
		}
		return items;
	}

	public void addAll(List<Channel> items) {
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
		if (viewType == ViewType.LIST) {
			return new ListHolder(AdapterChannelListBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
		}
		return new GridHolder(AdapterChannelGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Channel item = mItems.get(position);
		if (holder instanceof ListHolder) bindList((ListHolder) holder, item, position);
		else bindGrid((GridHolder) holder, item, position);
	}

	private void bindGrid(GridHolder holder, Channel item, int position) {
		if (size != null) {
			holder.binding.image.getLayoutParams().width = size[0];
			holder.binding.image.getLayoutParams().height = size[1];
		}
		holder.binding.name.setText(item.getName());
		holder.binding.number.setVisibility(View.GONE);
		String remark = item.getGroup() == null ? "" : item.getGroup().getName();
		holder.binding.remark.setText(remark);
		holder.binding.remark.setVisibility(remark.isEmpty() ? View.GONE : View.VISIBLE);
		bindCheck(holder.binding.check, position);
		ImgUtil.loadLogo(item.getName(), item.getLogo(), holder.binding.image);
		setClickListener(holder.binding.getRoot(), position, item);
	}

	private void bindList(ListHolder holder, Channel item, int position) {
		holder.binding.name.setText(item.getName());
		holder.binding.number.setVisibility(View.GONE);
		String remark = item.getGroup() == null ? "" : item.getGroup().getName();
		holder.binding.remark.setText(remark);
		holder.binding.remark.setVisibility(remark.isEmpty() ? View.GONE : View.VISIBLE);
		bindCheck(holder.binding.check, position);
		ImgUtil.loadLogo(item.getName(), item.getLogo(), holder.binding.image);
		setClickListener(holder.binding.getRoot(), position, item);
	}

	private void bindCheck(CheckBox check, int position) {
		boolean checked = mChecked.contains(position);
		check.setVisibility(select ? View.VISIBLE : View.GONE);
		check.setChecked(checked);
	}

	private void setClickListener(View root, int position, Channel item) {
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
