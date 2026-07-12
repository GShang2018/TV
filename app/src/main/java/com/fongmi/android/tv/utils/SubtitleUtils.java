package com.fongmi.android.tv.utils;

import android.util.TypedValue;

import androidx.media3.ui.SubtitleView;

import com.fongmi.android.tv.Setting;

public class SubtitleUtils {

	private static final float DEFAULT_TEXT_SIZE = 0.053f;
	private static final float DEFAULT_BOTTOM_PADDING = SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION;

	public static void addTextSize(SubtitleView view, float delta) {
		float currentSize = Setting.getSubtitleTextSize();
		float newSize = currentSize + delta;
		view.setFractionalTextSize(DEFAULT_TEXT_SIZE + newSize);
	}

	public static void subTextSize(SubtitleView view, float delta) {
		float currentSize = Setting.getSubtitleTextSize();
		float newSize = Math.max(-0.03f, currentSize - delta);
		view.setFractionalTextSize(DEFAULT_TEXT_SIZE + newSize);
	}

	public static float getTextSize(SubtitleView view) {
		return Setting.getSubtitleTextSize();
	}

	public static void addBottomPadding(SubtitleView view, float delta) {
		float currentPadding = Setting.getSubtitleBottomPadding();
		float newPadding = currentPadding + delta;
		view.setBottomPaddingFraction(DEFAULT_BOTTOM_PADDING + newPadding);
	}

	public static void subBottomPadding(SubtitleView view, float delta) {
		float currentPadding = Setting.getSubtitleBottomPadding();
		float newPadding = Math.max(-DEFAULT_BOTTOM_PADDING, currentPadding - delta);
		view.setBottomPaddingFraction(DEFAULT_BOTTOM_PADDING + newPadding);
	}

	public static float getBottomPadding(SubtitleView view) {
		return Setting.getSubtitleBottomPadding();
	}
}