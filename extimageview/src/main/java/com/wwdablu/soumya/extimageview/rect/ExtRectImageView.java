package com.wwdablu.soumya.extimageview.rect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.wwdablu.soumya.extimageview.BaseExtImageView;
import com.wwdablu.soumya.extimageview.Result;

public final class ExtRectImageView extends BaseExtImageView {

    private final String TAG = ExtRectImageView.class.getSimpleName();

    private TouchItem mTouchItem;
    private GridMode mGridMode;
    private CropMode mCropMode;

    private float mAnchorSize;
    private float mMinFrameSize;
    private float mLastX;
    private float mLastY;

    private int mViewWidth;
    private int mViewHeight;
    private int mTouchPadding;
    private int mGridLineCount;

    private boolean mIsReady;
    private boolean mShowGridLines;

    private Paint mFramePainter;
    private Paint mTranslucentPainter;
    private Paint mBitmapPainter;
    private Paint mGridPainter;
    private Paint mAnchorPainter;

    private Matrix mMatrix;

    private RectF mFrameRect;
    private RectF mImageRect;

    private Path mCropRegionPath;

    public ExtRectImageView(Context context) {
        this(context, null);
    }

    public ExtRectImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExtRectImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mIsReady = false;
        mShowGridLines = true;

        mTouchItem = TouchItem.NONE;
        mGridMode = GridMode.ALWAYS;
        mCropMode = CropMode.RECT;

        mTouchPadding = 0;

        mGridLineCount = Constants.GRID_LINE_COUNT;

        float density = getDensity();
        mAnchorSize = (density * Constants.ANCHOR_SIZE_DP);
        mMinFrameSize = density * Constants.FRAME_SIZE_MIN_DP;

        mFramePainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFramePainter.setFilterBitmap(true);
        mFramePainter.setColor(Constants.FRAME_LINE_COLOR);
        mFramePainter.setStyle(Paint.Style.STROKE);
        mFramePainter.setStrokeWidth(density * Constants.FRAME_BORDER_DP);

        mAnchorPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAnchorPainter.setColor(Constants.ANCHOR_COLOR);
        mAnchorPainter.setStyle(Paint.Style.FILL);

        mGridPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridPainter.setColor(Constants.WIDTH_LINE_COLOR);
        mGridPainter.setStrokeWidth(density * Constants.GRID_WIDTH_DP);

        mTranslucentPainter = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTranslucentPainter.setFilterBitmap(true);
        mTranslucentPainter.setColor(Constants.TRANSLUCENT_BLACK);
        mTranslucentPainter.setStyle(Paint.Style.FILL);

        mBitmapPainter = new Paint(Paint.ANTI_ALIAS_FLAG);

        mMatrix = new Matrix();

        mCropRegionPath = new Path();

