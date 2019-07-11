package com.wwdablu.soumya.extimageview.rect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;

import com.wwdablu.soumya.extimageview.Result;

final class OriginalBitmapCropper implements Runnable {

    private Bitmap mOriginalBitmap;
    private Bitmap mDisplayedBitmap;
    private RectF  mFrameRect;
    private Result<Bitmap> mResult;

    OriginalBitmapCropper(@NonNull Bitmap mOriginalBitmap,
                          @NonNull Bitmap mDisplayedBitmap,
                          @NonNull RectF frameRect,
                          @NonNull Result<Bitmap> result,
                          @NonNull PointF coordinate) {

        this.mOriginalBitmap = mOriginalBitmap;
        this.mDisplayedBitmap = mDisplayedBitmap;
        this.mResult = result;

        mFrameRect = new RectF(frameRect);
        mFrameRect.left -= coordinate.x;
        mFrameRect.top -= coordinate.y;
        mFrameRect.right -= coordinate.x;
        mFrameRect.bottom -=coordinate.y;
    }

    @Override
    public void run() {

        if(mOriginalBitmap == null) {
            mResult.onComplete(null);
            return;
        }

        try {
            int idWidth = mDisplayedBitmap.getWidth();
            int idHeight = mDisplayedBitmap.getHeight();

            float ioWidth = mOriginalBitmap.getWidth();
            float ioHeight = mOriginalBitmap.getHeight();

            float widthFactor = ioWidth/idWidth;
            float heightFactor = ioHeight/idHeight;

            int iFrameLeft = (int) (Math.floor(mFrameRect.left) * widthFactor);
            int iFrameTop = (int) (Math.floor(mFrameRect.top) * heightFactor);
            int iFrameRight = (int) (Math.floor(mFrameRect.right) * widthFactor);
            int iFrameBottom = (int) (Math.floor(mFrameRect.bottom) * heightFactor);

            int iFrameWidth = (int) (Math.floor(mFrameRect.width()) * widthFactor);
            int iFrameHeight = (int) (Math.floor(mFrameRect.height()) * heightFactor);

            Bitmap originalCropped = Bitmap.createBitmap(iFrameWidth,
                    iFrameHeight, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(originalCropped);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            canvas.drawBitmap(mOriginalBitmap, new Rect(iFrameLeft, iFrameTop, iFrameRight, iFrameBottom),
                    new Rect(0, 0, iFrameWidth, iFrameHeight),
                    paint);

            mOriginalBitmap.recycle();

            mOriginalBitmap = null;
            mDisplayedBitmap = null;
            mFrameRect = null;

            mResult.onComplete(originalCropped);

        } catch (Exception ex) {
            mResult.onError(ex);
        }
    }
}
