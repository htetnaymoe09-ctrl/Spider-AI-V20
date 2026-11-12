
package com.spiderai.v20;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class SpiderService extends Service {

    private WindowManager manager;
    private TextView spider;
    private WindowManager.LayoutParams params;

    private Handler handler = new Handler();
    private boolean moving = true;

    private int dx = 5;
    private int dy = 4;

    private int screenWidth;
    private int screenHeight;

    private final Runnable movement = new Runnable() {
        @Override
        public void run() {

            if (!moving || spider == null) return;

            params.x += dx;
            params.y += dy;

            int maxX = screenWidth - spider.getWidth();
            int maxY = screenHeight - spider.getHeight();

            if (params.x <= 0 || params.x >= maxX) {
                dx = -dx;
            }

            if (params.y <= 0 || params.y >= maxY) {
                dy = -dy;
            }

            params.x = Math.max(0, Math.min(params.x, maxX));
            params.y = Math.max(0, Math.min(params.y, maxY));

            try {
                manager.updateViewLayout(spider, params);
            } catch (Exception e) {
                stopSelf();
                return;
            }

            handler.postDelayed(this, 30);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if (!android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        manager = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        android.util.DisplayMetrics metrics =
                new android.util.DisplayMetrics();

        if (Build.VERSION.SDK_INT >= 17) {
            manager.getDefaultDisplay().getRealMetrics(metrics);
        }

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        spider = new TextView(this);
        spider.setText("🕷️");
        spider.setTextSize(38);
        spider.setTextColor(Color.BLACK);
        spider.setGravity(Gravity.CENTER);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 300;

        spider.setOnTouchListener(new View.OnTouchListener() {

            private int startX, startY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        moving = false;
                        handler.removeCallbacks(movement);

                        startX = params.x;
                        startY = params.y;

                        touchX = event.getRawX();
                        touchY = event.getRawY();

                        return true;

                    case MotionEvent.ACTION_MOVE:

                        params.x = startX +
                                (int)(event.getRawX() - touchX);

                        params.y = startY +
                                (int)(event.getRawY() - touchY);

                        params.x = Math.max(0, Math.min(
                                params.x,
                                screenWidth - spider.getWidth()
                        ));

                        params.y = Math.max(0, Math.min(
                                params.y,
                                screenHeight - spider.getHeight()
                        ));

                        manager.updateViewLayout(spider, params);

                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:

                        moving = true;
                        handler.postDelayed(movement, 30);

                        return true;
                }

                return false;
            }
        });

        manager.addView(spider, params);

        handler.post(movement);
    }

    @Override
    public void onDestroy() {

        moving = false;
        handler.removeCallbacks(movement);

        if (spider != null && manager != null) {
            try {
                manager.removeView(spider);
            } catch (Exception e) {
                // View already removed
            }
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
