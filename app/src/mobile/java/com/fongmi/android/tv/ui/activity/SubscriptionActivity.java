package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySubscribeBinding;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.fragment.SubscribeFragment;

public class SubscriptionActivity extends BaseActivity {

    private ActivitySubscribeBinding mBinding;
    private SubscribeFragment[] mFragments;

    public static void start(Activity activity) {
        start(activity, 0, false);
    }

    public static void start(Activity activity, int type, boolean select) {
        Intent intent = new Intent(activity, SubscriptionActivity.class);
        intent.putExtra("type", type);
        intent.putExtra("select", select);
        activity.startActivity(intent);
    }

    public boolean isSelect() {
        return getIntent().getBooleanExtra("select", false);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySubscribeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        int type = getIntent().getIntExtra("type", 0);
        mFragments = new SubscribeFragment[]{SubscribeFragment.newInstance(0), SubscribeFragment.newInstance(1)};
        mBinding.pager.setAdapter(new PageAdapter());
        mBinding.pager.setUserInputEnabled(false);
        mBinding.pager.setCurrentItem(type, false);
        mBinding.title.setText(type == 0 ? R.string.setting_subscribe_vod : R.string.setting_subscribe_live);
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(v -> onBackPress());
        mBinding.add.setOnClickListener(v -> mFragments[mBinding.pager.getCurrentItem()].onAdd());
    }

    @Override
    protected boolean handleBack() {
        return true;
    }

    @Override
    protected void onBackPress() {
        finish();
    }

    class PageAdapter extends FragmentStateAdapter {

        PageAdapter() {
            super(SubscriptionActivity.this);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return mFragments[position];
        }

        @Override
        public int getItemCount() {
            return mFragments.length;
        }
    }
}
