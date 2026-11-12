
package com.spiderai.v20;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.Random;

public class SpiderService extends Service {

    private WindowManager manager;
    private SpiderView spider;
    private WindowManager.LayoutParams params;
    private final Handler handler = new Handler();
    private final Random random = new Random();

    private boolean running = false;
    private boolean dragging = false;

    private float vx = 1.8f;
    private float vy = 0.7f;
    private long lastChange;
    private long pauseUntil;
    private long lastFrame;

    private int screenWidth;
    private int screenHeight;

    private final Runnable movement = new Runnable() {
        @Override
        public void run() {
            if (!running || spider == null) return;

            long now = System.currentTimeMillis();

            float dt = lastFrame == 0 ? 0.016f :
                    Math.min(0.04f, (now - lastFrame) / 1000f);

            lastFrame = now;

            if (!dragging && now >= pauseUntil) {

                if (now - lastChange > 2500 + random.nextInt(2500)) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    float speed = 1.0f + random.nextFloat() * 1.3f;

                    vx = (float) Math.cos(angle) * speed;
                    vy = (float) Math.sin(angle) * speed;

                    lastChange = now;

                    if (random.nextFloat() < 0.15f) {
                        pauseUntil = now + 700;
                    }
                }

                params.x += Math.round(vx * 60 * dt);
                params.y += Math.round(vy * 60 * dt);

                int maxX = Math.max(0, screenWidth - spider.getWidth());
                int maxY = Math.max(0, screenHeight - spider.getHeight());

                if (params.x <= 0 || params.x >= maxX) {
                    vx = -vx;
                    params.x = Math.max(0, Math.min(params.x, maxX));
                }

                if (params.y <= 0 || params.y >= maxY) {
                    vy = -vy;
                    params.y = Math.max(0, Math.min(params.y, maxY));
                }

                try {
                    manager.updateViewLayout(spider, params);
                } catch (Exception e) {
                    stopSelf();
                    return;
                }
            }

            spider.setWalking(!dragging && now >= pauseUntil);
            spider.invalidate();

            handler.postDelayed(this, 16);
        }
    };

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        manager = (WindowManager) getSystemService(WINDOW_SERVICE);

        android.util.DisplayMetrics metrics =
                new android.util.DisplayMetrics();

        if (Build.VERSION.SDK_INT >= 17) {
            manager.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            manager.getDefaultDisplay().getMetrics(metrics);
        }

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        spider = new SpiderView();

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                dp(76),
                dp(76),
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = screenWidth / 3;
        params.y = screenHeight / 3;

        spider.setOnTouchListener(new View.OnTouchListener() {

            private int startX, startY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        dragging = true;
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
                                params.x, screenWidth - spider.getWidth()));

                        params.y = Math.max(0, Math.min(
                                params.y, screenHeight - spider.getHeight()));

                        manager.updateViewLayout(spider, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        dragging = false;
                        lastChange = System.currentTimeMillis();
                        return true;
                }

                return false;
            }
        });

        manager.addView(spider, params);

        running = true;
        handler.post(movement);
    }

    private class SpiderView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float phase = 0;
        private boolean walking = true;

        SpiderView() {
            super(SpiderService.this);
        }

        void setWalking(boolean value) {
            walking = value;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            if (walking) phase += 0.18f;

            float bob = walking
                    ? (float)Math.sin(phase * 2) * dp(1.5f)
                    : 0;

            canvas.save();
            canvas.translate(0, bob);

            paint.setColor(Color.rgb(35, 25, 43));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2.7f));
            paint.setStrokeCap(Paint.Cap.ROUND);

            // Eight animated legs
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 4; i++) {

                    float baseY = cy + (i - 1.5f) * dp(5);

                    float swing = walking
                            ? (float)Math.sin(phase + i * 1.5f
                                    + (side == 1 ? Math.PI : 0)) * dp(5)
                            : 0;

                    Path leg = new Path();

                    leg.moveTo(cx + side * dp(8), baseY);

                    leg.quadTo(
                            cx + side * dp(20),
                            baseY - dp(8) + swing,
                            cx + side * dp(27),
                            baseY - dp(15) + swing
                    );

                    leg.lineTo(
                            cx + side * dp(33),
                            baseY - dp(21) + swing
                    );

                    canvas.drawPath(leg, paint);
                }
            }

            // Abdomen
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(45, 32, 55));

            canvas.drawOval(
                    cx - dp(15), cy - dp(1),
                    cx + dp(15), cy + dp(19), paint
            );

            // Body
            canvas.drawCircle(cx, cy - dp(10), dp(10), paint);

            // Head
            canvas.drawCircle(cx, cy - dp(22), dp(7), paint);

            // Red body markings
            paint.setColor(Color.rgb(210, 45, 55));
            canvas.drawCircle(cx, cy + dp(7), dp(3), paint);

            // Eyes
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx - dp(2.5f), cy - dp(23), dp(1.2f), paint);
            canvas.drawCircle(cx + dp(2.5f), cy - dp(23), dp(1.2f), paint);

            canvas.restore();
        }
    }

    @Override
    public void onDestroy() {

        running = false;
        handler.removeCallbacks(movement);

        if (spider != null && manager != null) {
            try {
                manager.removeView(spider);
            } catch (Exception ignored) { }
        }

        spider = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
