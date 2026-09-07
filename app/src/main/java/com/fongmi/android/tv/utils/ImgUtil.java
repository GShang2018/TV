package com.fongmi.android.tv.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Setting;
import com.github.catvod.utils.Json;
import com.google.common.net.HttpHeaders;

import java.util.Map;

import jahirfiquitiva.libs.textdrawable.TextDrawable;

public class ImgUtil {

    private static ObjectKey getSignature(String url) {
        return new ObjectKey(url + "_" + Setting.getQuality());
    }

    public static void load(String url, CustomTarget<Bitmap> target) {
        if (!TextUtils.isEmpty(url)) Glide.with(App.get()).asBitmap().load(getUrl(url)).skipMemoryCache(true).dontAnimate().signature(getSignature(url)).into(target);
    }

    public static void load(String url, int error, CustomTarget<Drawable> target) {
        if (TextUtils.isEmpty(url)) target.onLoadFailed(ResUtil.getDrawable(error));
        else Glide.with(App.get()).asDrawable().load(getUrl(url)).error(error).skipMemoryCache(true).dontAnimate().signature(getSignature(url)).into(target);
    }

    public static void rect(String text, String url, ImageView view) {
        load(text, url, view, ImageView.ScaleType.CENTER, true);
    }

    public static void oval(String text, String url, ImageView view) {
        load(text, url, view, ImageView.ScaleType.CENTER, false);
    }

    public static void load(String text, String url, ImageView view, ImageView.ScaleType scaleType, boolean rect) {
        // 加载中/失败等状态图标统一按容器最小边长 30% 居中显示，不铺满容器；加载成功后由 listener 设为 CENTER_CROP 铺满
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (!TextUtils.isEmpty(url)) {
            setStateIcon(view, R.drawable.ic_img_loading);
            Glide.with(App.get()).asBitmap().load(getUrl(url)).skipMemoryCache(true).dontAnimate().sizeMultiplier(Setting.getThumbnail()).signature(getSignature(url)).listener(getListener(view, scaleType)).into(view);
        } else if (text.length() > 0) view.setImageDrawable(getTextDrawable(text.substring(0, 1), rect));
        else setStateIcon(view, R.drawable.ic_img_error);
    }

