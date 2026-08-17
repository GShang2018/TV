package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.SyncDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HistoryActivity extends BaseActivity implements HistoryAdapter.OnClickListener {

    private ActivityHistoryBinding mBinding;
    private HistoryAdapter mAdapter;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HistoryActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        setRecyclerView();
        getHistory();
        updateViewIcon();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPressed());
        mBinding.sync.setOnClickListener(this::onSync);
        mBinding.view.setOnClickListener(this::toggleView);
        mBinding.delete.setOnClickListener(this::onDelete);
    }

    private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.getItemAnimator().setChangeDuration(0);
        mBinding.recycler.setAdapter(mAdapter = new HistoryAdapter(this));
        setLayout(Setting.getHistoryViewType());
    }

    private void setLayout(int viewType) {
        int column = viewType == ViewType.LIST ? Product.getListColumn(this) : (viewType == ViewType.PORTRAIT ? Product.getColumn(this) : Product.getColumn(this) - 1);
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, column));
        int space = ResUtil.dp2px(32) + ResUtil.dp2px(16 * (column - 1));
        int imageWidth = (ResUtil.getScreenWidth(this) - space) / column;
        int imageHeight = viewType == ViewType.PORTRAIT ? imageWidth * 4 / 3 : imageWidth * 3 / 4;
        mAdapter.setSize(new int[]{imageWidth, imageHeight});
        mAdapter.setViewType(viewType);
    }

    private void toggleView(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.inflate(R.menu.menu_view_type_simple);
        try {
            java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuPopup = field.get(popup);
            menuPopup.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuPopup, true);
        } catch (Exception e) {
            // ignore
        }
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            int viewType;
            if (id == R.id.view_portrait) viewType = ViewType.PORTRAIT;
            else if (id == R.id.view_grid) viewType = ViewType.GRID;
            else if (id == R.id.view_list) viewType = ViewType.LIST;
            else return false;
            Setting.putHistoryViewType(viewType);
            setLayout(viewType);
            mAdapter.notifyDataSetChanged();
            return true;
        });
        popup.show();
    }

    private void updateViewIcon() {
        mBinding.view.setImageResource(R.drawable.ic_action_view);
    }

    private void getHistory() {
        mAdapter.addAll(History.get());
        mBinding.delete.setVisibility(mAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
    }

    private void onSync(View view) {
        SyncDialog.create().history().show(this);
    }

    private void onDelete(View view) {
        if (mAdapter.isDelete()) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.dialog_delete_record).setMessage(R.string.dialog_delete_history).setNegativeButton(R.string.dialog_negative, null).setPositiveButton(R.string.dialog_positive, (dialog, which) -> mAdapter.clear()).show();
        } else if (mAdapter.getItemCount() > 0) {
            mAdapter.setDelete(true);
        } else {
            mBinding.delete.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType().equals(RefreshEvent.Type.HISTORY)) getHistory();
    }

    @Override
    public void onItemClick(History item) {
        VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public void onItemDelete(History item) {
        mAdapter.remove(item.delete());
        if (mAdapter.getItemCount() > 0) return;
        mBinding.delete.setVisibility(View.GONE);
        mAdapter.setDelete(false);
    }

    @Override
    public boolean onLongClick() {
        mAdapter.setDelete(!mAdapter.isDelete());
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.isDelete()) mAdapter.setDelete(false);
        else super.onBackPressed();
    }
}
