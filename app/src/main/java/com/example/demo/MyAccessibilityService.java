package com.example.demo;

import android.accessibilityservice.AccessibilityService;

import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toast;

public class MyAccessibilityService extends AccessibilityService {

    public static boolean isOnAccessibilityService = false;
    public static boolean isWindowCreated = false;

    //private final static int LEFT = 0, RIGHT = 1, TOP = 2, BOTTOM = 3;

    private final static int TEST_CLICK = 5;
    private final static int KEEP_CLICK = 0;
    private boolean isStarted = false;
    private boolean isTesting = true;

    private int lastX, lastY;

    private int statusH, screenW, screenH, edge, blank;

    private WindowManager wm;

    private TextView LT, RT, LB, RB;

    private View lDisplay, vDisplay;

    private Rect rDisplay;

    private View lInteract, vInteract, vEdgeL, vEdgeR, vEdgeT, vEdgeB;

    private View lMenu;

    private WindowManager.LayoutParams lpDisplay, lpInteract, lpMenu;

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

        lpMenu = initParams(-2, -2, type, 0.65f, Gravity.START | Gravity.CENTER, 0x00000020);
        lMenu = View.inflate(this, R.layout.service_menu, null);
        lMenu.setOnTouchListener(new MenuEvents());
        lMenu.findViewById(R.id.close).setOnClickListener(view -> close());
        lMenu.findViewById(R.id.start).setOnClickListener(view -> start());
        lMenu.findViewById(R.id.pause).setOnClickListener(view -> pause());

        lpDisplay = initParams(-1, -1, type, 0.85f, Gravity.START | Gravity.TOP, 0x00000010);
        lDisplay = View.inflate(this, R.layout.service_display, null);
        rDisplay = new Rect(0, 0, 0, 0);
        vDisplay = lDisplay.findViewById(R.id.mView);
        LT = lDisplay.findViewById(R.id.LT);
        LB = lDisplay.findViewById(R.id.LB);
        RT = lDisplay.findViewById(R.id.RT);
        RB = lDisplay.findViewById(R.id.RB);

        lpInteract = initParams(-2, -2, type, 0.65f, Gravity.START | Gravity.TOP, 0x00000020 | 0x00040000);
        lInteract = View.inflate(this, R.layout.service_interact, null);

        vInteract = lInteract.findViewById(R.id.mView);
        vInteract.setOnTouchListener(new InteractEvents());

        vEdgeL = lInteract.findViewById(R.id.left);
        vEdgeL.setOnTouchListener(new EdgeLeftEvents());

        vEdgeR = lInteract.findViewById(R.id.right);
        vEdgeR.setOnTouchListener(new EdgeRightEvents());

        vEdgeT = lInteract.findViewById(R.id.top);
        vEdgeT.setOnTouchListener(new EdgeTopEvents());

