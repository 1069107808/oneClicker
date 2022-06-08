package com.example.demo;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.accessibilityservice.AccessibilityService;

import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toast;

public class myAccessibilityService extends AccessibilityService {

    public static boolean isOnAccessibilityService = false;

    private boolean isStarted = false;

    private int event, edge;

    private int nowX, nowY;

    private int statusH, screenW, screenH;
    private WindowManager window = null;

    private View menu = null;
    private WindowManager.LayoutParams menuParams = null;

    private View rect = null;
    private ViewGroup.LayoutParams rectParams = null;

    private View layer1 = null;
    private WindowManager.LayoutParams params1 = null;

    private View layer2 = null;
    private WindowManager.LayoutParams params2 = null;

    private TextView A1, B1, C1, D1;

    @Override
    public void onCreate() {
        super.onCreate();

        statusH = this.getResources().getDimensionPixelSize(
                       getResources().getIdentifier("status_bar_height", "dimen", "android"));
        screenW = this.getResources().getDisplayMetrics().widthPixels;
        screenH = this.getResources().getDisplayMetrics().heightPixels - statusH;

        edge = (int) (15 * this.getResources().getDisplayMetrics().density + 0.5f);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        if (window == null)
            window = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (menu == null)
            createMenu(type);

        if (layer1 == null)
            createLayout1(type);

        if (layer2 == null)
            createLayout2(type);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        window.addView(layer1, params1);
        window.addView(layer2, params2);
        window.addView(menu, menuParams);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        isOnAccessibilityService = true;
        Toast.makeText(this, "已授权无障碍服务", Toast.LENGTH_SHORT).show();
    }

    private void createMenu(int type) {
        menu = View.inflate(this, R.layout.menu, null);
        menu.findViewById(R.id.close).setOnClickListener(view -> close());
        menu.findViewById(R.id.start).setOnClickListener(view -> start());
        menu.findViewById(R.id.stop).setOnClickListener(view -> stop());
        menu.setOnTouchListener(new touchOnMenu());
        menuParams = new WindowManager.LayoutParams(-2, -2, 0, 0, type, 0x00000020 | 0x00040000, 1);
        menuParams.gravity = Gravity.START | Gravity.TOP;
        menuParams.alpha = 0.625f;
    }

    private void createLayout1(int type) {
        layer1 = View.inflate(this, R.layout.layer1, null);
        rect = layer1.findViewById(R.id.myView);

        A1 = layer1.findViewById(R.id.A);
        B1 = layer1.findViewById(R.id.B);
        C1 = layer1.findViewById(R.id.C);
        D1 = layer1.findViewById(R.id.D);

        rectParams = rect.getLayoutParams();
        params1 = new WindowManager.LayoutParams(-1, -1, 0, 0, type, 0x00000010, 1);
        params1.gravity = Gravity.START | Gravity.TOP;
        params1.alpha = 0.85f;
    }

    private void createLayout2(int type) {
        layer2 = View.inflate(this, R.layout.layer2, null);
        layer2.setOnTouchListener(new touchOnView());
        params2 = new WindowManager.LayoutParams(-2, -2, 0, 0, type, 0x00000008, 1);
        params2.gravity = Gravity.START | Gravity.TOP;
        params2.alpha = 0.85f;
    }

    private void init() {
        menu.findViewById(R.id.stop).setVisibility(View.INVISIBLE);
        menu.findViewById(R.id.start).setVisibility(View.VISIBLE);

        menuParams.x = 0;
        menuParams.y = screenH / 2;

        rectParams.width = screenW / 2;
        rectParams.height = screenH / 3;
        rect.setX((float) rectParams.width / 2);
        rect.setY((float) rectParams.height * 2);

        setParams((int) rect.getX(), (int) rect.getY(), rectParams.height, rectParams.width);
    }