        setGridVisibility(mGridMode);
    }

    /**
     * Set the way in which the grid lines are to behave. The default is {@link GridMode#ALWAYS}.
     * @see GridMode
     * @param mode Grid line mode
     */
    public void setGridVisibility(GridMode mode) {
        mGridMode = mode;
        switch (mode) {
            case ALWAYS:
                mShowGridLines = true;
                break;
            case HIDE:
            case SHOW_ON_TOUCH:
                mShowGridLines = false;
                break;
        }

        invalidate();
    }

    /**
     * Set the color using which the grid lines are to be drawn.
     * @param color Color
     */
    public void setGridColor(@ColorInt int color) {
        mGridPainter.setColor(color);
    }

    /**
     * Set the number of grid lines to be displayed. Range is from 2 to 8. Default is 3.
     * @param count Number of lines
     */
    public void setGridLineCount(@IntRange(from = 2, to = 8) int count) {

        if(count < 2) {
            count = 2;
        } else if (count > 8) {
            count = 8;
        }

        mGridLineCount = count;
    }

    public void setAnchorColor(@ColorInt int color) {
        mAnchorPainter.setColor(color);
    }

    /**
     * Set the color using which the frame lines are to be drawn.
     * @param color Color
     */
    public void setFrameColor(@ColorInt int color) {
        mFramePainter.setColor(color);
    }

    /**
     * Set the mode for crop selection and finally cropping. Default is {@link CropMode#RECT}
     * @see CropMode
     * @param cropMode Mode of cropping
     */
    public void setCropMode(CropMode cropMode) {
        mCropMode = cropMode;
    }

    /**
     * Crop the image or bitmap once the selection is confirmed.
     */
    @Override
    public void crop(@Nullable Result<Void> result) {

        getOriginalBitmap(new Result<Bitmap>() {
            @Override
            public void onComplete(Bitmap data) {
                mExecutorService.execute(new OriginalBitmapCropper(data, mDisplayedBitmap, mFrameRect, new Result<Bitmap>() {
                    @Override
                    public void onComplete(Bitmap originalCropped) {

                        saveOriginalBitmap(originalCropped, new Result<Void>() {
                            @Override
                            public void onComplete(Void data) {

                                mExecutorService.execute(new DisplayBitmapCropper(mDisplayedBitmap, mFrameRect, new Result<Bitmap>() {
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
                                }));
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                if (result != null) {
                                    result.onError(throwable);
                                }
                            }
                        });
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
            public void onError(Throwable throwable) {
                if (result != null) {
                    result.onError(throwable);
                }
            }
        });
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
        if (getDrawable() != null) {
            prepareLayout(mViewWidth, mViewHeight);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);

        if(!mIsReady) {
            return;
        }

        if(mDisplayedBitmap != null && !mDisplayedBitmap.isRecycled()) {

            PointF coor = getImageContentStartCoordinate();

            //canvas.drawBitmap(mDisplayedBitmap, mMatrix, mBitmapPainter);
            canvas.drawBitmap(mDisplayedBitmap, coor.x, coor.y, mBitmapPainter);

            /*
             * Renders all the components which are required to select the crop region. It draws the
             * translucent overlay, the transparent crop region and finally the anchors which defines
             * the RECT for the crop region.
             */
            renderUncropRegionOverlay(canvas);
            renderCropFrame(canvas);
            renderAnchorPoints(canvas);

            if (mShowGridLines) {
                renderGridLines(canvas);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
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
                handleActionMove(event);
                return true;

            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                mTouchItem = TouchItem.NONE;
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (mGridMode == GridMode.SHOW_ON_TOUCH) {
                    mShowGridLines = false;
                }

                mTouchItem = TouchItem.NONE;
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDetachedFromWindow() {

        if(mDisplayedBitmap != null && !mDisplayedBitmap.isRecycled()) {
            mDisplayedBitmap.recycle();
            mDisplayedBitmap = null;
        }

        super.onDetachedFromWindow();
    }

    /*
     * Render the rect which shows the view that will be cropped.
     */
    private void renderCropFrame(@NonNull Canvas canvas) {
        canvas.drawRect(mFrameRect, mFramePainter);
    }

    /*
     * Render the rect which will have the translucent hue, denoting that this section of the
     * bitmap or image, will not be cropped.
     */
    private void renderUncropRegionOverlay(@NonNull Canvas canvas) {

        mCropRegionPath.reset();

        //Finds the translucent region (uncrop)
        mCropRegionPath.addRect(mImageRect, Path.Direction.CW);
        mCropRegionPath.addRect(mFrameRect, Path.Direction.CCW);

        canvas.drawPath(mCropRegionPath, mTranslucentPainter);
    }

    /*
     * Calculate the grid line spacing and then render them on canvas, if applicable.
     */
    private void renderGridLines(@NonNull Canvas canvas) {

        if(!mShowGridLines) {
            return;
        }

        float wSpacing = mFrameRect.width() / (float) mGridLineCount;
        float hSpacing = mFrameRect.height() / (float) mGridLineCount;

        for(int index = 0; index < mGridLineCount; index++) {

            canvas.drawLine(mFrameRect.left + (wSpacing * (index + 1)), mFrameRect.top,
            mFrameRect.left + (wSpacing * (index + 1)), mFrameRect.bottom, mGridPainter);

            canvas.drawLine(mFrameRect.left, mFrameRect.top + (hSpacing * (index + 1)),
            mFrameRect.right, mFrameRect.top + (hSpacing * (index + 1)), mGridPainter);
        }
    }

    /*
     * Draw the anchor points or circles. These will be used to control the size of the crop region.
     */
    private void renderAnchorPoints(@NonNull Canvas canvas) {

        canvas.drawCircle(mFrameRect.left, mFrameRect.top, mAnchorSize, mAnchorPainter);
        canvas.drawCircle(mFrameRect.right, mFrameRect.top, mAnchorSize, mAnchorPainter);
        canvas.drawCircle(mFrameRect.left, mFrameRect.bottom, mAnchorSize, mAnchorPainter);
        canvas.drawCircle(mFrameRect.right, mFrameRect.bottom, mAnchorSize, mAnchorPainter);
    }

    /*
     * Prepare the layout of the crop view based on the dimension of the displayed bitmap.
     */
    private void prepareLayout(int width, int height) {

        if (width == 0 || height == 0) {
            return;
        }

        PointF point = getImageContentStartCoordinate();

        mImageRect = new RectF(0f, 0f, width, height);

        int hCenter = width >> 1;
        int vCenter = height >> 1;
        int smaller = hCenter < vCenter ? hCenter : vCenter;
        mFrameRect = new RectF(smaller >> 1, (smaller >> 1) + ((int)point.y >> 1), smaller +
                (smaller >> 1), smaller + (smaller >> 1) + ((int)point.y >> 1));

        mIsReady = true;
        invalidate();
    }

    /*
     * Handle the ACTION_MOVE based on the TouchItem.
     */
    private void handleActionMove(MotionEvent e) {

        float dX = e.getX() - mLastX;
        float dY = e.getY() - mLastY;

        switch (mTouchItem) {

            case SELECTOR_CENTER:
                mFrameRect.left += dX;
                mFrameRect.right += dX;
                mFrameRect.top += dY;
                mFrameRect.bottom += dY;
                ensureCropBoundsWhileMoving();
                break;

            case SELECTOR_TL:
                moveTopLeftAnchor(dX, dY);
                break;

            case SELECTOR_TR:
                moveTopRightAnchor(dX, dY);
                break;

            case SELECTOR_BL:
                moveBottomLeftAnchor(dX, dY);
                break;

            case SELECTOR_BR:
                moveBottomRightAnchor(dX, dY);
                break;

            case NONE:
                break;
        }

        mLastX = e.getX();
        mLastY = e.getY();

        invalidate();
    }

    /*
     * Ensure that the crop frame remains within the bounds of the ImageView while moving
     */
    private void ensureCropBoundsWhileMoving() {

        PointF coor = getImageContentStartCoordinate();

        float delta = mFrameRect.left - mImageRect.left;
        delta -= coor.x;
        if (delta < 0) {
            mFrameRect.left -= delta;
            mFrameRect.right -= delta;
        }

        delta = mFrameRect.right - mImageRect.right;
        delta -= coor.x;
        if (delta > 0) {
            mFrameRect.left -= delta;
            mFrameRect.right -= delta;
        }

        delta = mFrameRect.top - mImageRect.top;
        delta -= coor.y;
        if (delta < 0) {
            mFrameRect.top -= delta;
            mFrameRect.bottom -= delta;
        }

        delta = mFrameRect.bottom - mImageRect.bottom;
        delta += coor.y;
        if (delta > 0) {
            mFrameRect.top -= delta;
            mFrameRect.bottom -= delta;
        }
    }

    /*
     * Determine the touch was inside the crop frame, on the anchors or outside altogether.
     */
    private void determineTouchedItem(float x, float y) {

        if (isInsideAnchor(x, y, mFrameRect.left, mFrameRect.top)) {
            mTouchItem = TouchItem.SELECTOR_TL;
        }

        else if (isInsideAnchor(x, y, mFrameRect.right, mFrameRect.top)) {
            mTouchItem = TouchItem.SELECTOR_TR;
        }

        else if (isInsideAnchor(x, y, mFrameRect.left, mFrameRect.bottom)) {
            mTouchItem = TouchItem.SELECTOR_BL;
        }

        else if (isInsideAnchor(x, y, mFrameRect.right, mFrameRect.bottom)) {
            mTouchItem = TouchItem.SELECTOR_BR;
        }

        else if (isTouchInsideCropFrame(x, y)) {
            mTouchItem = TouchItem.SELECTOR_CENTER;
        }

        else {
            mTouchItem = TouchItem.NONE;
        }

        mShowGridLines = (mGridMode != GridMode.HIDE) && (mTouchItem != TouchItem.NONE);
    }

    /*
     * Determine if the touch is inside the crop frame which means that the user is trying to
     * move around the frame.
     */
    private boolean isTouchInsideCropFrame(float x, float y) {

        if (mFrameRect.left <= x && mFrameRect.right >= x) {
            if (mFrameRect.top <= y && mFrameRect.bottom >= y) {
                mTouchItem = TouchItem.SELECTOR_CENTER;
                return true;
            }
        }
        return false;
    }

    /*
     * Determine if the user is trying to move around the anchors to change the dimension of the
     * crop frame.
     */
    private boolean isInsideAnchor(float touchX, float touchY, float anchorX, float anchorY) {

        float dX = touchX - anchorX;
        float dY = touchY - anchorY;

        return Math.pow(mAnchorSize + mTouchPadding, 2) >= ((dX * dX) + (dY * dY));
    }

    /*
     * Move the anchor which is at the top left of the crop rect
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void moveTopLeftAnchor(float x, float y) {

        mFrameRect.left += x;

        if (mCropMode == CropMode.RECT) {

            mFrameRect.top += y;

            if (isFrameWidthBelowMinimum()) {
                float offsetX = mMinFrameSize - mFrameRect.width();
                mFrameRect.left -= offsetX;
            }
            if (isFrameHeightBelowMinimum()) {
                float offsetY = mMinFrameSize - mFrameRect.height();
                mFrameRect.top -= offsetY;
            }

        } else if (mCropMode == CropMode.SQUARE) {

            mFrameRect.top += x;

            if (isFrameWidthBelowMinimum()) {
                float offsetX = mMinFrameSize - mFrameRect.width();
                mFrameRect.top -= offsetX;
                mFrameRect.left -= offsetX;
            }
            if (isFrameHeightBelowMinimum()) {
                float offsetY = mMinFrameSize - mFrameRect.height();
                mFrameRect.top -= offsetY;
                mFrameRect.left -= offsetY;
            }
        }

        ensureCropBoundsWhileUsingAnchor();
    }

    /*
     * Move the anchor which is at the top right of the crop rect
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void moveTopRightAnchor(float x, float y) {

        mFrameRect.right += x;

        if (mCropMode == CropMode.RECT) {

            mFrameRect.top += y;
            if (isFrameWidthBelowMinimum()) {
                float offsetX = mMinFrameSize - mFrameRect.width();
                mFrameRect.right += offsetX;
            }
            if (isFrameHeightBelowMinimum()) {
                float offsetY = mMinFrameSize - mFrameRect.height();
                mFrameRect.top -= offsetY;
            }

        } else if (mCropMode == CropMode.SQUARE) {

            mFrameRect.top -= x;

            if (isFrameWidthBelowMinimum()) {
                float offsetX = mMinFrameSize - mFrameRect.width();
                mFrameRect.top -= offsetX;
                mFrameRect.right += offsetX;
            }
            if (isFrameHeightBelowMinimum()) {
                float offsetY = mMinFrameSize - mFrameRect.height();
                mFrameRect.top -= offsetY;
                mFrameRect.right += offsetY;
            }
        }

        ensureCropBoundsWhileUsingAnchor();
    }

    /*
     * Move the anchor which is at the bottom left of the crop rect
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void moveBottomLeftAnchor(float x, float y) {

        mFrameRect.left += x;

        if (mCropMode == CropMode.RECT) {

            mFrameRect.bottom += y;

            if (isFrameWidthBelowMinimum()) {
                float offsetX = mMinFrameSize - mFrameRect.width();
                mFrameRect.left -= offsetX;
            }
            if (isFrameHeightBelowMinimum()) {
                float offsetY = mMinFrameSize - mFrameRect.height();
                mFrameRect.bottom += offsetY;
            }

        } else if (mCropMode == CropMode.SQUARE) {

            mFrameRect.bottom -= x;

            if (isFrameWidthBelowMinimum()) {
                float offsetX = mMinFrameSize - mFrameRect.width();
                mFrameRect.bottom += offsetX;
                mFrameRect.left -= offsetX;
            }
            if (isFrameHeightBelowMinimum()) {
                float offsetY = mMinFrameSize - mFrameRect.height();
                mFrameRect.bottom += offsetY;
                mFrameRect.left -= offsetY;
            }
        }

        ensureCropBoundsWhileUsingAnchor();
    }

    /*
     * Move the anchor which is at the bottom right of the crop rect
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void moveBottomRightAnchor(float x, float y) {

        mFrameRect.right += x;

        if (mCropMode == CropMode.RECT) {

            mFrameRect.bottom += y;

            if (isFrameWidthBelowMinimum()) {
                float offsetX = mMinFrameSize - mFrameRect.width();
                mFrameRect.right += offsetX;
            }
            if (isFrameHeightBelowMinimum()) {
                float offsetY = mMinFrameSize - mFrameRect.height();
                mFrameRect.bottom += offsetY;
            }

        } else if (mCropMode == CropMode.SQUARE) {

            mFrameRect.bottom += x;

            if (isFrameWidthBelowMinimum()) {
                float offsetX = mMinFrameSize - mFrameRect.width();
                mFrameRect.bottom += offsetX;
                mFrameRect.right += offsetX;
            }
            if (isFrameHeightBelowMinimum()) {
                float offsetY = mMinFrameSize - mFrameRect.height();
                mFrameRect.bottom += offsetY;
                mFrameRect.right += offsetY;
            }
        }

        ensureCropBoundsWhileUsingAnchor();
    }

    private boolean isFrameWidthBelowMinimum() {
        return mFrameRect.width() < mMinFrameSize;
    }

    private boolean isFrameHeightBelowMinimum() {
        return mFrameRect.height() < mMinFrameSize;
    }

    /*
     * When moving the anchors ensure that the frame is within the bounds of the ImageView
     */
    private void ensureCropBoundsWhileUsingAnchor() {

        PointF coor = getImageContentStartCoordinate();

        float dLeft = (mFrameRect.left - mImageRect.left);
        float dRight = (mFrameRect.right - mImageRect.right);
        float dTop = (mFrameRect.top - mImageRect.top);
        float dBottom = (mFrameRect.bottom - mImageRect.bottom);

        dLeft -= coor.x;
        dRight -= coor.x;
        dTop -= coor.y;
        dBottom += coor.y;

        if (dLeft < 0) {
            mFrameRect.left -= dLeft;
        }
        if (dRight > 0) {
            mFrameRect.right -= dRight;
        }
        if (dTop < 0) {
            mFrameRect.top -= dTop;
        }
        if (dBottom > 0) {
            mFrameRect.bottom -= dBottom;
        }
    }

    private PointF getImageContentStartCoordinate() {

        int idWidth = mDisplayedBitmap.getWidth();
        int idHeight = mDisplayedBitmap.getHeight();

        float left = 0;
        float top = 0;

        if(idWidth == getMeasuredWidth()) {
            top = (getMeasuredHeight() - idHeight) >> 1;
        } else {
            left = (getMeasuredWidth() - idWidth) >> 1;
        }

        return new PointF(left, top);
    }
}
