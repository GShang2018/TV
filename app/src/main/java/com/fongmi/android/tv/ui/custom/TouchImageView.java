package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class TouchImageView extends AppCompatImageView {

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];
    private final PointF last = new PointF();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private float minScale = 1f;
    private float maxScale = 5f;
    private float saveScale = 1f;
    private int mode = NONE;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private float origWidth, origHeight;
    private int viewWidth, viewHeight;

    public TouchImageView(Context context) {
        this(context, null);
    }

    public TouchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
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

    private void fitCenter() {
        if (origWidth <= 0 || origHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return;
        float scaleW = (float) viewWidth / origWidth;
        float scaleH = (float) viewHeight / origHeight;
        minScale = Math.min(scaleW, scaleH);
        float fitScale = Math.min(scaleW, scaleH);
        maxScale = Math.max(fitScale * 3f, fitScale + 2f);
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
        if (origWidth > 0 && origHeight > 0) fitCenter();
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

    private void onTouch(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                last.set(event.getX(), event.getY());
                mode = DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                last.set(event.getX(), event.getY());
                mode = ZOOM;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    float dx = event.getX() - last.x;
                    float dy = event.getY() - last.y;
                    matrix.postTranslate(dx, dy);
                    checkBound();
                    last.set(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }
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
        matrix.postTranslate(dx, dy);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float factor = detector.getScaleFactor();
            float newScale = saveScale * factor;
            if (newScale < minScale || newScale > maxScale) return true;
            matrix.postScale(factor, factor, detector.getFocusX(), detector.getFocusY());
            checkBound();
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mode = ZOOM;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            saveScale = matrixValues[Matrix.MSCALE_X];
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float scale = matrixValues[Matrix.MSCALE_X];
            float fitScale = Math.min((float) viewWidth / origWidth, (float) viewHeight / origHeight);
            if (scale > fitScale * 1.05f) {
                fitCenter();
            } else {
                float target = fitScale * 1.3f;
                matrix.postScale(target / scale, target / scale, e.getX(), e.getY());
                checkBound();
                setImageMatrix(matrix);
                saveScale = target;
            }
            return true;
        }
    }
}
