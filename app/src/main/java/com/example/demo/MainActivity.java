package com.example.demo;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{

    public static int Width;
    public static int Height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener());
    }

    private class OnSystemUiVisibilityChangeListener implements View.OnSystemUiVisibilityChangeListener {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            Width = getResources().getDisplayMetrics().widthPixels;
            Height = getResources().getDisplayMetrics().heightPixels;
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                Height -= getResources().getDimensionPixelSize(getResources().getIdentifier("status_bar_height", "dimen", "android"));
            Log.d(TAG, "W:"+Width+" H:"+Height);
        }
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

        /*
        if (!MyAccessibilityService.isOnAccessibilityService) {
            Toast.makeText(MainActivity.this, "Accessibility services have not been authorized", Toast.LENGTH_SHORT).show();
            return;
        }
         */

        startService(new Intent(MainActivity.this, MyAccessibilityService.class));
    }

}