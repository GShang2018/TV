package com.fongmi.android.tv.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.databinding.ActivityRelBinding;
import com.fongmi.android.tv.ui.adapter.VodAdapter;
import com.fongmi.android.tv.ui.base.ViewType;
import com.fongmi.android.tv.ui.custom.ViewTypeMenu;

import java.util.ArrayList;

public class RelActivity extends AppCompatActivity implements VodAdapter.OnClickListener {

    private ActivityRelBinding mBinding;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityRelBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mKey = getIntent().getStringExtra("key");
        mItems = getIntent().getParcelableArrayListExtra("items");
        if (mItems == null || mItems.isEmpty()) {
            finish();
            return;
        }
        mBinding.title.setText(getString(R.string.detail_rel));
        mBinding.back.setOnClickListener(v -> finish());
        mBinding.viewToggle.setOnClickListener(v -> ViewTypeMenu.show(this, v, R.menu.menu_view_type, Setting.getRelViewType(), viewType -> {
            Setting.putRelViewType(viewType);
            initGrid();
        }));
        initGrid();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initGrid();
    }

    private Style getRelStyle() {
        int viewType = Setting.getRelViewType();
        if (viewType == ViewType.CONFIG) {
            // 默认：读取推荐条目的 style（与首页一致），无 style 则横版
            Style style = mItems != null && !mItems.isEmpty() ? mItems.get(0).getStyle() : null;
            return style != null ? style : Style.land();
        }
        switch (viewType) {
            case ViewType.PORTRAIT:
                return Style.rect();
            case ViewType.LIST:
                return Style.list();
            default:
                return Style.land();
        }
    }

    private void initGrid() {
        Style style = getRelStyle();
        int column = style.isList() ? Product.getListColumn(this) : Product.getColumn(this, style);
        mBinding.grid.setLayoutManager(new GridLayoutManager(this, column));
        VodAdapter adapter = new VodAdapter(this, style, style.isList() ? new int[]{0, 0} : Product.getSpec(this, style));
        mBinding.grid.setAdapter(adapter);
        adapter.addAll(mItems);
    }

    @Override
    public void onItemClick(Vod item) {
        DetailActivity.start(this, mKey, item.getVodId(), item.getVodName(), item.getVodPic());
    }

    @Override
    public boolean onLongClick(Vod item) {
        return false;
    }
}
