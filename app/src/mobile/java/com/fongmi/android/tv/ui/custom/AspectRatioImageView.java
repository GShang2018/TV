package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;

import com.google.android.material.imageview.ShapeableImageView;

/**
 * 固定宽高比 ImageView：宽度被父容器约束（match_parent/固定值）时，高度 = 宽度 * RATIO。
 * 在 onMeasure 阶段直接输出最终尺寸，首帧几何即正确：
 * 无需运行时改 LayoutParams、无二次 requestLayout、Glide 首次解码即按最终尺寸，一次到位清晰。
 * 继承 ShapeableImageView 以支持 shapeAppearanceOverlay 圆角裁切。
 */
public class AspectRatioImageView extends ShapeableImageView {

	// 高/宽 = 3/4（宽:高 = 4:3）
	private static final float RATIO = 3f / 4f;

	public AspectRatioImageView(Context context) {
		super(context);
	}

	public AspectRatioImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AspectRatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int width = getMeasuredWidth();
		if (width > 0) setMeasuredDimension(width, Math.round(width * RATIO));
	}
}
