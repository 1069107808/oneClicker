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

    private int lastX, lastY;

    private int statusH, screenW, screenH, limit;
    private WindowManager manager = null;

    private View rect = null;
    private ViewGroup.LayoutParams rectParams = null;

    private View consoleView = null;
    private WindowManager.LayoutParams consoleParams = null;

    private View window1 = null;
    private WindowManager.LayoutParams windowParams1 = null;

    private View window2 = null;
    private WindowManager.LayoutParams windowParams2 = null;

    private TextView A1, B1, C1, D1;

    @Override
    public void onCreate() {
        super.onCreate();

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        manager = (WindowManager) getSystemService(WINDOW_SERVICE);

        statusH = this.getResources().getDimensionPixelSize(
                       getResources().getIdentifier("status_bar_height", "dimen", "android"));
        screenW = this.getResources().getDisplayMetrics().widthPixels;
        screenH = this.getResources().getDisplayMetrics().heightPixels - statusH;
        limit = screenW / 6;
        edge = (int) (15 * this.getResources().getDisplayMetrics().density + 0.5f);

        consoleParams = initParams(-2, -2, type, 0.65f, 0x00000020 | 0x00040000);
        windowParams1 = initParams(-1, -1, type, 0.85f, 0x00000010);
        windowParams2 = initParams(-2, -2, type, 0.85f, 0x00000008);

        createConsole();
        createWindow1();
        createWindow2();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        manager.addView(window1, windowParams1);
        manager.addView(window2, windowParams2);
        manager.addView(consoleView, consoleParams);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        isOnAccessibilityService = true;
        Toast.makeText(this, "已授权无障碍服务", Toast.LENGTH_SHORT).show();
    }

    private WindowManager.LayoutParams initParams(int w, int h, int type, float alpha, int _flags) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(w, h, 0, 0, type, _flags, 1);
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.alpha = alpha;
        return layoutParams;
    }

    private void createConsole() {
        consoleView = View.inflate(this, R.layout.console, null);
        consoleView.findViewById(R.id.close).setOnClickListener(view -> close());
        consoleView.findViewById(R.id.start).setOnClickListener(view -> start());
        consoleView.findViewById(R.id.pause).setOnClickListener(view -> pause());
        consoleView.setOnTouchListener(new touchOnMenu());
    }

    private void createWindow1() {
        window1 = View.inflate(this, R.layout.layer1, null);
        rect = window1.findViewById(R.id.myView);
        A1 = window1.findViewById(R.id.A);
        B1 = window1.findViewById(R.id.B);
        C1 = window1.findViewById(R.id.C);
        D1 = window1.findViewById(R.id.D);
        rectParams = rect.getLayoutParams();
    }

    private void createWindow2() {
        window2 = View.inflate(this, R.layout.layer2, null);
        window2.setOnTouchListener(new touchOnView());
    }

    private void init() {
        consoleView.findViewById(R.id.pause).setVisibility(View.INVISIBLE);
        consoleView.findViewById(R.id.start).setVisibility(View.VISIBLE);

        consoleParams.x = 0;
        consoleParams.y = screenH / 2;

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
                    consoleParams.x += (int) motionEvent.getRawX() - lastX;
                    consoleParams.y += (int) motionEvent.getRawY() - lastY;
                    manager.updateViewLayout(consoleView, consoleParams);
                default:
                    updateLastXY((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                    break;
                case MotionEvent.ACTION_OUTSIDE:
                    updateLastXY((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                    Log.d(TAG, "x:"+ lastX +" y:"+ lastY);
                    if (isStarted && inBox(lastX, lastY)) click(lastX, lastY);
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
                    final int offsetX = (int) motionEvent.getRawX() - lastX;
                    final int offsetY = (int) motionEvent.getRawY() - lastY;
                    switch (event) {
                        case 1:
                            updateHeight(-1, offsetY);
                            moveY((int) rect.getY() + offsetY);
                            break;
                        case 2:
                            updateHeight(1, offsetY);
                            break;
                        case 3:
                            updateWidth(-1, offsetX);
                            moveX((int) rect.getX() + offsetX);
                            break;
                        case 4:
                            updateWidth(1, offsetX);
                            break;
                        case 0:
                            move((int) rect.getX() + offsetX, (int) rect.getY() + offsetY);
                            break;
                    }
                    showLocation();
                    manager.updateViewLayout(window1, windowParams1);
                    break;
                case MotionEvent.ACTION_UP:
                    setParams((int) rect.getX(), (int) rect.getY(), rectParams.height, rectParams.width);
                    manager.updateViewLayout(window2, windowParams2);
                    break;
            }
            updateLastXY((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
            return true;
        }

        private void updateWidth(int flag, int offsetX) {
            rectParams.width += flag * offsetX;
            rectParams.width = Math.max(rectParams.width, limit);
            rectParams.width = Math.min(rectParams.width, screenW);
        }

        private void updateHeight(int flag, int offsetY) {
            rectParams.height += flag * offsetY;
            rectParams.height = Math.max(rectParams.height, limit);
            rectParams.height = Math.min(rectParams.height, screenH);
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
            if      (lastY <= rectY + edge) return 1;
            else if (lastY >= rectY + rectParams.height - edge) return 2;
            else if (lastX <= rectX + edge) return 3;
            else if (lastX >= rectX + rectParams.width - edge) return 4;
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
        windowParams2.x = x;
        windowParams2.y = y;
        windowParams2.height = height;
        windowParams2.width = width;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
    }

    private void start() {
        consoleView.findViewById(R.id.start).setVisibility(View.INVISIBLE);
        consoleView.findViewById(R.id.pause).setVisibility(View.VISIBLE);
        isStarted = true;

        windowParams2.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        manager.updateViewLayout(window2, windowParams2);
    }

    private void pause() {
        consoleView.findViewById(R.id.pause).setVisibility(View.INVISIBLE);
        consoleView.findViewById(R.id.start).setVisibility(View.VISIBLE);
        isStarted = false;

        windowParams2.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        manager.updateViewLayout(window2, windowParams2);
    }

    private void close() {
        manager.removeView(window1);
        manager.removeView(window2);
        manager.removeView(consoleView);
    }

    private void updateLastXY(int x, int y) { lastX = x; lastY = y;}

    private boolean inBox(int x, int y) {
        return x > windowParams2.x && x < windowParams2.x + windowParams2.width && y > windowParams2.y&& y < windowParams2.y + windowParams2.height;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
