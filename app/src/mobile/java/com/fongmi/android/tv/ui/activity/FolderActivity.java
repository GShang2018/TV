package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.databinding.ActivityFolderBinding;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.dialog.FilterDialog;
import com.fongmi.android.tv.ui.fragment.TypeFragment;

public class FolderActivity extends BaseActivity {

    private ActivityFolderBinding mBinding;

    public static void start(Activity activity, String key, Result result) {
        if (result == null || result.getTypes().isEmpty()) return;
        Intent intent = new Intent(activity, FolderActivity.class);
        intent.putExtra("key", key);
        intent.putExtra("result", result);
        activity.startActivity(intent);
    }

    private String getKey() {
        return getIntent().getStringExtra("key");
    }

    private Result getResult() {
        return getIntent().getParcelableExtra("result");
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityFolderBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        Result result = getResult();
        Class type = result.getTypes().get(0);
        mBinding.text.setText(type.getTypeName());
        // 与首页/电视端一致：filters 的 init 值作为分类请求的 extend 初值，分类页可继续筛选
        getSupportFragmentManager().beginTransaction().replace(R.id.container, TypeFragment.newInstance(getKey(), type.getTypeId(), type.getStyle(), type.getExtend(false), "1".equals(type.getTypeFlag())), "0").commitAllowingStateLoss();
        // 与电视端一致：分类带 filters 时显示筛选入口，筛选变化由 TypeFragment 刷新
        mBinding.filter.setVisibility(type.getFilters().isEmpty() ? View.GONE : View.VISIBLE);
        mBinding.filter.setOnClickListener(v -> FilterDialog.create().filter(type.getFilters()).show(getFragment()));
    }

    private TypeFragment getFragment() {
        return (TypeFragment) getSupportFragmentManager().findFragmentByTag("0");
    }

    @Override
    public void onBackPressed() {
        if (getFragment().canBack()) super.onBackPressed();
    }
}
