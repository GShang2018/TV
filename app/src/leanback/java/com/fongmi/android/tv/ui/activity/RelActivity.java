package com.fongmi.android.tv.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityRelBinding;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.presenter.VodPresenter;

import java.util.ArrayList;

public class RelActivity extends BaseActivity implements VodPresenter.OnClickListener {

    private ActivityRelBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private String mKey;
    private ArrayList<Vod> mItems;

    public static void start(Context context, String key, ArrayList<Vod> items) {
        if (items == null || items.isEmpty()) return;
        Intent intent = new Intent(context, RelActivity.class);
        intent.putExtra("key", key);
        intent.putParcelableArrayListExtra("items", items);
        context.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityRelBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mKey = getIntent().getStringExtra("key");
        mItems = getIntent().getParcelableArrayListExtra("items");
        if (mItems == null || mItems.isEmpty()) {
            finish();
            return;
        }
        mBinding.title.setText(getString(R.string.detail_rel));
        mBinding.back.setVisibility(View.VISIBLE);
        setRecyclerView();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(view -> onBackPressed());
    }

    private void setRecyclerView() {
        Style style = Style.rect();
        mBinding.grid.setLayoutManager(new GridLayoutManager(this, Product.getColumn(style)));
        mBinding.grid.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(new VodPresenter(this, style))));
        mAdapter.addAll(0, mItems);
    }

    @Override
    public void onItemClick(Vod item) {
        VideoActivity.start(this, mKey, item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        return false;
    }
}
