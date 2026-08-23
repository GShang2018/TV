package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

public class CustomViewPager extends ViewPager {

    private float downX;
    private float downY;

    public CustomViewPager(@NonNull Context context) {
        super(context);
    }

    public CustomViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            downX = ev.getX();
            downY = ev.getY();
        }
        try {
            boolean intercept = super.onInterceptTouchEvent(ev);
            // 父类未拦截时，手动识别横向滑动，保证页面内容跟手移动
            if (!intercept && isHorizontalDrag(ev)) {
                return true;
            }
            return intercept;
        } catch (Exception e) {
            return isHorizontalDrag(ev);
        }
    }

    private boolean isHorizontalDrag(MotionEvent ev) {
        if (ev.getAction() != MotionEvent.ACTION_MOVE) return false;
        float dx = Math.abs(ev.getX() - downX);
        float dy = Math.abs(ev.getY() - downY);
        int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        return dx > slop && dx > dy;
    }
}
