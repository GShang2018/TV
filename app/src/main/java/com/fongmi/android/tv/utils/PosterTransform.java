package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * 海报裁剪变换：统一映射为 3:4 竖版比例。
 * （原 4:3 横图逻辑已注释，视频播放页海报固定使用 3:4）
 * 比目标更宽的图，按裁切方式水平靠左/居中/靠右裁剪；
 * 比目标更窄/更高的图，垂直居中裁剪，不影响原图主体。
 */
public class PosterTransform extends BitmapTransformation {

    private static final float VERTICAL = 3f / 4f;   // 竖图：宽:高 = 3:4
    private static final float HORIZONTAL = 4f / 3f; // 横图：宽:高 = 4:3
    private static final String ID = "com.fongmi.android.tv.utils.PosterTransform";
    private static final byte[] ID_BYTES = ID.getBytes(Charset.forName("UTF-8"));

    private final int cropMode; // 0=靠左，1=居中，2=靠右

    public PosterTransform() {
        this(1);
    }

    public PosterTransform(int cropMode) {
        this.cropMode = cropMode;
    }

    @Override
    protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform, int outWidth, int outHeight) {
        int srcW = toTransform.getWidth();
        int srcH = toTransform.getHeight();
        float srcRatio = (float) srcW / srcH;

        // 统一使用 3:4 竖版比例（原 4:3 启用逻辑已注释）
        // float target = srcRatio >= 1f ? HORIZONTAL : VERTICAL; // 旧逻辑：横图/方图 4:3
        float target = VERTICAL;

        int cropW = srcW;
        int cropH = srcH;
        int x = 0;
        int y = 0;

        if (srcRatio >= target) {
            // 比目标更宽：按高度计算目标宽度，按裁切方式水平靠左/居中/靠右截取
            cropW = Math.min(srcW, Math.round(srcH * target));
            x = getCropX(srcW, cropW);
            y = 0;
        } else {
            // 比目标更窄/更高：按宽度计算目标高度，垂直居中裁剪
            cropH = Math.min(srcH, Math.round(srcW / target));
            x = 0;
            y = (srcH - cropH) / 2;
        }

        if (cropW == srcW && cropH == srcH) {
            return toTransform;
        }
        return Bitmap.createBitmap(toTransform, x, y, cropW, cropH);
    }

    private int getCropX(int srcW, int cropW) {
        if (cropMode == 0) return 0;          // 靠左
        if (cropMode == 2) return srcW - cropW; // 靠右
        return (srcW - cropW) / 2;            // 居中
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(ID_BYTES);
        messageDigest.update((byte) cropMode);
    }
}
