package com.fongmi.android.tv.ui.base;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.fongmi.android.tv.bean.Vod;

public abstract class BaseVodHolder extends Presenter.ViewHolder {

    public BaseVodHolder(View view) {
        super(view);
    }

    public abstract void initView(Vod item);

    public abstract void setSize(int[] size);

    public static void setTagMaxWidth(View imageView, int paddingDp, TextView... tags) {
        imageView.post(() -> {
            int imageWidth = imageView.getWidth();
            if (imageWidth <= 0) {
                imageWidth = imageView.getLayoutParams().width;
            }
            if (imageWidth > 0) {
                float density = Resources.getSystem().getDisplayMetrics().density;
                int paddingPx = (int) (paddingDp * density + 0.5f);
                int maxWidth = imageWidth - paddingPx;
                for (TextView tag : tags) {
                    if (tag != null) {
                        tag.setMaxWidth(maxWidth);
                    }
                }
            }
        });
    }
}
