package com.fongmi.android.tv.ui.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

import androidx.appcompat.widget.AppCompatImageView;

public class TouchImageView extends AppCompatImageView {

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];
    private final PointF last = new PointF();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private VelocityTracker velocityTracker;
    private OverScroller scroller;
    private ValueAnimator zoomAnimator;
    private float minScale = 1f;
    private float maxScale = 5f;
    private float saveScale = 1f;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private float origWidth, origHeight;
    private int viewWidth, viewHeight;
    private int touchSlop;
    private boolean isDragging;

    public TouchImageView(Context context) {
        this(context, null);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        scroller = new OverScroller(context);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        setOnTouchListener((v, event) -> {
            scaleDetector.onTouchEvent(event);
            gestureDetector.onTouchEvent(event);
            onTouch(event);
            return true;
        });
    }

    public void reset() {
        fitCenter();
    }

    private float fitScale() {
        if (origWidth <= 0 || origHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return 1f;
        return Math.min((float) viewWidth / origWidth, (float) viewHeight / origHeight);
    }

    private void fitCenter() {
        if (origWidth <= 0 || origHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return;
        float fitScale = fitScale();
        minScale = fitScale;
        maxScale = Math.max(fitScale * 3f, fitScale + 2f);
        matrix.reset();
        matrix.setScale(fitScale, fitScale);
        matrix.postTranslate((viewWidth - origWidth * fitScale) / 2f, (viewHeight - origHeight * fitScale) / 2f);
        saveScale = fitScale;
        setImageMatrix(matrix);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        // 仅首次尺寸有效时执行，避免频繁闪烁
        if (origWidth > 0 && origHeight > 0 && matrix.isIdentity()) {
            fitCenter();
        }
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        matrix.getValues(matrixValues);
    }

    public void setOrigSize(int width, int height) {
        this.origWidth = width;
        this.origHeight = height;
        if (viewWidth > 0 && viewHeight > 0) fitCenter();
    }

    // ==========【修复核心BUG：惯性滑动实现】==========
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            matrix.getValues(matrixValues);
            float currX = scroller.getCurrX();
            float currY = scroller.getCurrY();
            // 先重置缩放，再设置绝对偏移，杜绝持续叠加漂移
            matrix.setScale(matrixValues[Matrix.MSCALE_X], matrixValues[Matrix.MSCALE_X]);
            matrix.postTranslate(currX, currY);
            checkBound();
            setImageMatrix(matrix);
            postInvalidateOnAnimation();
        }
    }

    private void initVelocityTracker(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    /**
     * 支持模拟器/桌面端 Ctrl + 鼠标滚轮缩放（参考 PiliPlus mouse_interactive_viewer）。
     * 以鼠标位置为焦点，使用指数缩放因子实现平滑缩放。
     */
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_SCROLL
                && (event.getMetaState() & KeyEvent.META_CTRL_ON) != 0
                && event.getAxisValue(MotionEvent.AXIS_VSCROLL) != 0f) {
            if (zoomAnimator != null) zoomAnimator.cancel();
            float scroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            float scaleChange = (float) Math.exp(-scroll / 200f);
            zoomByScroll(scaleChange, event.getX(), event.getY());
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }

    /**
     * 供外部（GalleryActivity）调用的滚轮缩放入口。
     */
    public void zoomByScroll(float scaleChange, float focusX, float focusY) {
        if (origWidth <= 0 || origHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return;
        if (zoomAnimator != null) zoomAnimator.cancel();
        matrix.getValues(matrixValues);
        float current = matrixValues[Matrix.MSCALE_X];
        float target = clamp(current * scaleChange, minScale, maxScale);
        float factor = target / current;
        matrix.postScale(factor, factor, focusX, focusY);
        checkBound();
        setImageMatrix(matrix);
        saveScale = matrixValues[Matrix.MSCALE_X];
    }

    private void onTouch(MotionEvent event) {
        initVelocityTracker(event);
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                last.set(event.getX(), event.getY());
                mode = DRAG;
                isDragging = false;
                if (isZoomed()) {
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (mode == DRAG) {
                    mode = ZOOM;
                    // 开始双指，终止惯性
                    if (!scroller.isFinished()) scroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    float dx = event.getX() - last.x;
                    float dy = event.getY() - last.y;
                    if (!isDragging) {
                        isDragging = Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop;
                    }
                    if (isDragging && isZoomed()) {
                        matrix.postTranslate(dx, dy);
                        checkBound();
                        last.set(event.getX(), event.getY());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mode == DRAG && isDragging) {
                    velocityTracker.computeCurrentVelocity(1000);
                    float vx = velocityTracker.getXVelocity();
                    float vy = velocityTracker.getYVelocity();
                    matrix.getValues(matrixValues);
                    float transX = matrixValues[Matrix.MTRANS_X];
                    float transY = matrixValues[Matrix.MTRANS_Y];
                    float scale = matrixValues[Matrix.MSCALE_X];
                    float width = origWidth * scale;
                    float height = origHeight * scale;

                    // 计算允许滑动范围
                    float minX, maxX, minY, maxY;
                    if (width <= viewWidth) {
                        minX = maxX = (viewWidth - width) / 2f;
                    } else {
                        minX = viewWidth - width;
                        maxX = 0;
                    }
                    if (height <= viewHeight) {
                        minY = maxY = (viewHeight - height) / 2f;
                    } else {
                        minY = viewHeight - height;
                        maxY = 0;
                    }

                    // =========修复fling起点：使用当前图片偏移transX,transY=========
                    scroller.fling((int) transX, (int) transY, (int) vx, (int) vy,
                            (int) minX, (int) maxX, (int) minY, (int) maxY);
                    postInvalidateOnAnimation();
                }
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                mode = NONE;
                isDragging = false;
                recycleVelocityTracker();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }
    }

    private boolean isZoomed() {
        if (origWidth <= 0 || origHeight <= 0) return false;
        matrix.getValues(matrixValues);
        float scale = matrixValues[Matrix.MSCALE_X];
        return scale > minScale * 1.01f;
    }

    private void checkBound() {
        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scale = matrixValues[Matrix.MSCALE_X];
        float width = origWidth * scale;
        float height = origHeight * scale;
        float dx = 0, dy = 0;

        if (width <= viewWidth) {
            dx = (viewWidth - width) / 2f - transX;
        } else {
            if (transX > 0) dx = -transX;
            if (transX + width < viewWidth) dx = viewWidth - transX - width;
        }
        if (height <= viewHeight) {
            dy = (viewHeight - height) / 2f - transY;
        } else {
            if (transY > 0) dy = -transY;
            if (transY + height < viewHeight) dy = viewHeight - transY - height;
        }
        if (dx != 0 || dy != 0) {
            matrix.postTranslate(dx, dy);
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void animateZoom(float targetScale, float focusX, float focusY) {
        if (origWidth <= 0 || origHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return;
        if (zoomAnimator != null) zoomAnimator.cancel();
        matrix.getValues(matrixValues);
        float s0 = matrixValues[Matrix.MSCALE_X];
        float t0x = matrixValues[Matrix.MTRANS_X];
        float t0y = matrixValues[Matrix.MTRANS_Y];
        float px = (focusX - t0x) / s0;
        float py = (focusY - t0y) / s0;

        zoomAnimator = ValueAnimator.ofFloat(0f, 1f);
        zoomAnimator.setDuration(250);
        zoomAnimator.setInterpolator(new DecelerateInterpolator());
        zoomAnimator.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            float s = s0 + (targetScale - s0) * t;
            matrix.setScale(s, s);
            matrix.postTranslate(focusX - px * s, focusY - py * s);
            checkBound();
            setImageMatrix(matrix);
        });
        zoomAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                saveScale = matrixValues[Matrix.MSCALE_X];
            }
        });
        zoomAnimator.start();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float current = matrixValues[Matrix.MSCALE_X];
            float target = clamp(current * detector.getScaleFactor(), minScale, maxScale);
            float factor = target / current;
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            checkBound();
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (zoomAnimator != null) zoomAnimator.cancel();
            if (!scroller.isFinished()) scroller.abortAnimation();
            mode = ZOOM;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            saveScale = matrixValues[Matrix.MSCALE_X];
            mode = NONE;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float scale = matrixValues[Matrix.MSCALE_X];
            float fitScale = fitScale();
            if (scale > fitScale * 1.5f) {
                animateZoom(fitScale, e.getX(), e.getY());
            } else {
                animateZoom(Math.min(fitScale * 2f, maxScale), e.getX(), e.getY());
            }
            return true;
        }
    }

    // ==========新增：防止动画内存泄漏==========
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (zoomAnimator != null) {
            zoomAnimator.cancel();
            zoomAnimator = null;
        }
        if (!scroller.isFinished()) {
            scroller.abortAnimation();
        }
        recycleVelocityTracker();
    }

    // =========可选对外接口=========
    public float getMinScale() {
        return minScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public float getCurrentScale() {
        matrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }
}

