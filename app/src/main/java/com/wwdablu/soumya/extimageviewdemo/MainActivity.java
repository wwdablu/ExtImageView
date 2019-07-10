package com.wwdablu.soumya.extimageviewdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.Toast;

import com.wwdablu.soumya.extimageview.Result;
import com.wwdablu.soumya.extimageview.rect.CropMode;
import com.wwdablu.soumya.extimageview.rect.ExtRectImageView;
import com.wwdablu.soumya.extimageview.rect.GridMode;

public class MainActivity extends AppCompatActivity {

    private ExtRectImageView extRectImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        extRectImageView = findViewById(R.id.iv_display);
        extRectImageView.setGridColor(Color.GREEN);
        extRectImageView.setGridVisibility(GridMode.ALWAYS);
        extRectImageView.setCropMode(CropMode.RECT);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inDensity = 0;
        options.inTargetDensity = 0;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample);

        ((ExtRectImageView) findViewById(R.id.iv_display)).setImageBitmap(bitmap);

        findViewById(R.id.btn_capture).setOnClickListener(v -> {
            extRectImageView.crop(new Result<Void>() {
                @Override
                public void onComplete(Void data) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Crop completed", Toast.LENGTH_SHORT).show();

                        findViewById(R.id.iv_display).setVisibility(View.GONE);
                        findViewById(R.id.iv_display_cropped).setVisibility(View.VISIBLE);

                        extRectImageView.getCroppedBitmap(new Result<Bitmap>() {
                            @Override
                            public void onComplete(Bitmap data) {
                                runOnUiThread(() -> {

                                    Bitmap d = extRectImageView.scaleToFit(data, extRectImageView
                                            .getMeasuredWidth(), extRectImageView.getMeasuredHeight());

                                    data.recycle();
                                    ((AppCompatImageView) findViewById(R.id.iv_display_cropped))
                                            .setImageBitmap(d);
                                });
                            }

                            @Override
                            public void onError(Throwable throwable) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                        "Could not get cropped bitmap" + throwable.getMessage(),
                                        Toast.LENGTH_SHORT).show());
                            }
                        });
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,
                            "Crop failed" + throwable.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
