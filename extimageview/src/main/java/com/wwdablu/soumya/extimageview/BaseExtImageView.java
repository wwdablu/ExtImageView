package com.wwdablu.soumya.extimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseExtImageView extends AppCompatImageView {

    public enum Rotate {
        CW_90(90),
        CW_180(180),
        CW_270(270),
        CCW_90(-90),
        CCW_180(-180),
        CCW_270(-270);

        private int value;
        Rotate(final int value) {
            this.value = value;
        }
    }

    private Matrix mMatrix;
    private BitmapStorage mStorage;
    private boolean mIsDisplayBitmapReady;

    protected Bitmap mDisplayedBitmap;
    protected ExecutorService mExecutorService;
    private Handler mUIHandler;

    public abstract void crop(@Nullable Result<Void> result);

    public BaseExtImageView(Context context) {
        this(context, null, 0);
    }

    public BaseExtImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseExtImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMatrix = new Matrix();
        mExecutorService = Executors.newSingleThreadExecutor();
        mIsDisplayBitmapReady = false;
        mUIHandler = new Handler(Looper.getMainLooper());
        mStorage = new BitmapStorage(context, "uid_" + System.currentTimeMillis());
    }

    @Override
    public void setImageBitmap(Bitmap bm) {

        if(!mIsDisplayBitmapReady) {
            mDisplayedBitmap = bm;
            if(!mStorage.isOriginalBitmapPresent()) {
                mStorage.saveOriginalBitmap(mDisplayedBitmap, null);
            }
            return;
        }

        super.setImageBitmap(bm);

        if(bm.equals(mDisplayedBitmap)) {
            return;
        }

        if(mDisplayedBitmap != null && !mDisplayedBitmap.isRecycled()) {
            mDisplayedBitmap.recycle();
        }

        mDisplayedBitmap = bm;
    }

    /**
     * Returns a mutable copy of the bitmap that is being displayed on screen to the device. Note,
     * that the caller has the responsibility to recycle() the bitmap once it has been used.
     * @param result Returns the bitmap or the exception generated
     */
    public final void getCroppedBitmap(@NonNull Result<Bitmap> result) {

        if(mStorage == null) {
            result.onError(new IllegalStateException("Invalid object"));
            return;
        }

        mStorage.getOriginalBitmap(result);
    }

    /**
     * Rotate the bitmap by the specified option.
     * @see Rotate
     * @param by Rotation value
     */
    public void rotate(Rotate by) {

        mMatrix.reset();
        mMatrix.preRotate(by.value);

        setImageBitmap(Bitmap.createBitmap(mDisplayedBitmap, 0, 0, mDisplayedBitmap.getWidth(),
                mDisplayedBitmap.getHeight(), mMatrix, true));
    }

    protected final void getOriginalBitmap(@NonNull Result<Bitmap> result) {
        mStorage.getOriginalBitmap(result);
    }

    protected final void saveOriginalBitmap(@NonNull Bitmap bitmap, @NonNull Result<Void> result) {
        mStorage.saveOriginalBitmap(bitmap, result);
    }

    @Override
    @CallSuper
    protected void onFinishInflate() {

        super.onFinishInflate();
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                if(mDisplayedBitmap == null || mDisplayedBitmap.isRecycled() || getVisibility() == View.GONE) {
                    return;
                }

                getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Bitmap scaled = scaleToFit(mDisplayedBitmap, getMeasuredWidth(), getMeasuredHeight());
                mDisplayedBitmap.recycle();
                mDisplayedBitmap = scaled;
                mIsDisplayBitmapReady = true;
                setImageBitmap(mDisplayedBitmap);
            }
        });
    }

    @Override
    @CallSuper
    protected void onDetachedFromWindow() {

        if(!mExecutorService.isShutdown()) {
            mExecutorService.shutdownNow();
        }

        mStorage.deleteOriginalBitmap();
        super.onDetachedFromWindow();
    }

    protected final void runOnUiThread(@NonNull Runnable runnable) {
        mUIHandler.post(runnable);
    }

    public final Bitmap scaleToFit(Bitmap bitmap, int toWidth, int toHeight) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        float useFactor = getScaleFactor(bitmap, toWidth, toHeight);

        int scaleWidth = (int) (originalWidth / useFactor);
        int scaleHeight = (int) (originalHeight / useFactor);

        return Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, true);
    }

    protected final float getScaleFactor(Bitmap bitmap, int toWidth, int toHeight) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();

        float heightFactor = (float) originalWidth / (float) toWidth;
        float widthFactor = (float) originalHeight / (float) toHeight;

        return (widthFactor >= heightFactor) ? widthFactor : heightFactor;
    }

    protected final float getDensity() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(displayMetrics);
        return displayMetrics.density;
    }
}
