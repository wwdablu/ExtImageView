package com.wwdablu.soumya.extimageview.rect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.wwdablu.soumya.extimageview.Result;

final class DisplayBitmapCropper implements Runnable {

    private Bitmap mDisplayedBitmap;
    private RectF mFrameRect;
    private Result<Bitmap> mResult;

    DisplayBitmapCropper(Bitmap mDisplayedBitmap, RectF mFrameRect, Result<Bitmap> mResult) {
        this.mDisplayedBitmap = mDisplayedBitmap;
        this.mFrameRect = mFrameRect;
        this.mResult = mResult;
    }

    @Override
    public void run() {

        if(mDisplayedBitmap == null || mDisplayedBitmap.isRecycled()) {
            mResult.onComplete(null);
            return;
        }

        try {
            int iFrameLeft = (int) Math.floor(mFrameRect.left);
            int iFrameTop = (int) Math.floor(mFrameRect.top);
            int iFrameRight = (int) Math.floor(mFrameRect.right);
            int iFrameBottom = (int) Math.floor(mFrameRect.bottom);

            int iFrameWidth = (int) Math.floor(mFrameRect.width());
            int iFrameHeight = (int) Math.floor(mFrameRect.height());

            Bitmap cropBitmap = Bitmap.createBitmap(iFrameWidth, iFrameHeight, Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(cropBitmap);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            canvas.drawBitmap(mDisplayedBitmap,
                    new Rect(iFrameLeft, iFrameTop, iFrameRight, iFrameBottom),
                    new Rect(0, 0, iFrameWidth, iFrameHeight),
                    paint);

            mResult.onComplete(cropBitmap);

        } catch (Exception ex) {
            mResult.onError(ex);
        }
    }
}
