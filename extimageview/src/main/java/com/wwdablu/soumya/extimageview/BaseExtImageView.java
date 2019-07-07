package com.wwdablu.soumya.extimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;

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

    protected Bitmap mDisplayedBitmap;
    private Matrix mMatrix;

    public BaseExtImageView(Context context) {
        this(context, null, 0);
    }

    public BaseExtImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaseExtImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mMatrix = new Matrix();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
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

    protected final Bitmap scaleToFit(Bitmap bitmap, int toWidth, int toHeight) {

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

        float heightFactor = (float) originalHeight / (float) toWidth;
        float widthFactor = (float) originalWidth / (float) toHeight;

        return (widthFactor >= heightFactor) ? widthFactor : heightFactor;
    }

    protected final float getDensity() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(displayMetrics);
        return displayMetrics.density;
    }
}
