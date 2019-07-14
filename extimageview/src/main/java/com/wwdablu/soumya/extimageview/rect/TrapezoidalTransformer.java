package com.wwdablu.soumya.extimageview.rect;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.annotation.NonNull;

import com.wwdablu.soumya.extimageview.Result;

final class TrapezoidalTransformer implements Runnable {

    private Bitmap mBitmap;
    private Result<Bitmap> mResult;
    private int mWidth;
    private int mHeight;

    TrapezoidalTransformer(@NonNull Bitmap bitmap,
                           @NonNull Result<Bitmap> result,
                           int outWidth,
                           int outHeight) {

        mBitmap = bitmap;
        mResult = result;
        mWidth = outWidth;
        mHeight = outHeight;
    }

    @Override
    public void run() {

        // Set up a source polygon.
        // X and Y values are "flattened" into the array.
        float[] src = new float[8];
        src[0] = x1;
        src[1] = y1;
        src[2] = x2;
        src[3] = y2;
        src[4] = x3;
        src[5] = y3;
        src[6] = x4;
        src[7] = y4;

        // set up a dest polygon which is just a rectangle
        float[] dst = new float[8];
        dst[0] = 0;
        dst[1] = 0;
        dst[2] = mWidth;
        dst[3] = 0;
        dst[4] = mWidth;
        dst[5] = mHeight;
        dst[6] = 0;
        dst[7] = mHeight;

        // create a matrix for transformation.
        Matrix matrix = new Matrix();

        // set the matrix to map the source values to the dest values.
        boolean mapped = matrix.setPolyToPoly (src, 0, dst, 0, 4);

        // check to make sure your mapping succeeded
        // if your source polygon is a distorted rectangle, you should be okay
        if (mapped) {

            // create a new bitmap from the original bitmap using the matrix for transform
            Bitmap imageOut = Bitmap.createBitmap(mBitmap, 0, 0, mWidth, mHeight, matrix, true);
        }
    }
}
