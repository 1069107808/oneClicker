package com.example.demo;

import static android.content.ContentValues.TAG;

import android.accessibilityservice.AccessibilityService;

import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.ColorInt;

public class MyAccessibilityService extends AccessibilityService {

    public static boolean isOnAccessibilityService = false;

    private final static int CENTRAL = 0, TOP = 1, BOTTOM = 2, LEFT = 3, RIGHT = 4, INDEX = 5;

    private final static int TEST_CLICK = 25;
    private final static int KEEP_CLICK = 0;

    private boolean isStarted = false;
    private boolean isCreated = false;
    private boolean isRemoved = false;

    private int lastX, lastY;

    private int statusH, screenW, screenH, edge, blank;

    private WindowManager wm;

    private Rect rect;

    private View menu, display, vDisplay;

    private WindowManager.LayoutParams menuLP, displayLP;

    private final View[] interact = new View[INDEX];

    private final WindowManager.LayoutParams[] interactLP = new WindowManager.LayoutParams[INDEX];

    Path path;

    GestureDescription.Builder builder;

    GestureResultCallback callback;

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

        edge = (int) (15 * this.getResources().getDisplayMetrics().density + 0.5f);

        blank = edge / 15;

        menuLP = initParams(-2, -2, type, 0.65f, Gravity.CENTER, 0x00000020);
        menu = View.inflate(this, R.layout.service_menu, null);
        menu.setOnTouchListener(new MenuEvents());
        menu.findViewById(R.id.close).setOnClickListener(view -> close());
        menu.findViewById(R.id.start).setOnClickListener(view -> start());
        menu.findViewById(R.id.pause).setOnClickListener(view -> pause());

        displayLP = initParams(-1, -1, type, 0.85f, Gravity.TOP, 0x00000010);
        display = View.inflate(this, R.layout.service_display, null);
        rect = new Rect(0, 0, 0, 0);
        vDisplay = display.findViewById(R.id.mView);

        interact[CENTRAL] = createView(Color.parseColor("#03A9F4"), CENTRAL);
        interact[TOP] = createView(Color.parseColor("#FFEB3B"), TOP);
        interact[BOTTOM] = createView(Color.parseColor("#673AB7"), BOTTOM);
        interact[LEFT] = createView(Color.parseColor("#F44336"), LEFT);
        interact[RIGHT] = createView(Color.parseColor("#4CAF50"), RIGHT);

        for (int i = 0; i < INDEX; i++) {
            interactLP[i] = initParams(-2, -2, type, 0.65f, Gravity.TOP, 0x00000020);
            interact[i].setOnTouchListener(new InteractEvents());
        }

        path = new Path();

        builder = new GestureDescription.Builder();

