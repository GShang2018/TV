package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.CustomVod;
import com.fongmi.android.tv.databinding.ActivityMineEditBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Notify;

public class MineEditActivity extends BaseActivity {

    private ActivityMineEditBinding mBinding;
    private CustomVod mItem;
    private int mId;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, MineEditActivity.class));
    }

    public static void start(Activity activity, int id) {
        Intent intent = new Intent(activity, MineEditActivity.class);
        intent.putExtra("id", id);
        activity.startActivity(intent);
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityMineEditBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        mId = getIntent().getIntExtra("id", 0);
        if (mId > 0) {
            for (CustomVod vod : CustomVod.getAll()) {
                if (vod.getId() == mId) {
                    mItem = vod;
                    break;
                }
            }
        }
        mBinding.title.setText(mItem == null ? R.string.mine_add : R.string.mine_edit);
        setData();
    }

    @Override
    protected void initEvent() {
        mBinding.back.setOnClickListener(view -> onBackPressed());
        mBinding.save.setOnClickListener(view -> save());
    }

    private void setData() {
        if (mItem == null) return;
        mBinding.etVodName.setText(mItem.getVodName());
        mBinding.etVodId.setText(mItem.getVodId());
        mBinding.etTypeName.setText(mItem.getTypeName());
        mBinding.etVodYear.setText(mItem.getVodYear());
        mBinding.etVodArea.setText(mItem.getVodArea());
        mBinding.etVodRemarks.setText(mItem.getVodRemarks());
        mBinding.etVodTag.setText(mItem.getVodTag());
        mBinding.etVodDirector.setText(mItem.getVodDirector());
        mBinding.etVodActor.setText(mItem.getVodActor());
        mBinding.etVodContent.setText(mItem.getVodContent());
        mBinding.etVodPic.setText(mItem.getVodPic());
        mBinding.etVodPlayFrom.setText(mItem.getVodPlayFrom());
        mBinding.etVodPlayUrl.setText(mItem.getVodPlayUrl());
        mBinding.etVodScore.setText(mItem.getVodScore());
        mBinding.etVodLang.setText(mItem.getVodLang());
        mBinding.etVodTv.setText(mItem.getVodTv());
        mBinding.etVodClass.setText(mItem.getVodClass());
        mBinding.etVodPubdate.setText(mItem.getVodPubdate());
        mBinding.etVodDuration.setText(mItem.getVodDuration());
        mBinding.etVodAuthor.setText(mItem.getVodAuthor());
        mBinding.etVodPicThumb.setText(mItem.getVodPicThumb());
        mBinding.etVodPicSlide.setText(mItem.getVodPicSlide());
        mBinding.etVodPicScreenshot.setText(mItem.getVodPicScreenshot());
    }

    private void save() {
        String name = mBinding.etVodName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Notify.show(R.string.mine_name_required);
            return;
        }
        CustomVod item = mItem == null ? new CustomVod() : mItem;
        item.setVodName(name);
        item.setVodId(mBinding.etVodId.getText().toString().trim());
        item.setTypeName(mBinding.etTypeName.getText().toString().trim());
        item.setVodYear(mBinding.etVodYear.getText().toString().trim());
        item.setVodArea(mBinding.etVodArea.getText().toString().trim());
        item.setVodRemarks(mBinding.etVodRemarks.getText().toString().trim());
        item.setVodTag(mBinding.etVodTag.getText().toString().trim());
        item.setVodDirector(mBinding.etVodDirector.getText().toString().trim());
        item.setVodActor(mBinding.etVodActor.getText().toString().trim());
        item.setVodContent(mBinding.etVodContent.getText().toString().trim());
        item.setVodPic(mBinding.etVodPic.getText().toString().trim());
        item.setVodPlayFrom(mBinding.etVodPlayFrom.getText().toString().trim());
        item.setVodPlayUrl(mBinding.etVodPlayUrl.getText().toString().trim());
        item.setVodScore(mBinding.etVodScore.getText().toString().trim());
        item.setVodLang(mBinding.etVodLang.getText().toString().trim());
        item.setVodTv(mBinding.etVodTv.getText().toString().trim());
        item.setVodClass(mBinding.etVodClass.getText().toString().trim());
        item.setVodPubdate(mBinding.etVodPubdate.getText().toString().trim());
        item.setVodDuration(mBinding.etVodDuration.getText().toString().trim());
        item.setVodAuthor(mBinding.etVodAuthor.getText().toString().trim());
        item.setVodPicThumb(mBinding.etVodPicThumb.getText().toString().trim());
        item.setVodPicSlide(mBinding.etVodPicSlide.getText().toString().trim());
        item.setVodPicScreenshot(mBinding.etVodPicScreenshot.getText().toString().trim());
        item.save();
        RefreshEvent.mine();
        Notify.show(R.string.mine_saved);
        finish();
    }
}
