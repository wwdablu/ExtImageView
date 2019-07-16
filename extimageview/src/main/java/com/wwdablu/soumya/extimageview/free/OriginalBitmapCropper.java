package com.wwdablu.soumya.extimageview.free;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;

import com.wwdablu.soumya.extimageview.Result;
import com.wwdablu.soumya.extimageview.free.pointcollector.CPoint;

import java.util.List;

final class OriginalBitmapCropper implements Runnable {

    private static final int GREEN_SCREEN = Color.parseColor("#00b140");

    private Result<Bitmap> mResult;
    private Bitmap mOriginalBitmap;
    private Bitmap mDisplayedBitmap;
    private List<CPoint> mPointList;

    OriginalBitmapCropper(@NonNull Bitmap originalBitmap,
                          @NonNull Bitmap displayBitmap,
                          @NonNull List<CPoint> pointList,
                          @NonNull Result<Bitmap> result) {

        mResult = result;
        mOriginalBitmap = originalBitmap;
        mDisplayedBitmap = displayBitmap;
        mPointList = pointList;
    }

    @Override
    public void run() {

        if(mOriginalBitmap == null || mOriginalBitmap.isRecycled()) {
            mResult.onComplete(null);
            return;
        }

        CPointInfo cPointInfo = getPathFrom(mPointList);

        Path path = cPointInfo.path;
        RectF pathBound = new RectF();
        path.computeBounds(pathBound, true);

        Bitmap newBitmap = Bitmap.createBitmap((int) pathBound.width(), (int) pathBound.height(),
                Bitmap.Config.ARGB_8888);
        Bitmap maskBitmap = mask(mOriginalBitmap, path);

        Canvas canvas = new Canvas(newBitmap);
        Paint bitmapPainter = new Paint(Paint.ANTI_ALIAS_FLAG);

        canvas.drawBitmap(maskBitmap, new Rect(cPointInfo.minimums.xAsInt(), cPointInfo.minimums.yAsInt(),
                (int) pathBound.right + cPointInfo.minimums.xAsInt(),
                (int) pathBound.bottom + cPointInfo.minimums.yAsInt()),
                new Rect(0, 0, (int) pathBound.right, (int) pathBound.bottom), bitmapPainter);

        maskBitmap.recycle();

        mResult.onComplete(newBitmap);
    }

    private Bitmap mask(Bitmap bitmap, Path path) {

        Bitmap maskBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        maskBitmap.eraseColor(Color.TRANSPARENT);

        Canvas canvas = new Canvas(maskBitmap);
        Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(GREEN_SCREEN);
        canvas.drawPath(path, pathPaint);

        Paint xPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, xPaint);
        return maskBitmap;
    }

    private CPointInfo getPathFrom(List<CPoint> points) {

        CPointInfo cPointInfo = new CPointInfo();

        cPointInfo.path = new Path();
        if(points == null || points.size() == 0) {
            return cPointInfo;
        }

        int idWidth = mDisplayedBitmap.getWidth();
        int idHeight = mDisplayedBitmap.getHeight();

        float ioWidth = mOriginalBitmap.getWidth();
        float ioHeight = mOriginalBitmap.getHeight();

        float widthFactor = ioWidth/idWidth;
        float heightFactor = ioHeight/idHeight;

        //Find the left-most and top-most CPoint
        cPointInfo.minimums = new CPoint(Float.MAX_VALUE, Float.MAX_VALUE);

        for(CPoint point : points) {

            point.x *= widthFactor;
            point.y *= heightFactor;

            if(cPointInfo.path.isEmpty()) {
                cPointInfo.path.moveTo(point.x, point.y);
            }

            cPointInfo.path.lineTo(point.x, point.y);
            if(point.x < cPointInfo.minimums.x) {
                cPointInfo.minimums.x = point.x;
            }

            if(point.y < cPointInfo.minimums.y) {
                cPointInfo.minimums.y = point.y;
            }
        }

        cPointInfo.path.close();
        return cPointInfo;
    }

    private class CPointInfo {
        Path path;
        CPoint minimums;
    }
}
