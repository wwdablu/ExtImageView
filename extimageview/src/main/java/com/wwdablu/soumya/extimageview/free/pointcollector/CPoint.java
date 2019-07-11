package com.wwdablu.soumya.extimageview.free.pointcollector;

import android.graphics.PointF;

public class CPoint extends PointF {

    private boolean mDrawn;

    public CPoint(float x, float y) {
        super(x, y);
    }

    public float x() {
        return x;
    }

    public int xAsInt() {
        return Math.round(x);
    }

    public float y() {
        return y;
    }

    public int yAsInt() {
        return Math.round(y);
    }

    public boolean isDrawn() {
        return mDrawn;
    }

    public void setDrawn() {
        mDrawn = true;
    }
}
