package com.wwdablu.soumya.extimageviewdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selection);

        findViewById(R.id.btn_rect).setOnClickListener(v -> launch("rect"));
        findViewById(R.id.btn_free).setOnClickListener(v -> launch("free"));
        findViewById(R.id.btn_trapez).setOnClickListener(v -> launch("trapez"));
    }

    private void launch(String mode) {

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }
}
