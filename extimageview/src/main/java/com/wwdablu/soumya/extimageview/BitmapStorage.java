package com.wwdablu.soumya.extimageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.locks.ReentrantLock;

final class BitmapStorage {

    private File mOriginalFile;
    private ReentrantLock mGuard;

    BitmapStorage(@NonNull Context context, @NonNull String id) {
        mOriginalFile = new File(context.getCacheDir(), id + "_" + "o.png");
        mGuard = new ReentrantLock();
    }

    void saveOriginalBitmap(@NonNull Bitmap bitmap, @Nullable Result<Void> result) {

        new Thread(() -> {

            mGuard.lock();
            try (FileOutputStream fos = new FileOutputStream(mOriginalFile)) {

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                if(result != null) {
                    result.onComplete(null);
                }

            } catch (Exception ex) {
                if(result != null) {
                    result.onError(ex);
                }
            } finally {
                mGuard.unlock();
            }
        }).start();
    }

    void getOriginalBitmap(@NonNull Result<Bitmap> result) {

        new Thread(() -> {

            mGuard.lock();
            try {
                result.onComplete(getOriginalBitmap());
            } catch (Exception ex) {
                result.onError(ex);
            } finally {
                mGuard.unlock();
            }
        }).start();
    }

    private Bitmap getOriginalBitmap() throws Exception {

        if(!isOriginalBitmapPresent()) {
            return null;
        }

        Bitmap bitmap;
        try (FileInputStream fis = new FileInputStream(mOriginalFile)) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDensity = 0;
            options.inScaled = false;
            options.inTargetDensity = 0;
            bitmap = BitmapFactory.decodeStream(fis, null, options);

        }

        return bitmap;
    }

    boolean isOriginalBitmapPresent() {

        return mOriginalFile.exists();
    }

    void deleteOriginalBitmap() {

        if(mOriginalFile.exists()) {
            mOriginalFile.delete();
        }
    }
}
