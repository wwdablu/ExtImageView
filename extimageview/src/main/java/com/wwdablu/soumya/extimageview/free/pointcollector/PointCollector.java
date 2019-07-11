package com.wwdablu.soumya.extimageview.free.pointcollector;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Stores the history of all the points collected as epoch. Provides methods to store, retrieve and
 * remove points on an epoch or on the epoch itself.
 * @param <T> Classes of type PointF
 */
public final class PointCollector<T extends PointF> {

    private PointF mThreashold;

    private List<List<T>> mPoints;
    private List<T> mCurrentPointList;
    private T mLastPoint;

    public PointCollector() {
        mPoints = new LinkedList<>();
        mThreashold = new PointF(0, 0);
    }

    /**
     * Defines whether any points have been collected or not.
     * @return If empty or not
     */
    public boolean isEmpty() {
        return mPoints.size() == 0;
    }

    /**
     * Clear all the recorded points.
     */
    public void clear() {
        mPoints.clear();
    }

    /**
     * Number of sessions during which sets of points collected over time.
     * @return Epoch count
     */
    public int getEpochCount() {
        return mPoints.size();
    }

    /**
     * Number of points in an epoch
     * @param epoch Index 0 based
     * @return Number of points recorded
     */
    public int getPointCountForEpoch(int epoch) {

        if(epoch >= mPoints.size()) {
            return 0;
        }

        List<T> epochList = mPoints.get(epoch);
        return epochList == null ? 0 : epochList.size();
    }

    /**
     * Begins a new epoch (session) on which the points are to be collected
     */
    public void newEpoch() {
        mCurrentPointList = new LinkedList<>();
        mPoints.add(mCurrentPointList);
        mLastPoint = null;
    }

    /**
     * Stores the collected points for the current epoch. Must have a valid epoch.
     * @param point Point to store
     */
    public void addPoint(@NonNull T point) {

        if(mCurrentPointList == null) {
            throw new IllegalStateException("No epoch present. Call newEpoch to add points");
        }

        if(pointWithinThreashold(point)) {
            return;
        }

        mCurrentPointList.add(point);
        mLastPoint = point;
    }

    @Nullable
    public T getLastPointAdded() {
        return mLastPoint;
    }

    /**
     * Remove the last point stored for the current epoch.
     * @return True if point is removed
     */
    public boolean removeLastPoint() {

        if(mCurrentPointList.size() == 0) {
            mLastPoint = null;
            return false;
        }

        mCurrentPointList.remove(mCurrentPointList.size() - 1);

        if(mCurrentPointList.size() == 1) {
            mLastPoint = null;
        } else {
            mLastPoint = mCurrentPointList.get(mCurrentPointList.size() - 1);
        }

        return true;
    }

    /**
     * Remove the last stored epoch. All points stored in an epoch is also lost.
     * @return True if epoch is removed
     */
    public boolean removeLastEpoch() {

        if(mPoints.size() == 0) {
            mCurrentPointList = null;
            mLastPoint = null;
            return false;
        }

        mPoints.remove(mPoints.size() - 1);
        mCurrentPointList = mPoints.get(mPoints.size() -1);
        mLastPoint = mCurrentPointList.get(mCurrentPointList.size() - 1);

        return true;
    }

    public void setThreashold(T threashold) {

        if(threashold == null) {
            mThreashold = new PointF(0, 0);
        }

        mThreashold = threashold;
    }

    /**
     * Get all points collected over time as a list. Epochs are flattened.
     * @return Points collected over epochs
     */
    @NonNull
    public List<T> asList() {

        List<T> flattened = new LinkedList<>();
        for(List<T> epoch : mPoints) {
            flattened.addAll(epoch);
        }

        return Collections.unmodifiableList(flattened);
    }

    private boolean pointWithinThreashold(T point) {

        if(mCurrentPointList.size() == 0 || mLastPoint == null) {
            return false;
        }

        return !((Math.abs(mLastPoint.x - point.x) >= mThreashold.x) ||
                (Math.abs(mLastPoint.y - point.y) >= mThreashold.y));
    }
}