        vEdgeB = lInteract.findViewById(R.id.bottom);
        vEdgeB.setOnTouchListener(new EdgeBottomEvents());
    }

    private void padding(int x, int y) {
        vEdgeT.getLayoutParams().height = y - lpInteract.y - statusH - edge;
        vEdgeB.getLayoutParams().height = lpInteract.height - vEdgeT.getLayoutParams().height - 2 * edge;
        vEdgeL.getLayoutParams().width = x - lpInteract.x - edge;
        vEdgeR.getLayoutParams().width = lpInteract.width - vEdgeL.getLayoutParams().width - 2 * edge;
        wm.updateViewLayout(lInteract, lpInteract);
    }

    private class EdgeTopEvents implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (isStarted) {
                padding((int) event.getRawX(), (int) event.getRawY());
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE)
                resizeTop((int) event.getRawY() - lastY);
            interactEvent(event.getAction(), (int) event.getRawX(), (int) event.getRawY());
            return true;
        }
    }

    private class EdgeBottomEvents implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (isStarted) {
                padding((int) event.getRawX(), (int) event.getRawY());
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE)
                resizeBottom((int) event.getRawY() - lastY);
            interactEvent(event.getAction(), (int) event.getRawX(), (int) event.getRawY());
            return true;
        }
    }

    private class EdgeLeftEvents implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (isStarted) {
                padding((int) event.getRawX(), (int) event.getRawY());
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE)
                resizeLeft((int) event.getRawX() - lastX);
            interactEvent(event.getAction(), (int) event.getRawX(), (int) event.getRawY());
            return true;
        }
    }

    private class EdgeRightEvents implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (isStarted) {
                padding((int) event.getRawX(), (int) event.getRawY());
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE)
                resizeRight((int) event.getRawX() - lastX);
            interactEvent(event.getAction(), (int) event.getRawX(), (int) event.getRawY());
            return true;
        }
    }

    private void interactEvent(int action, int x, int y) {
        if (isTesting) showLocation();
        wm.updateViewLayout(lDisplay, lpDisplay);
        if (action == MotionEvent.ACTION_UP) updateInteractLayer();
        else updateLastTouch(x, y);
    }

    private void updateInteractLayer() {
        syncParams(vDisplay, lpInteract);
        wm.updateViewLayout(lInteract, lpInteract);
    }

    private void resizeTop(int diffY) {
        vDisplay.getLayoutParams().height -= diffY;
        rDisplay.bottom = screenH - vDisplay.getLayoutParams().height;
        moveY(diffY);
    }

    private void resizeBottom(int diffY) {
        vDisplay.getLayoutParams().height += diffY;
        rDisplay.bottom = screenH - vDisplay.getLayoutParams().height;
    }

    private void resizeLeft(int diffX) {
        vDisplay.getLayoutParams().width -= diffX;
        rDisplay.right = screenW - vDisplay.getLayoutParams().width;
        moveX(diffX);
    }

    private void resizeRight(int diffX) {
        vDisplay.getLayoutParams().width += diffX;
        rDisplay.right = screenW - vDisplay.getLayoutParams().width;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isWindowCreated = true;
        initView();
        wm.addView(lDisplay, lpDisplay);
        wm.addView(lInteract, lpInteract);
        wm.addView(lMenu, lpMenu);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        isOnAccessibilityService = true;
        Toast.makeText(this, "Authorized Accessibility Services", Toast.LENGTH_SHORT).show();
    }

    private WindowManager.LayoutParams initParams(int w, int h, int type, float alpha, int gravity, int flag) {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(w, h, type, flag, 1);
        layoutParams.gravity = gravity;
        layoutParams.alpha = alpha;
        return layoutParams;
    }

    private void initView() {
        lpMenu.x = 0;
        lpMenu.y = 0;
        lMenu.findViewById(R.id.pause).setVisibility(View.INVISIBLE);
        lMenu.findViewById(R.id.start).setVisibility(View.VISIBLE);

        setViewParams(vDisplay, screenW / 4, screenH / 4 * 3, screenW / 2, screenH / 4);
        syncParams(vDisplay, lpInteract);

        rDisplay.right = screenW - vDisplay.getLayoutParams().width;
        rDisplay.bottom = screenH - vDisplay.getLayoutParams().height;
    }

    private void setViewParams(View view, int x, int y, int width, int height) {
        view.setX(x);
        view.setY(y);
        view.getLayoutParams().width = width;
        view.getLayoutParams().height = height;
    }

    private void syncParams(View view, WindowManager.LayoutParams params) {
        params.x = (int) view.getX();
        params.y = (int) view.getY();
        params.width = view.getLayoutParams().width;
        params.height = view.getLayoutParams().height;
    }

    private class MenuEvents implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    lpMenu.x += (int) event.getRawX() - lastX;
                    lpMenu.y += (int) event.getRawY() - lastY;
                    wm.updateViewLayout(lMenu, lpMenu);
                case MotionEvent.ACTION_DOWN:
                    updateLastTouch((int) event.getRawX(), (int) event.getRawY());
                    break;
                case MotionEvent.ACTION_UP:
                    lpMenu.x = Math.max(0, lpMenu.x);
                    lpMenu.y = Math.max(0, lpMenu.y);
                    break;
            }
            return true;
        }
    }

    private class InteractEvents implements View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (isStarted) {
                vInteract.setVisibility(View.GONE);
                updateLastTouch((int) event.getRawX(), (int) event.getRawY());

                padding(lastX, lastY);
                click(lastX, lastY, TEST_CLICK);
                return true;
            }

            if (event.getAction() == MotionEvent.ACTION_MOVE)
                move((int) event.getRawX() - lastX, (int) event.getRawY() - lastY);
            interactEvent(event.getAction(), (int) event.getRawX(), (int) event.getRawY());
            return true;
        }
    }

    private void move(int diffX, int diffY) { moveX(diffX); moveY(diffY); }

    private void moveX(int diffX) {
        final int x = (int) vDisplay.getX() + diffX;
        if (x < rDisplay.left) vDisplay.setX(rDisplay.left);
        else vDisplay.setX(Math.min(x, rDisplay.right));
    }

    private void moveY(int diffY) {
        final int y = (int) vDisplay.getY() + diffY;
        if (y < rDisplay.top) vDisplay.setY(rDisplay.top);
        else vDisplay.setY(Math.min(y, rDisplay.bottom));
    }

    @SuppressLint("SetTextI18n")
    private void showLocation() {
        int x1 = (int) vDisplay.getX();
        int y1 = (int) vDisplay.getY();
        int x2 = x1 + vDisplay.getWidth();
        int y2 = y1 + vDisplay.getHeight();

        LT.setText(" "+x1+","+y1);
        LB.setText(" "+x1+","+y2);
        RT.setText(x2+","+y1+" ");
        RB.setText(x2+","+y2+" ");

        LT.setX(x1);
        LT.setY(y1);
        LB.setX(x1);
        LB.setY(y2 - LB.getHeight());
        RT.setX(x2 - RT.getWidth());
        RT.setY(y1);
        RB.setX(x2 - RB.getWidth());
        RB.setY(y2 - RB.getHeight());
    }

    private void start() {
        lMenu.findViewById(R.id.start).setVisibility(View.INVISIBLE);
        lMenu.findViewById(R.id.pause).setVisibility(View.VISIBLE);
        isStarted = true;
    }

    private void pause() {
        lMenu.findViewById(R.id.pause).setVisibility(View.INVISIBLE);
        lMenu.findViewById(R.id.start).setVisibility(View.VISIBLE);
        vInteract.setVisibility(View.VISIBLE);
        isStarted = false;

        vEdgeT.getLayoutParams().height = edge;
        vEdgeB.getLayoutParams().height = edge;
        vEdgeL.getLayoutParams().width = edge;
        vEdgeR.getLayoutParams().width = edge;
    }

    private void close() {
        isWindowCreated = false;
        wm.removeView(lInteract);
        wm.removeView(lDisplay);
        wm.removeView(lMenu);
    }

    private void updateLastTouch(int x, int y) { lastX = x; lastY = y;}

    private int count = 0;
    private void click(int x, int y, int mCount) {
        final Path path = new Path();
        path.moveTo(x, y);

        if (count < mCount) {
            dispatchGesture(new GestureDescription.Builder().
                    addStroke(new GestureDescription.StrokeDescription(path, 0, 100)).build(),new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    count++;
                }
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                }}, null);
        }
        else count = 0;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override
    public void onDestroy() { super.onDestroy(); }
    @Override
    public void onInterrupt() {}
}
