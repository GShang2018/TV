package com.fongmi.android.tv.ui.activity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityGalleryBinding;
import com.fongmi.android.tv.databinding.ItemGalleryThumbBinding;
import com.fongmi.android.tv.ui.custom.TouchImageView;
import com.fongmi.android.tv.utils.ImgUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryActivity extends AppCompatActivity {

    private ActivityGalleryBinding mBinding;
    private ThumbAdapter mThumbAdapter;
    private List<String> mUrls;
    private int mPosition;
    private String mTitle = "";
    private ExecutorService mExecutor;

    public static void start(Context context, ArrayList<String> urls, int position) {
        start(context, urls, position, "");
    }

    public static void start(Context context, ArrayList<String> urls, int position, String title) {
        Intent intent = new Intent(context, GalleryActivity.class);
        intent.putStringArrayListExtra("urls", urls);
        intent.putExtra("position", position);
        intent.putExtra("title", title);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mUrls = getIntent().getStringArrayListExtra("urls");
        mPosition = getIntent().getIntExtra("position", 0);
        mTitle = getIntent().getStringExtra("title");
        if (mTitle == null) mTitle = "";
        if (mUrls == null || mUrls.isEmpty()) {
            finish();
            return;
        }
        mExecutor = Executors.newSingleThreadExecutor();
        setPager();
        setThumbs();
        updateCounter(mPosition);
        mBinding.counter.setSingleLine(true);
        mBinding.counter.setEllipsize(TextUtils.TruncateAt.END);
        mBinding.pager.setCurrentItem(mPosition, false);
        mBinding.back.setOnClickListener(v -> finish());
        mBinding.download.setOnClickListener(v -> downloadCurrent());
        mBinding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                mPosition = position;
                updateCounter(position);
                mThumbAdapter.setSelected(position);
                mThumbAdapter.scrollToPosition(position);
            }
        });
    }

    private void setPager() {
        mBinding.pager.setAdapter(new PagerAdapter());
    }

    private void setThumbs() {
        mBinding.thumbs.setHasFixedSize(true);
        mThumbAdapter = new ThumbAdapter();
        mBinding.thumbs.setAdapter(mThumbAdapter);
        mThumbAdapter.setSelected(mPosition);
        mBinding.thumbs.scrollToPosition(mPosition);
    }

    private void updateCounter(int position) {
        if (!TextUtils.isEmpty(mTitle)) mBinding.counter.setText(mTitle);
        else mBinding.counter.setText(getString(R.string.gallery_counter, position + 1, mUrls.size()));
    }

    private void downloadCurrent() {
        String url = mUrls.get(mPosition);
        if (url.isEmpty()) return;
        mExecutor.execute(() -> {
            try {
                Bitmap bitmap = Glide.with(GalleryActivity.this).asBitmap().load(ImgUtil.getUrl(url)).submit().get();
                saveBitmap(bitmap);
                runOnUiThread(() -> Toast.makeText(this, R.string.gallery_saved, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, R.string.gallery_save_fail, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveBitmap(Bitmap bitmap) throws Exception {
        String name = "TV_" + System.currentTimeMillis() + ".jpg";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
                }
            }
        } else {
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File file = new File(dir, name);
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out);
            }
            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), name, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExecutor != null) mExecutor.shutdown();
    }

    /**
     * 在 Activity 层面拦截 Ctrl + 鼠标滚轮事件并转发给当前显示的 TouchImageView。
     * 因为 ViewPager2 内部的 RecyclerView 会拦截滚轮事件用于翻页，导致 TouchImageView
     * 的 dispatchGenericMotionEvent 收不到，所以必须在更上层处理。
     */
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL
                && (event.getMetaState() & KeyEvent.META_CTRL_ON) != 0
                && event.getAxisValue(MotionEvent.AXIS_VSCROLL) != 0f) {
            TouchImageView image = getCurrentImage();
            if (image != null) {
                float scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                // 与 PiliPlus 一致：scaleChange = exp(-scrollDelta / scaleFactor)，scaleFactor = 200
                float scaleChange = (float) Math.exp(-scroll / 200f);
                // 将 Activity 窗口坐标转换为 TouchImageView 局部坐标
                int[] loc = new int[2];
                image.getLocationOnScreen(loc);
                float localX = event.getRawX() - loc[0];
                float localY = event.getRawY() - loc[1];
                image.zoomByScroll(scaleChange, localX, localY);
                return true;
            }
        }
        return super.dispatchGenericMotionEvent(event);
    }

    /**
     * 获取 ViewPager2 当前显示页的 TouchImageView。
     */
    private TouchImageView getCurrentImage() {
        if (mBinding.pager.getChildCount() == 0) return null;
        View child = mBinding.pager.getChildAt(0);
        if (!(child instanceof RecyclerView)) return null;
        RecyclerView.ViewHolder holder = ((RecyclerView) child).findViewHolderForAdapterPosition(mPosition);
        if (holder instanceof PagerAdapter.Holder) {
            return ((PagerAdapter.Holder) holder).image;
        }
        return null;
    }

    private class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.Holder> {

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TouchImageView image = new TouchImageView(parent.getContext());
            image.setBackgroundResource(R.color.black_20);
            image.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new Holder(image);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String url = mUrls.get(position);
            Glide.with(GalleryActivity.this).asBitmap().load(ImgUtil.getUrl(url)).into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    holder.image.setOrigSize(resource.getWidth(), resource.getHeight());
                    holder.image.setImageBitmap(resource);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                }
            });
        }

        @Override
        public int getItemCount() {
            return mUrls.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TouchImageView image;

            Holder(TouchImageView image) {
                super(image);
                this.image = image;
            }
        }
    }

    private class ThumbAdapter extends RecyclerView.Adapter<ThumbAdapter.Holder> {

        private int selected = 0;

        void setSelected(int position) {
            int old = selected;
            selected = position;
            notifyItemChanged(old);
            notifyItemChanged(selected);
        }

        void scrollToPosition(int position) {
            mBinding.thumbs.smoothScrollToPosition(position);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemGalleryThumbBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String url = mUrls.get(position);
            holder.binding.thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(GalleryActivity.this).asBitmap().load(ImgUtil.getUrl(url)).placeholder(R.drawable.ic_img_loading).into(holder.binding.thumb);
            holder.binding.border.setVisibility(position == selected ? View.VISIBLE : View.GONE);
            holder.itemView.setOnClickListener(v -> {
                mBinding.pager.setCurrentItem(position, false);
            });
        }

        @Override
        public int getItemCount() {
            return mUrls.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            ItemGalleryThumbBinding binding;

            Holder(ItemGalleryThumbBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
