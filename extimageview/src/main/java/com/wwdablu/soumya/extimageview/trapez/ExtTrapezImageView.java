package com.wwdablu.soumya.extimageview.trapez;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.wwdablu.soumya.extimageview.BaseExtImageView;
import com.wwdablu.soumya.extimageview.Result;

import java.util.ArrayList;
import java.util.List;

public class ExtTrapezImageView extends BaseExtImageView {

    private static final int TOP_LEFT = 0;
    private static final int TOP_RIGHT = 1;
    private static final int BOTTOM_LEFT = 2;
    private static final int BOTTOM_RIGHT = 3;

    private TouchItem mTouchItem;

    private List<Point> mAnchorPoints;

    private Paint mAnchorPainter;
    private Paint mConnectingLinePainter;
    private Paint mBitmapPainter;

    private int mViewWidth;
    private int mViewHeight;

    private float mLastX;
    private float mLastY;
    private float mAnchorSize;

    public ExtTrapezImageView(Context context) {
        this(context, null);
    }

    public ExtTrapezImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExtTrapezImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mAnchorPoints = new ArrayList<>(4);
        for(int i = 0; i < 4; i++) {
            mAnchorPoints.add(new Point(0, 0));
        }

        float density = getDensity();
        mAnchorSize = (density * Constants.ANCHOR_SIZE_DP);

        mTouchItem = TouchItem.NONE;

        mAnchorPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAnchorPainter.setColor(Constants.ANCHOR_COLOR);

        mConnectingLinePainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mConnectingLinePainter.setColor(Constants.WIDTH_LINE_COLOR);
        mConnectingLinePainter.setStrokeWidth(density * Constants.LINE_WIDTH_DP);

        mBitmapPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    public void crop(@Nullable Result<Void> result) {
        mExecutorService.execute(new DisplayBitmapCropper(mDisplayedBitmap, mAnchorPoints, new Result<Bitmap>() {
            @Override
            public void onComplete(Bitmap data) {
                runOnUiThread(() -> setImageBitmap(data));
                if (result != null) {
                    result.onComplete(null);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (result != null) {
                    result.onError(throwable);
                }
            }
        }, getImageContentStartCoordinate()));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        setMeasuredDimension(viewWidth, viewHeight);

        mViewWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        mViewHeight = viewHeight - getPaddingTop() - getPaddingBottom();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if(getDrawable() != null) {
            prepareLayout(mViewWidth, mViewHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawColor(Color.BLACK);

        PointF coor = getImageContentStartCoordinate();
        canvas.drawBitmap(mDisplayedBitmap, coor.x, coor.y, mBitmapPainter);

        drawConnectedAnchors(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                mLastX = event.getX();
                mLastY = event.getY();
                determineTouchedItem(event.getX(), event.getY());
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (mTouchItem != TouchItem.NONE) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                handleAnchorMove(event);
                return true;

            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                mTouchItem = TouchItem.NONE;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                mTouchItem = TouchItem.NONE;
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    /*
     * Finds the anchor which has been touched
     */
    private void determineTouchedItem(float x, float y) {

        int anchorIndex = getSelectedAnchorIndex(x, y);

        if (anchorIndex == TOP_LEFT) {
            mTouchItem = TouchItem.SELECTOR_TL;
        }

        else if (anchorIndex == TOP_RIGHT) {
            mTouchItem = TouchItem.SELECTOR_TR;
        }

        else if (anchorIndex == BOTTOM_LEFT) {
            mTouchItem = TouchItem.SELECTOR_BL;
        }

        else if (anchorIndex == BOTTOM_RIGHT) {
            mTouchItem = TouchItem.SELECTOR_BR;
        }

        else {
            mTouchItem = TouchItem.NONE;
        }
    }

    /*
     * Finds the anchor index which has been touched
     */
    private int getSelectedAnchorIndex(float touchX, float touchY) {

        int index = 0;
        for(Point anchorPoint : mAnchorPoints) {

            if (((touchX >= (anchorPoint.x - mAnchorSize)) && (touchX <= (anchorPoint.x + mAnchorSize)))
             && ((touchY >= (anchorPoint.y - mAnchorSize)) && (touchY <= (anchorPoint.y + mAnchorSize)))) {
                return index;
            }

            ++index;
        }

        return -1;
    }

    /*
     * Move the anchors around on the image
     */
    private void handleAnchorMove(MotionEvent e) {

        if(mTouchItem == TouchItem.NONE) {
            return;
        }

        Point anchorPoint = null;

        switch (mTouchItem) {

            case SELECTOR_TL:
                anchorPoint = mAnchorPoints.get(TOP_LEFT);
                break;

            case SELECTOR_TR:
                anchorPoint = mAnchorPoints.get(TOP_RIGHT);
                break;

            case SELECTOR_BL:
                anchorPoint = mAnchorPoints.get(BOTTOM_LEFT);
                break;

            case SELECTOR_BR:
                anchorPoint = mAnchorPoints.get(BOTTOM_RIGHT);
                break;
        }

        if(anchorPoint == null) {
            return;
        }

        float dX = e.getX() - mLastX;
        float dY = e.getY() - mLastY;

        anchorPoint.x += dX;
        anchorPoint.y += dY;

        mLastX = e.getX();
        mLastY = e.getY();

        ensureCropBoundsWhileUsingAnchor(anchorPoint);
        invalidate();
    }

    /*
     * Ensure that the anchors are within the image
     */
    private void ensureCropBoundsWhileUsingAnchor(Point anchorPoint) {

        PointF coor = getImageContentStartCoordinate();

        float maxLeft = coor.x;
        float maxTop = coor.y;
        float maxRight = maxLeft + mDisplayedBitmap.getWidth();
        float maxBottom = maxTop + mDisplayedBitmap.getHeight();

        if(anchorPoint.x < maxLeft) {
            anchorPoint.x = (int) Math.floor(maxLeft);
        } else if (anchorPoint.x > maxRight) {
            anchorPoint.x = (int) Math.floor(maxRight);
        }

        if(anchorPoint.y < maxTop) {
            anchorPoint.y = (int) Math.floor(maxTop);
        } else if (anchorPoint.y > maxBottom) {
            anchorPoint.y = (int) Math.floor(maxBottom);
        }
    }

    /*
     * Draw the initial anchor points once the image has been loaded.
     */
    private void prepareLayout(int width, int height) {

        if (width == 0 || height == 0) {
            return;
        }

        int wMid = width >> 1;
        int hMid = height >> 1;

        int wQuart = wMid >> 2;
        int hQuart = hMid >> 2;

        Point anchorPoint = mAnchorPoints.get(TOP_LEFT);
        anchorPoint.set(wMid - wQuart, hMid - hQuart);

        anchorPoint = mAnchorPoints.get(TOP_RIGHT);
        anchorPoint.set(wMid + wQuart, hMid - hQuart);

        anchorPoint = mAnchorPoints.get(BOTTOM_LEFT);
        anchorPoint.set(wMid - wQuart, hMid + hQuart);

        anchorPoint = mAnchorPoints.get(BOTTOM_RIGHT);
        anchorPoint.set(wMid + wQuart, hMid + hQuart);
    }

    /*
     * Draw the anchors and then connect them with lines.
     */
    private void drawConnectedAnchors(@NonNull Canvas canvas) {

        if(mAnchorPoints.get(0) == null) {
            return;
        }

        for(Point anchorPoint : mAnchorPoints) {
            canvas.drawCircle(anchorPoint.x, anchorPoint.y, mAnchorSize, mAnchorPainter);
        }

        Point startPoint = mAnchorPoints.get(TOP_LEFT);
        Point endPoint = mAnchorPoints.get(TOP_RIGHT);
        canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, mConnectingLinePainter);

        startPoint = mAnchorPoints.get(TOP_RIGHT);
        endPoint = mAnchorPoints.get(BOTTOM_RIGHT);
        canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, mConnectingLinePainter);

        startPoint = mAnchorPoints.get(BOTTOM_RIGHT);
        endPoint = mAnchorPoints.get(BOTTOM_LEFT);
        canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, mConnectingLinePainter);

        startPoint = mAnchorPoints.get(BOTTOM_LEFT);
        endPoint = mAnchorPoints.get(TOP_LEFT);
        canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, mConnectingLinePainter);
    }
}
