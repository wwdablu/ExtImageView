package com.wwdablu.soumya.extimageview.trapez;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.support.annotation.NonNull;

import com.wwdablu.soumya.extimageview.Result;

import java.util.ArrayList;
import java.util.List;

class OriginalBitmapCropper implements Runnable {

    private Bitmap mOriginalBitmap;
    private Bitmap mDisplayBitmap;
    private List<Point> mAnchorPoints;
    private PointF mImageCoor;
    private Result<Bitmap> mResult;

    OriginalBitmapCropper(@NonNull Bitmap originalBitmap,
                          @NonNull Bitmap displayBitmap,
                          @NonNull List<Point> anchorPoints,
                          @NonNull Result<Bitmap> result,
                          @NonNull PointF imageCoor) {

        this.mOriginalBitmap = originalBitmap;
        this.mDisplayBitmap = displayBitmap;

        this.mAnchorPoints = new ArrayList<>(anchorPoints.size());
        for(Point p : anchorPoints) {
            this.mAnchorPoints.add(new Point(p));
        }

        this.mResult = result;
        this.mImageCoor = imageCoor;
    }

    @Override
    public void run() {

        try {
            int idWidth = mDisplayBitmap.getWidth();
            int idHeight = mDisplayBitmap.getHeight();

            float ioWidth = mOriginalBitmap.getWidth();
            float ioHeight = mOriginalBitmap.getHeight();

            float widthFactor = ioWidth/idWidth;
            float heightFactor = ioHeight/idHeight;

            pathCorrection(widthFactor, heightFactor);

            Bitmap croppedAndCorrected = trapezoidToRectangleTransform();
            mResult.onComplete(croppedAndCorrected);
        } catch (Exception ex) {
            mResult.onError(ex);
        }
    }

    private Bitmap trapezoidToRectangleTransform() {

        float[] src = new float[8];

        Point point = mAnchorPoints.get(0);
        src[0] = point.x;
        src[1] = point.y;

        point = mAnchorPoints.get(1);
        src[2] = point.x;
        src[3] = point.y;

        point = mAnchorPoints.get(3);
        src[4] = point.x;
        src[5] = point.y;

        point = mAnchorPoints.get(2);
        src[6] = point.x;
        src[7] = point.y;

        // set up a dest polygon which is just a rectangle
        float[] dst = new float[8];
        dst[0] = 0;
        dst[1] = 0;
        dst[2] = mOriginalBitmap.getWidth();
        dst[3] = 0;
        dst[4] = mOriginalBitmap.getWidth();
        dst[5] = mOriginalBitmap.getHeight();
        dst[6] = 0;
        dst[7] = mOriginalBitmap.getHeight();

        // create a matrix for transformation.
        Matrix matrix = new Matrix();

        // set the matrix to map the source values to the dest values.
        boolean mapped = matrix.setPolyToPoly (src, 0, dst, 0, 4);

        float[] mappedTL = new float[] { 0, 0 };
        matrix.mapPoints(mappedTL);
        int maptlx = Math.round(mappedTL[0]);
        int maptly = Math.round(mappedTL[1]);

        float[] mappedTR = new float[] { mOriginalBitmap.getWidth(), 0 };
        matrix.mapPoints(mappedTR);
        int maptry = Math.round(mappedTR[1]);

        float[] mappedLL = new float[] { 0, mOriginalBitmap.getHeight() };
        matrix.mapPoints(mappedLL);
        int mapllx = Math.round(mappedLL[0]);

        int shiftX = Math.max(-maptlx, -mapllx);
        int shiftY = Math.max(-maptry, -maptly);

        Bitmap croppedAndCorrected = null;
        if (mapped) {
            Bitmap imageOut = Bitmap.createBitmap(mOriginalBitmap, 0, 0, mOriginalBitmap.getWidth(), mOriginalBitmap.getHeight(), matrix, true);
            croppedAndCorrected = Bitmap.createBitmap(imageOut, shiftX, shiftY, mOriginalBitmap.getWidth(), mOriginalBitmap.getHeight(), null, true);
            imageOut.recycle();
            mOriginalBitmap.recycle();
            System.gc();
        }

        return croppedAndCorrected;
    }

    private void pathCorrection(float wf, float hf) {

        Path path = new Path();

        Point point = mAnchorPoints.get(0);
        point.x -= mImageCoor.x;
        point.y -= mImageCoor.y;
        point.x *= wf;
        point.y *= hf;
        path.moveTo(point.x, point.y);
        path.lineTo(point.x, point.y);

        point = mAnchorPoints.get(1);
        point.x -= mImageCoor.x;
        point.y -= mImageCoor.y;
        point.x *= wf;
        point.y *= hf;
        path.lineTo(point.x, point.y);

        point = mAnchorPoints.get(3);
        point.x -= mImageCoor.x;
        point.y -= mImageCoor.y;
        point.x *= wf;
        point.y *= hf;
        path.lineTo(point.x, point.y);

        point = mAnchorPoints.get(2);
        point.x -= mImageCoor.x;
        point.y -= mImageCoor.y;
        point.x *= wf;
        point.y *= hf;
        path.lineTo(point.x, point.y);

        path.close();
    }
}