        /*
        callback = new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "onCompleted: ");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.d(TAG, "onCancelled: ");
            }
        };
         */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isCreated) {
            initView();
            wm.addView(display, displayLP);
            addView(0);
            wm.addView(menu, menuLP);
            rect.right = screenW - vDisplay.getWidth();
            rect.bottom = screenH - vDisplay.getHeight();

            isCreated = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void initView() {
        menuLP.x = 0;
        menuLP.y = 0;

        final int x = screenW / 4, y = screenH / 4;

        vDisplay.setX(x);
        vDisplay.setY(y * 3);
        vDisplay.getLayoutParams().width = x * 2;
        vDisplay.getLayoutParams().height = y;

        homing();
    }

    private void setLayoutParams(WindowManager.LayoutParams params, View view) {
        setLRParams(params, (int) view.getX(), view.getLayoutParams().width);
        setTBParams(params, (int) view.getY(), view.getLayoutParams().height);
    }

    private void setLayoutParams(WindowManager.LayoutParams params, int x, int y, int width, int height) {
        setLRParams(params, x, width);
        setTBParams(params, y, height);
    }

    private void setLRParams(WindowManager.LayoutParams params, int x, int width) {
        params.x = x;
        params.width = width;
    }

    private void setTBParams(WindowManager.LayoutParams params, int y, int height) {
        params.y = y;
        params.height = height;
    }

    private class MenuEvents implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    menuLP.x += (int) event.getRawX() - lastX;
                    menuLP.y += (int) event.getRawY() - lastY;
                    wm.updateViewLayout(menu, menuLP);
                case MotionEvent.ACTION_DOWN:
                    updateLastTouch((int) event.getRawX(), (int) event.getRawY());
                    break;
                case MotionEvent.ACTION_UP:
                    menuLP.x = Math.max(0, menuLP.x);
                    menuLP.y = Math.max(0, menuLP.y);
                    break;
            }
            return true;
        }
    }

    private void move(int diffX, int diffY) { moveX(diffX); moveY(diffY); }

    private void moveX(int diffX) {
        final int x = (int) vDisplay.getX() + diffX;
        if (x < rect.left) vDisplay.setX(rect.left);
        else vDisplay.setX(Math.min(x, rect.right));
    }

    private void moveY(int diffY) {
        final int y = (int) vDisplay.getY() + diffY;
        if (y < rect.top) vDisplay.setY(rect.top);
        else vDisplay.setY(Math.min(y, rect.bottom));
    }

    private class CentralEvents implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (isStarted) {
                padding(lastX, lastY, edge);
                allowTouchEvents(1);
                updateViewLayout(0);
                isRemoved = true;
            } else {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        move((int) event.getRawX() - lastX, (int) event.getRawY() - lastY);
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        updateLastTouch((int) event.getRawX(), (int) event.getRawY());
                        break;
                }
            }
            return true;
        }
    }

    private class InteractEvents implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (isStarted) {
                updateLastTouch((int) event.getRawX(), (int) event.getRawY());
                if (inBox(vDisplay, lastX, lastY)) padding(lastX, lastY, edge);
                else homing();
                updateViewLayout(1);
                //click(lastX, lastY, TEST_CLICK)
            } else {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        updateDisplay(v.getId(), (int) event.getRawX(), (int) event.getRawY());
                    case MotionEvent.ACTION_DOWN:
                        updateLastTouch((int) event.getRawX(), (int) event.getRawY());
                        break;
                    case MotionEvent.ACTION_UP:
                        updateInteractLayer();
                        break;
                }
            }
            return true;
        }
    }

    private void start() {
        menu.findViewById(R.id.start).setVisibility(View.INVISIBLE);
        menu.findViewById(R.id.pause).setVisibility(View.VISIBLE);

        blockTouchEvents(1);
        updateViewLayout(0);
        isStarted = true;
    }

    private void pause() {
        menu.findViewById(R.id.pause).setVisibility(View.INVISIBLE);
        menu.findViewById(R.id.start).setVisibility(View.VISIBLE);

        if (isRemoved) {
            homing();
            interactLP[CENTRAL].flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            updateViewLayout(0);
        } else {
            allowTouchEvents(1);
            updateViewLayout(1);
        }

        isStarted = false;
        isRemoved = false;
    }

    private void close() {
        menu.findViewById(R.id.pause).setVisibility(View.INVISIBLE);
        menu.findViewById(R.id.start).setVisibility(View.VISIBLE);

        setTouchable(0);

        removeView(0);
        wm.removeView(display);
        wm.removeView(menu);

        isStarted = false;
        isRemoved = false;
        isCreated = false;
    }

    private void updateDisplay(int id, int nowX, int nowY) {
        switch (id) {
            case CENTRAL:
                move(nowX - lastX, nowY - lastY);
                break;
            case TOP:
                resizeTop(nowY - lastY);
                break;
            case BOTTOM:
                resizeBottom(nowY - lastY);
                break;
            case LEFT:
                resizeLeft(nowX - lastX);
                break;
            case RIGHT:
                resizeRight(nowX - lastX);
                break;
        }
        wm.updateViewLayout(display, displayLP);
    }

    private void updateLastTouch(int x, int y) { lastX = x; lastY = y;}

    private void updateInteractLayer() { homing(); updateViewLayout(0); }

    private int mCount = 0;
    private void click(int x, int y, int count) {
        path.moveTo(x, y);
        GestureDescription gesture = builder.addStroke(new GestureDescription.StrokeDescription(path, 500, 300)).build();

        while (mCount <= count + 1) {
            mCount++;
            dispatchGesture(gesture, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d(TAG, "onCompleted: "+ mCount);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    Log.d(TAG, "onCancelled: "+ mCount);
                }
            }, null);
        }
        mCount = 0;
    }

    private View createView(@ColorInt int color, int id) {
        View view = new View(this);
        view.setBackgroundColor(color);
        view.setId(id);
        return view;
    }

    private void addView(int flag) { for (int i = flag; i < INDEX; i++) wm.addView(interact[i], interactLP[i]); }

    private void updateViewLayout(int flag) { for (int i = flag; i < INDEX; i++) wm.updateViewLayout(interact[i], interactLP[i]); }

    private void removeView(int flag) { for (int i = flag; i < INDEX; i++) wm.removeView(interact[i]); }

    private void setTouchable(int flag) { for (int i = flag; i < INDEX; i++) interactLP[i].flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL; }

    private void allowTouchEvents(int flag) {
        if (flag == 1) interactLP[CENTRAL].flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        for (int i = flag; i < INDEX; i++)
            interactLP[i].flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
    }

    private void blockTouchEvents(int flag) {
        if (flag == 1) interactLP[CENTRAL].flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        for (int i = flag; i < INDEX; i++)
            interactLP[i].flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    }

    private void resizeTop(int diffY) {
        vDisplay.getLayoutParams().height -= diffY;
        rect.bottom = display.getHeight() - vDisplay.getHeight();
        moveY(diffY);
    }

    private void resizeBottom(int diffY) {
        vDisplay.getLayoutParams().height += diffY;
        rect.bottom = display.getHeight() - vDisplay.getHeight();
    }

    private void resizeLeft(int diffX) {
        vDisplay.getLayoutParams().width -= diffX;
        rect.right = display.getWidth() - vDisplay.getWidth();
        moveX(diffX);
    }

    private void resizeRight(int diffX) {
        vDisplay.getLayoutParams().width += diffX;
        rect.right = display.getWidth() - vDisplay.getWidth();
    }

    private void homing() {
        if (isRemoved) {
            isRemoved = false;
            blockTouchEvents(1);
            wm.updateViewLayout(interact[CENTRAL], interactLP[CENTRAL]);
        }

        setLayoutParams(interactLP[CENTRAL], vDisplay);
        setLayoutParams(interactLP[TOP], interactLP[CENTRAL].x, interactLP[CENTRAL].y, interactLP[CENTRAL].width, edge);
        setLayoutParams(interactLP[BOTTOM], interactLP[CENTRAL].x, interactLP[CENTRAL].y + interactLP[CENTRAL].height - edge, interactLP[CENTRAL].width, edge);
        setLayoutParams(interactLP[LEFT], interactLP[CENTRAL].x, interactLP[CENTRAL].y, edge, interactLP[CENTRAL].height);
        setLayoutParams(interactLP[RIGHT], interactLP[CENTRAL].x + interactLP[CENTRAL].width - edge, interactLP[CENTRAL].y, edge, interactLP[CENTRAL].height);
    }

    private void padding(int x, int y, int interval) {
        if (!isRemoved) {
            isRemoved = true;
            allowTouchEvents(1);
            wm.updateViewLayout(interact[CENTRAL], interactLP[CENTRAL]);

            interactLP[TOP].y = 0;
            setLRParams(interactLP[TOP], 0, display.getWidth());
            setLRParams(interactLP[BOTTOM], 0, display.getWidth());

            interactLP[LEFT].x = 0;
            setTBParams(interactLP[LEFT], 0, display.getHeight());
            setTBParams(interactLP[RIGHT], 0, display.getHeight());
        }

        final int h = display.getHeight() == screenH ? statusH : 0;
        interactLP[TOP].height = y - h - interval;
        setTBParams(interactLP[BOTTOM], y - h + interval, display.getHeight() - interactLP[BOTTOM].y);

        interactLP[LEFT].width = x - interval;
        setLRParams(interactLP[RIGHT], x + interval, display.getWidth() - interactLP[RIGHT].x);
    }

    private boolean inBox(View view, int x, int y) {
        final int l = (int) view.getX(), r = l + view.getWidth(), t = (int) view.getY(), b = t + view.getHeight();
        return x > l && x < r && y > t && y < b;
    }

    private WindowManager.LayoutParams initParams(int w, int h, int type, float alpha, int gravity, int flag) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(w, h, type, flag, 1);
        layoutParams.gravity = Gravity.START | gravity;
        layoutParams.alpha = alpha;
        return layoutParams;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        isOnAccessibilityService = true;
        Toast.makeText(this, "Authorized Accessibility Services", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override
    public void onDestroy() { super.onDestroy(); }
    @Override
    public void onInterrupt() {}
}
