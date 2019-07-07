package com.wwdablu.soumya.extimageviewdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.camera2.CameraDevice;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.wwdablu.soumya.cam2lib.Cam2Lib;
import com.wwdablu.soumya.cam2lib.Cam2LibCallback;
import com.wwdablu.soumya.extimageview.BaseExtImageView;
import com.wwdablu.soumya.extimageview.rect.CropMode;
import com.wwdablu.soumya.extimageview.rect.ExtRectImageView;
import com.wwdablu.soumya.extimageview.rect.GridMode;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity implements Cam2LibCallback {

    private Cam2Lib cam2Lib;
    private ExtRectImageView extRectImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        extRectImageView = findViewById(R.id.iv_display);
        extRectImageView.setGridColor(Color.GREEN);
        extRectImageView.setGridVisibility(GridMode.ALWAYS);
        extRectImageView.setCropMode(CropMode.RECT);

        cam2Lib = new Cam2Lib(this, this);
        cam2Lib.open(findViewById(R.id.texv_camera), CameraDevice.TEMPLATE_PREVIEW);

        findViewById(R.id.btn_capture).setOnClickListener(v -> cam2Lib.getImage());
    }

    @Override
    public void onReady() {
        cam2Lib.startPreview();
    }

    @Override
    public void onComplete() {
        findViewById(R.id.texv_camera).setVisibility(View.GONE);
        findViewById(R.id.btn_capture).setVisibility(View.GONE);
        findViewById(R.id.iv_display).setVisibility(View.VISIBLE);
    }

    @Override
    public void onImage(Image image) {

        int iw = image.getWidth();
        int ih = image.getHeight();

        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inDensity = 0;
        options.inTargetDensity = 0;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length,null);

        ((ExtRectImageView) findViewById(R.id.iv_display)).setImageBitmap(bitmap);
        ((ExtRectImageView) findViewById(R.id.iv_display)).rotate(BaseExtImageView.Rotate.CW_90);

        cam2Lib.stopPreview();
        cam2Lib.close();
    }

    @Override
    public void onError(Throwable throwable) {
        cam2Lib.stopPreview();
        cam2Lib.close();
    }
}
