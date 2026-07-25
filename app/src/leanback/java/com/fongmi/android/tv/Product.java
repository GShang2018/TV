package com.fongmi.android.tv;

import android.content.Context;

import com.fongmi.android.tv.bean.Style;
import com.fongmi.android.tv.utils.ResUtil;

public class Product {

    public static int getDeviceType() {
        return 0;
    }

    public static int getColumn() {
        return Math.abs(Setting.getSize() - 7);
    }

    public static int getColumn(Context context) {
        return getColumn();
    }

    public static int getColumn(Style style) {
        return style.isLand() ? getColumn() - 1 : getColumn();
    }

    public static int getColumn(Context context, Style style) {
        return getColumn(style);
    }

    public static int[] getSpec(Style style) {
        int column = getColumn(style);
        int space = ResUtil.dp2px(48) + ResUtil.dp2px(16 * (column - 1));
        if (style.isOval()) space += ResUtil.dp2px(column * 16);
        return getSpec(space, column, style);
    }

    public static int[] getSpec(Context context, Style style) {
        return getSpec(style);
    }

    public static int[] getSpec(int space, int column, Style style) {
        int base = ResUtil.getScreenWidth() - space;
        int width = base / column;
        int height = (int) (width / style.getRatio());
        return new int[]{width, height};
    }

    public static int[] getSpec(Context context, int space, int column) {
        return getSpec(space, column, Style.land());
    }

    public static int getEms() {
        return Math.min(ResUtil.getScreenWidth() / ResUtil.sp2px(24), 35);
    }
}
