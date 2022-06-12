package com.example.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openAccessibilitySettings(View view) {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    public void openOverLaySettings(View view) {
        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).setData(Uri.parse("package:" + getPackageName())));
    }

    public void openFloatingWindow(View view) {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(MainActivity.this, "Overlay permission has not been authorized", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!myAccessibilityService.isOnAccessibilityService) {
            Toast.makeText(MainActivity.this, "Accessibility services have not been authorized", Toast.LENGTH_SHORT).show();
            return;
        }

        startService(new Intent(MainActivity.this, myAccessibilityService.class));
    }

}