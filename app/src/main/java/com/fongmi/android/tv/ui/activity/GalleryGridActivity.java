package com.fongmi.android.tv.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityGalleryGridBinding;
import com.fongmi.android.tv.databinding.AdapterGalleryGridBinding;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;

public class GalleryGridActivity extends AppCompatActivity {

    private ActivityGalleryGridBinding mBinding;
    private ArrayList<String> mUrls;

    public static void start(Context context, ArrayList<String> urls) {
        Intent intent = new Intent(context, GalleryGridActivity.class);
        intent.putStringArrayListExtra("urls", urls);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityGalleryGridBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mUrls = getIntent().getStringArrayListExtra("urls");
        if (mUrls == null || mUrls.isEmpty()) {
            finish();
            return;
        }
        mBinding.title.setText(getString(R.string.detail_gallery_all, mUrls.size()));
        mBinding.back.setOnClickListener(v -> finish());
        initGrid();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initGrid();
    }

    private void initGrid() {
        int spacing = ResUtil.dp2px(12);
        int screenWidth = ResUtil.getScreenWidth(this);
        int spanCount = Math.max(3, screenWidth / ResUtil.dp2px(160));
        int imageSize = (screenWidth - spacing * (spanCount + 1)) / spanCount;
        GridLayoutManager glm = new GridLayoutManager(this, spanCount);
        mBinding.grid.setLayoutManager(glm);
        if (mBinding.grid.getItemDecorationCount() == 0) {
            mBinding.grid.addItemDecoration(new ItemDecoration(spacing));
        }
        mBinding.grid.setAdapter(new GridAdapter(mUrls, imageSize));
    }

    private class GridAdapter extends RecyclerView.Adapter<GridAdapter.Holder> {

        private final ArrayList<String> urls;
        private final int size;

        GridAdapter(ArrayList<String> urls, int size) {
            this.urls = urls;
            this.size = size;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(AdapterGalleryGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String url = urls.get(position);
            holder.binding.getRoot().getLayoutParams().width = size;
            holder.binding.getRoot().getLayoutParams().height = size;
            ImgUtil.loadVod("", url, holder.binding.image);
            holder.itemView.setOnClickListener(v -> GalleryActivity.start(GalleryGridActivity.this, urls, position));
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            AdapterGalleryGridBinding binding;

            Holder(AdapterGalleryGridBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    static class ItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        ItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int spanCount = ((GridLayoutManager) parent.getLayoutManager()).getSpanCount();
            int halfSpacing = spacing / 2;
            outRect.left = halfSpacing;
            outRect.right = halfSpacing;
            outRect.top = halfSpacing;
            outRect.bottom = halfSpacing;
        }
    }
}
