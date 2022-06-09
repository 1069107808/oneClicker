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

    private final static int RESIZE_TOP    = 1;
    private final static int RESIZE_BOTTOM = 2;
    private final static int RESIZE_LEFT   = 3;
    private final static int RESIZE_RIGHT  = 4;
    private final static int MOVE_WINDOW   = 0;

    public static boolean isOnAccessibilityService = false;

    private boolean isCreated = false;
    private boolean isStarted = false;
    private boolean isTesting = true;

    private int action, edge;

    private int lastX, lastY;

    private int statusH, screenW, screenH, limit;

    private WindowManager wm = null;

    private View rect = null;
    private View window1 = null;
    private View window2 = null;
    private View consoleView = null;

    private ViewGroup.LayoutParams rectParams = null;
    private WindowManager.LayoutParams windowParams1 = null;
    private WindowManager.LayoutParams windowParams2 = null;
    private WindowManager.LayoutParams consoleParams = null;

    private TextView A1, B1, C1, D1;

    @Override
    public void onCreate() {
        super.onCreate();

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        statusH = this.getResources().getDimensionPixelSize(
                getResources().getIdentifier("status_bar_height", "dimen", "android"));
        screenW = this.getResources().getDisplayMetrics().widthPixels;
        screenH = this.getResources().getDisplayMetrics().heightPixels - statusH;
        limit = screenW / 20;
        edge = (int) (15 * this.getResources().getDisplayMetrics().density + 0.5f);

        Log.d(TAG, "onCreate: "+screenW+"x"+screenH);

        consoleParams = initParams(-2, -2, type, 0.65f, 0x00000020 | 0x00040000);
        windowParams1 = initParams(-1, -1, type, 0.85f, 0x00000010);
        windowParams2 = initParams(-2, -2, type, 0.85f, 0x00000008);

        createConsole();
        createWindow1();
        createWindow2();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isCreated) {
            isCreated = true;
            init();
            wm.addView(window1, windowParams1);
            wm.addView(window2, windowParams2);
            wm.addView(consoleView, consoleParams);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        isOnAccessibilityService = true;
        Toast.makeText(this, "已授权无障碍服务", Toast.LENGTH_SHORT).show();
    }

    private WindowManager.LayoutParams initParams(int w, int h, int type, float alpha, int _flags) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(w, h, type, _flags, 1);
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.alpha = alpha;
        return layoutParams;
    }

    private void createConsole() {
        consoleView = View.inflate(this, R.layout.console, null);
        consoleView.setOnTouchListener(new touchOnMenu());
        consoleView.findViewById(R.id.close).setOnClickListener(view -> close());
        consoleView.findViewById(R.id.start).setOnClickListener(view -> start());
        consoleView.findViewById(R.id.pause).setOnClickListener(view -> pause());
    }

    private void createWindow1() {
        window1 = View.inflate(this, R.layout.layer1, null);
        rect = window1.findViewById(R.id.myView);
        rectParams = rect.getLayoutParams();
        A1 = window1.findViewById(R.id.A);
        B1 = window1.findViewById(R.id.B);
        C1 = window1.findViewById(R.id.C);
        D1 = window1.findViewById(R.id.D);
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
        if (isTesting) showLocation();
    }

    private class touchOnMenu implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    consoleParams.x += (int) motionEvent.getRawX() - lastX;
                    consoleParams.y += (int) motionEvent.getRawY() - lastY;
                    wm.updateViewLayout(consoleView, consoleParams);
                default:
                    updateLastTouch((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                    break;
                case MotionEvent.ACTION_OUTSIDE:
                    updateLastTouch((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
                    Log.d(TAG, "x:"+ lastX +" y:"+ lastY);
                    if (isStarted && inBox(lastX, lastY)) click(lastX, lastY);
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
                    action = getAction();
                    break;
                case MotionEvent.ACTION_MOVE:
                    final int offsetX = (int) motionEvent.getRawX() - lastX;
                    final int offsetY = (int) motionEvent.getRawY() - lastY;
                    switch (action) {
                        case RESIZE_TOP:
                            resizeUp(offsetY);
                            break;
                        case RESIZE_BOTTOM:
                            resizeDown(offsetY);
                            break;
                        case RESIZE_LEFT:
                            resizeLeft(offsetX);
                            break;
                        case RESIZE_RIGHT:
                            resizeRight(offsetX);
                            break;
                        case MOVE_WINDOW:
                            moveTo((int) rect.getX() + offsetX, (int) rect.getY() + offsetY);
                            break;
                    }
                    if (isTesting) showLocation();
                    if (rectParams.width <= limit || rectParams.height <= limit) maximize();
                    wm.updateViewLayout(window1, windowParams1);
                    break;
                case MotionEvent.ACTION_UP:
                    setParams((int) rect.getX(), (int) rect.getY(), rectParams.height, rectParams.width);
                    wm.updateViewLayout(window2, windowParams2);
                    break;
            }
            updateLastTouch((int) motionEvent.getRawX(), (int) motionEvent.getRawY());
            return true;
        }

        private void maximize() {
            rect.setX(0);
            rect.setY(0);
            rectParams.width = screenW;
            rectParams.height = screenH;
        }

        private void resizeUp(int offsetY) {
            rectParams.height -= offsetY;
            moveToY((int) rect.getY() + offsetY);
        }

        private void resizeDown(int offsetY) {
            rectParams.height += offsetY;
        }

        private void resizeLeft(int offsetX) {
            rectParams.width -= offsetX;
            moveToX((int) rect.getX() + offsetX);
        }

        private void resizeRight(int offsetX) {
            rectParams.width += offsetX;
        }

        private void moveTo(int x, int y) { moveToX(x); moveToY(y); }

        private void moveToX(int x) {
            if (x < 0) rect.setX(0);
            else rect.setX(Math.min(x, screenW - rectParams.width));
        }

        private void moveToY(int y) {
            if (y < 0) rect.setY(0);
            else rect.setY(Math.min(y, screenH - rectParams.height));
        }

        private int getAction() {
            final int rectX = (int) rect.getX(), rectY = (int) rect.getY() + statusH;
            if      (lastY <= rectY + edge)                     return RESIZE_TOP;
            else if (lastY >= rectY + rectParams.height - edge) return RESIZE_BOTTOM;
            else if (lastX <= rectX + edge)                     return RESIZE_LEFT;
            else if (lastX >= rectX + rectParams.width - edge)  return RESIZE_RIGHT;
            else                                                return MOVE_WINDOW;
        }
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

    private void setParams(int x, int y, int height, int width) {
        windowParams2.x = x;
        windowParams2.y = y;
        windowParams2.height = height;
        windowParams2.width = width;
    }

    private void start() {
        consoleView.findViewById(R.id.start).setVisibility(View.INVISIBLE);
        consoleView.findViewById(R.id.pause).setVisibility(View.VISIBLE);
        isStarted = true;

        windowParams2.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        wm.updateViewLayout(window2, windowParams2);
    }

    private void pause() {
        consoleView.findViewById(R.id.pause).setVisibility(View.INVISIBLE);
        consoleView.findViewById(R.id.start).setVisibility(View.VISIBLE);
        isStarted = false;

        windowParams2.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        wm.updateViewLayout(window2, windowParams2);
    }

    private void close() {
        isCreated = false;
        wm.removeView(window1);
        wm.removeView(window2);
        wm.removeView(consoleView);
    }

    private void updateLastTouch(int x, int y) { lastX = x; lastY = y;}

    private boolean inBox(int x, int y) {
        return x > windowParams2.x && x < windowParams2.x + windowParams2.width && y > windowParams2.y&& y < windowParams2.y + windowParams2.height;
    }

    @Override
    public void onDestroy() { super.onDestroy(); }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