    private class touchOnMenu implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    menuParams.x += (int) motionEvent.getRawX() - nowX;
                    menuParams.y += (int) motionEvent.getRawY() - nowY;
                    window.updateViewLayout(menu, menuParams);
                default:
                    updateNowXY((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                    break;
                case MotionEvent.ACTION_OUTSIDE:
                    updateNowXY((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                    Log.d(TAG, "x:"+nowX+" y:"+nowY);
                    if (isStarted && inBox(nowX, nowY)) click(nowX, nowY);
                    break;
            }
            return true;
        }
    }

    private void click(int x, int y) {
        final Path path = new Path();
        path.moveTo(x, y);
        dispatchGesture(new GestureDescription.Builder().
                addStroke(new GestureDescription.StrokeDescription(path, 0, 100)).build(),new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) { super.onCompleted(gestureDescription);}
            @Override
            public void onCancelled(GestureDescription gestureDescription) { super.onCancelled(gestureDescription);}}, null);
    }

    private class touchOnView implements View.OnTouchListener {
        @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    event = getEvent();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int offsetX = (int) motionEvent.getRawX() - nowX;
                    int offsetY = (int) motionEvent.getRawY() - nowY;
                    switch (event) {
                        case 1:
                            rectParams.height -= offsetY;
                            moveY((int) rect.getY() + offsetY);
                            break;
                        case 2:
                            rectParams.height += offsetY;
                            break;
                        case 3:
                            rectParams.width -= offsetX;
                            moveX((int) rect.getX() + offsetX);
                            break;
                        case 4:
                            rectParams.width += offsetX;
                            break;
                        case 0:
                            move((int) rect.getX() + offsetX, (int) rect.getY() + offsetY);
                            break;
                    }

                    showLocation();

                    window.updateViewLayout(layer1, params1);
                    break;
                case MotionEvent.ACTION_UP:
                    setParams((int) rect.getX(), (int) rect.getY(), rectParams.height, rectParams.width);
                    window.updateViewLayout(layer2, params2);
                    break;
            }
            updateNowXY((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
            return true;
        }

        private void move(int x, int y) { moveX(x); moveY(y);}

        private void moveX(int x) {
            if (x < 0) rect.setX(0);
            else rect.setX(Math.min(x, screenW - rectParams.width));
        }

        private void moveY(int y) {
            if (y < 0) rect.setY(0);
            else rect.setY(Math.min(y, screenH - rectParams.height));
        }

        private int getEvent() {
            final int rectX = (int) rect.getX(), rectY = (int) rect.getY() + statusH;
            if      (nowY <= rectY + edge) return 1;
            else if (nowY >= rectY + rectParams.height - edge) return 2;
            else if (nowX <= rectX + edge) return 3;
            else if (nowX >= rectX + rectParams.width - edge) return 4;
            else return 0;
        }

        @SuppressLint("SetTextI18n")
        private void showLocation() {
            int x1 = (int) rect.getX();
            int x2 = x1 + rectParams.width;
            int y1 = (int) rect.getY();
            int y2 = y1 + rectParams.height;

            A1.setText(" "+ x1+","+(y1+statusH));
            B1.setText(x2+","+(y1+statusH)+" ");
            C1.setText(" "+x1+","+(y2+statusH));
            D1.setText(x2+","+(y2+statusH)+" ");

            A1.setX(x1);
            A1.setY(y1);

            B1.setX(x2 - B1.getWidth());
            B1.setY(y1);

            C1.setX(x1);
            C1.setY(y2 - C1.getHeight());

            D1.setX(x2 - D1.getWidth());
            D1.setY(C1.getY());
        }
    }

    private void setParams(int x, int y, int height, int width) {
        params2.x = x;
        params2.y = y;
        params2.height = height;
        params2.width = width;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
    }

    private void start() {
        menu.findViewById(R.id.start).setVisibility(View.INVISIBLE);
        menu.findViewById(R.id.stop).setVisibility(View.VISIBLE);
        isStarted = true;

        params2.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        window.updateViewLayout(layer2, params2);
    }

    private void stop() {
        menu.findViewById(R.id.stop).setVisibility(View.INVISIBLE);
        menu.findViewById(R.id.start).setVisibility(View.VISIBLE);
        isStarted = false;

        params2.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        window.updateViewLayout(layer2, params2);
    }

    private void close() {
        window.removeView(layer1);
        window.removeView(layer2);
        window.removeView(menu);
    }

    private void updateNowXY(int x, int y) { nowX = x; nowY = y;}

    private boolean inBox(int x, int y) {
        return x > params2.x && x < params2.x + params2.width && y > params2.y&& y < params2.y + params2.height;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