    // 海报加载：加载中显示状态图标（容器最小边长 30%，不铺满），完成后按 3:4 竖版固定显示
    public static void loadPoster(String text, String url, ImageView view) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (!TextUtils.isEmpty(url)) {
            setStateIcon(view, R.drawable.ic_img_loading);
            Glide.with(App.get()).asBitmap().load(getUrl(url)).transform(new PosterTransform(Setting.getPosterCrop())).skipMemoryCache(true).dontAnimate().listener(getPosterListener(view)).into(view);
        } else if (text.length() > 0) view.setImageDrawable(getTextDrawable(text.substring(0, 1), true));
        else setStateIcon(view, R.drawable.ic_img_error);
    }

    public static void loadVod(String text, String url, ImageView view) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (!TextUtils.isEmpty(url)) {
            setStateIcon(view, R.drawable.ic_img_loading);
            Glide.with(App.get()).asBitmap().load(getUrl(url)).listener(getListener(view)).into(view);
        } else if (text.length() > 0) view.setImageDrawable(getTextDrawable(text.substring(0, 1), true));
        else setStateIcon(view, R.drawable.ic_img_error);
    }

    public static void loadLive(String url, ImageView view) {
        view.setVisibility(TextUtils.isEmpty(url) ? View.GONE : View.VISIBLE);
        if (TextUtils.isEmpty(url)) setStateIcon(view, R.drawable.ic_img_empty);
        else Glide.with(App.get()).asBitmap().load(url).skipMemoryCache(true).dontAnimate().signature(getSignature(url)).listener(getLiveLogoListener(view)).into(view);
    }

    // 台标大图加载：显式 override 目标尺寸，请求尺寸与 view 布局时序解耦（对齐点播封面按固定像素发起请求的逻辑），
    // 首次解码分辨率即 ≥ 显示需求，避免"点击重绑后按更大尺寸重新解码才变清晰"
    public static void loadLive(String url, ImageView view, int width, int height) {
        view.setVisibility(TextUtils.isEmpty(url) ? View.GONE : View.VISIBLE);
        if (TextUtils.isEmpty(url)) setStateIcon(view, R.drawable.ic_img_empty);
        else Glide.with(App.get()).asBitmap().load(url).override(width, height).skipMemoryCache(true).dontAnimate().signature(getSignature(url)).listener(getLiveLogoListener(view)).into(view);
    }

    // 台标 logo 加载：加载中显示状态图标（容器最小边长 30%，不铺满），加载完成后 FIT_CENTER 等比缩放完整显示不裁剪
    public static void loadLogo(String text, String url, ImageView view) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        if (!TextUtils.isEmpty(url)) {
            setStateIcon(view, R.drawable.ic_img_loading);
            Glide.with(App.get()).asBitmap().load(getUrl(url)).skipMemoryCache(true).dontAnimate().signature(getSignature(url)).listener(getLogoListener(view)).into(view);
        } else if (text.length() > 0) view.setImageDrawable(getTextDrawable(text.substring(0, 1), true));
        else setStateIcon(view, R.drawable.ic_img_error);
    }

    // 缩略图加载：加载中显示 30% 尺寸状态图标（不铺满），成功后恢复 centerCrop 铺满显示
    public static void loadThumb(String url, ImageView view) {
        if (TextUtils.isEmpty(url)) {
            setStateIcon(view, R.drawable.ic_img_error);
            return;
        }
        setStateIcon(view, R.drawable.ic_img_loading);
        Glide.with(App.get()).asBitmap().load(getUrl(url)).dontAnimate().listener(new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                // 加载失败同样按容器最小边长 30% 显示错误图标，不铺满
                setStateIcon(view, R.drawable.ic_img_error);
                return true;
            }

            @Override
            public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return false;
            }
        }).into(view);
    }

    // 状态图标（加载中/空/错误）统一规则：任何条件下都不铺满容器，
    // 而是按容器最小边长的 30% 计算图标尺寸并居中显示；容器未完成测量时先按默认尺寸兜底，布局完成后校正
    public static void setStateIcon(ImageView view, int resId) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        int w = view.getWidth(), h = view.getHeight();
        int side = w > 0 && h > 0 ? Math.max(1, (int) (Math.min(w, h) * 0.3f)) : ResUtil.dp2px(36);
        view.setImageDrawable(new StateIconDrawable(ResUtil.getDrawable(resId), side));
        if (w <= 0 || h <= 0) {
            // 布局完成后若图标仍未被真实图片替换，则按容器最小边长 30% 重新设置一次
            view.post(() -> {
                if (!(view.getDrawable() instanceof StateIconDrawable)) return;
                int nw = view.getWidth(), nh = view.getHeight();
                if (nw <= 0 || nh <= 0) return; // 容器仍无尺寸则保持默认尺寸，避免空转
                setStateIcon(view, resId);
            });
        }
    }

    // 等比缩放到指定像素尺寸的状态图标：以该尺寸作为固有尺寸绘制，ImageView 居中显示时不铺满容器
    private static class StateIconDrawable extends Drawable {
        private final Drawable icon;
        private int size;

        StateIconDrawable(Drawable icon, int size) {
            this.icon = icon;
            this.size = size;
            setBounds(0, 0, size, size);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            icon.setBounds(getBounds());
            icon.draw(canvas);
        }

        @Override
        public void setAlpha(int alpha) {
            icon.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            icon.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }
    }

    private static Drawable getTextDrawable(String text, boolean rect) {
        TextDrawable.Builder builder = new TextDrawable.Builder().withBorder(ResUtil.dp2px(2), ColorGenerator.get700(text));
        if (rect) return builder.buildRoundRect(text, ColorGenerator.get500(text), ResUtil.dp2px(8));
        return builder.buildRound(text, ColorGenerator.get500(text));
    }

    public static Object getUrl(String url) {
        String param = null;
        url = UrlUtil.convert(url);
        if (url.startsWith("data:")) return url;
        LazyHeaders.Builder builder = new LazyHeaders.Builder();
        if (url.contains("@Headers=")) addHeader(builder, param = url.split("@Headers=")[1].split("@")[0]);
        if (url.contains("@Cookie=")) builder.addHeader(HttpHeaders.COOKIE, param = url.split("@Cookie=")[1].split("@")[0]);
        if (url.contains("@Referer=")) builder.addHeader(HttpHeaders.REFERER, param = url.split("@Referer=")[1].split("@")[0]);
        if (url.contains("@User-Agent=")) builder.addHeader(HttpHeaders.USER_AGENT, param = url.split("@User-Agent=")[1].split("@")[0]);
        url = param == null ? url : url.split("@")[0];
        return TextUtils.isEmpty(url) ? null : new GlideUrl(url, builder.build());
    }

    private static void addHeader(LazyHeaders.Builder builder, String header) {
        Map<String, String> map = Json.toMap(Json.parse(header));
        for (Map.Entry<String, String> entry : map.entrySet()) builder.addHeader(UrlUtil.fixHeader(entry.getKey()), entry.getValue());
    }

    // 直播台标加载监听：失败时按状态图标规则显示（容器最小边长 30%，不铺满）；
    // 成功后若此前被状态图标强制为 CENTER，则恢复为 FIT_CENTER 等比完整显示不裁剪
    private static RequestListener<Bitmap> getLiveLogoListener(ImageView view) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                // 台标加载失败图标按容器最小边长 30% 居中显示，不铺满
                setStateIcon(view, R.drawable.ic_img_empty);
                return true;
            }

            @Override
            public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                if (view.getScaleType() == ImageView.ScaleType.CENTER) view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                return false;
            }
        };
    }

    // 海报加载监听：加载完成后按 3:4 竖版固定显示框，再铺满显示
    private static RequestListener<Bitmap> getPosterListener(ImageView view) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                setPosterFrame(view, false);
                // 加载失败图标按容器最小边长 30% 居中显示，不铺满
                setStateIcon(view, R.drawable.ic_img_error);
                return true;
            }

            @Override
            public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                setPosterFrame(view, resource.getWidth() >= resource.getHeight());
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return false;
            }
        };
    }

    // 海报显示框统一保持 3:4 竖版比例（原 4:3 启用逻辑已注释）
    private static void setPosterFrame(ImageView view, boolean horizontal) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        int w = params.width;
        int h = params.height;
        if (w < h) return; // 已是竖版 3:4，无需调整
        // 旧逻辑：横图 4:3（宽高对调），竖图 3:4（保持竖版），面积不变
        // if ((w > h) == horizontal) return;
        // params.width = h;
        // params.height = w;
        // view.setLayoutParams(params);
    }

    private static RequestListener<Bitmap> getListener(ImageView view) {
        return getListener(view, ImageView.ScaleType.CENTER);
    }

    private static RequestListener<Bitmap> getLogoListener(ImageView view) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                // 加载失败图标按容器最小边长 30% 居中显示，不铺满
                setStateIcon(view, R.drawable.ic_img_error);
                return true;
            }

            @Override
            public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                return false;
            }
        };
    }

    private static RequestListener<Bitmap> getListener(ImageView view, ImageView.ScaleType scaleType) {
        return new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Bitmap> target, boolean isFirstResource) {
                // 加载失败图标按容器最小边长 30% 居中显示，不铺满
                setStateIcon(view, R.drawable.ic_img_error);
                return true;
            }

            @Override
            public boolean onResourceReady(@NonNull Bitmap resource, @NonNull Object model, Target<Bitmap> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                return false;
            }
        };
    }
}
