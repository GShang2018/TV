package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.R;

// 评分星星：5 颗矢量星按分数精确裁剪填充。
// 不使用系统 RatingBar，因其仅对 BitmapDrawable 平铺，矢量星星会被拉伸成单颗（见 issue）
public class ScoreStars extends LinearLayout {

    private static final int STAR_COUNT = 5;

    private final ImageView[] stars = new ImageView[STAR_COUNT];

    public ScoreStars(Context context) {
        this(context, null);
    }

    public ScoreStars(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        int size = dp(14);
        for (int i = 0; i < STAR_COUNT; i++) {
            stars[i] = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i > 0) params.setMarginStart(dp(2));
            stars[i].setLayoutParams(params);
            addView(stars[i]);
        }
    }

    // 10 分制评分转 5 星显示：每颗星按比例裁剪，分数越高星星越满
    public void setScore(float score) {
        float rating = score / 2f;
        for (int i = 1; i <= STAR_COUNT; i++) {
            float fill = Math.max(0f, Math.min(1f, rating - (i - 1)));
            stars[i - 1].setImageDrawable(star(fill));
        }
    }

    private Drawable star(float fill) {
        if (fill >= 0.999f) return drawable(R.drawable.ic_star_filled);
        if (fill <= 0.001f) return drawable(R.drawable.ic_star_empty);
        ClipDrawable clip = new ClipDrawable(drawable(R.drawable.ic_star_filled), Gravity.START, ClipDrawable.HORIZONTAL);
        clip.setLevel((int) (fill * 10000));
        return new LayerDrawable(new Drawable[]{drawable(R.drawable.ic_star_empty), clip});
    }

    private Drawable drawable(int res) {
        return ContextCompat.getDrawable(getContext(), res);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
