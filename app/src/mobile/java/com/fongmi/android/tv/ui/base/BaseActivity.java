package com.fongmi.android.tv.ui.base;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.utils.FileUtil;
import com.fongmi.android.tv.utils.ResUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

public abstract class BaseActivity extends AppCompatActivity {

    protected abstract ViewBinding getBinding();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        if (transparent()) setTransparent(this);
        setContentView(getBinding().getRoot());
        EventBus.getDefault().register(this);
        initView(savedInstanceState);
        setBackCallback();
        initEvent();
    }

    private void applyTheme() {
        String themeColor = Setting.getThemeColor();
        if (themeColor == null) themeColor = "green";
        int themeResId = getThemeStyleResId(themeColor);
        setTheme(themeResId);
    }

    private int getThemeStyleResId(String color) {
        switch (color) {
            case "blue":    return R.style.AppTheme_Blue;
            case "red":     return R.style.AppTheme_Red;
            case "purple":  return R.style.AppTheme_Purple;
            case "orange":  return R.style.AppTheme_Orange;
            case "teal":    return R.style.AppTheme_Teal;
            case "pink":    return R.style.AppTheme_Pink;
            default:        return R.style.AppTheme_Green;
        }
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        refreshWall();
    }

    protected Activity getActivity() {
        return this;
    }

    protected boolean transparent() {
        return true;
    }

    protected boolean customWall() {
        return true;
    }

    protected boolean handleBack() {
        return false;
    }

    protected void initView(Bundle savedInstanceState) {
    }

    protected void initEvent() {
    }

    protected void onBackPress() {
    }

    protected boolean isVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    protected boolean isGone(View view) {
        return view.getVisibility() == View.GONE;
    }

    protected void setPadding(ViewGroup layout) {
        setPadding(layout, false);
    }

    protected void setPadding(ViewGroup layout, boolean leftOnly) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return;
        DisplayCutout cutout = ResUtil.getDisplay(this).getCutout();
        if (cutout == null) return;
        int top = cutout.getSafeInsetTop();
        int left = cutout.getSafeInsetLeft();
        int right = cutout.getSafeInsetRight();
        int bottom = cutout.getSafeInsetBottom();
        int padding = left | right | top | bottom;
        layout.setPadding(padding, 0, leftOnly ? 0 : padding, 0);
    }

    protected void noPadding(ViewGroup layout) {
        layout.setPadding(0, 0, 0, 0);
    }

    private void setBackCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(handleBack()) {
            @Override
            public void handleOnBackPressed() {
                onBackPress();
            }
        });
    }

    private void setTransparent(Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    private void refreshWall() {
        try {
            if (!customWall()) return;
            File file = FileUtil.getWall(Setting.getWall());
            if (file.exists() && file.length() > 0) {
                applyWallpaperWithOverlay(file);
            } else {
                getWindow().setBackgroundDrawableResource(ResUtil.getDrawable(file.getName()));
            }
        } catch (Exception e) {
            getWindow().setBackgroundDrawableResource(R.drawable.wallpaper_1);
        }
    }

    private void applyWallpaperWithOverlay(File file) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) {
            getWindow().setBackgroundDrawableResource(R.drawable.wallpaper_4);
            return;
        }
        // 先用壁纸作为背景（确保立即显示）
        getWindow().setBackgroundDrawable(new CenterCropDrawable(bitmap));
        // 异步提取深色并叠加
        Palette.from(bitmap).generate(palette -> {
            int darkColor = 0xFF222222;
            if (palette.getDarkVibrantSwatch() != null) {
                darkColor = palette.getDarkVibrantSwatch().getRgb();
            } else if (palette.getDarkMutedSwatch() != null) {
                darkColor = palette.getDarkMutedSwatch().getRgb();
            } else if (palette.getDominantSwatch() != null) {
                darkColor = palette.getDominantSwatch().getRgb();
            }
            int overlayColor = Color.argb(230, Color.red(darkColor), Color.green(darkColor), Color.blue(darkColor));
            Drawable[] layers = new Drawable[]{
                    new CenterCropDrawable(bitmap),
                    new ColorDrawable(overlayColor)
            };
            getWindow().setBackgroundDrawable(new LayerDrawable(layers));
        });
    }

    /**
     * 保持宽高比的 center-crop 背景 Drawable，横屏竖屏都不会拉伸变形
     */
    private static class CenterCropDrawable extends Drawable {

        private final Bitmap bitmap;
        private final Paint paint;

        CenterCropDrawable(Bitmap bitmap) {
            this.bitmap = bitmap;
            this.paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty() || bitmap == null) return;

            float bitmapRatio = (float) bitmap.getWidth() / bitmap.getHeight();
            float boundsRatio = (float) bounds.width() / bounds.height();

            float scale, dx, dy;
            if (bitmapRatio > boundsRatio) {
                // 图片更宽：以高度为基准，宽度裁剪
                scale = (float) bounds.height() / bitmap.getHeight();
                float scaledWidth = bitmap.getWidth() * scale;
                dx = (bounds.width() - scaledWidth) / 2f;
                dy = 0;
            } else {
                // 图片更高：以宽度为基准，高度裁剪
                scale = (float) bounds.width() / bitmap.getWidth();
                float scaledHeight = bitmap.getHeight() * scale;
                dx = 0;
                dy = (bounds.height() - scaledHeight) / 2f;
            }

            RectF srcRect = new RectF(dx, dy, dx + bitmap.getWidth() * scale, dy + bitmap.getHeight() * scale);
            canvas.drawBitmap(bitmap, null, srcRect, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.WALL) refreshWall();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
