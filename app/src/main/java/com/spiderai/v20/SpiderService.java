
package com.spiderai.v20;

import android.app.Service;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.WindowManager;

import java.util.Random;

public class SpiderService extends Service {

    private WindowManager wm;
    private WindowManager.LayoutParams lp;
    private SpiderView spider;
    private Handler handler = new Handler();
    private Random random = new Random();

    private int sw, sh;
    private float vx = 1.5f, vy = 0.5f;
    private float phase = 0;
    private boolean walking = true;
    private boolean dragging = false;
    private long lastTime;
    private long directionTime;
    private long pauseUntil;

    private int dp(float v) {
        return (int)(v * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        android.util.DisplayMetrics dm =
                new android.util.DisplayMetrics();

        wm.getDefaultDisplay().getRealMetrics(dm);
        sw = dm.widthPixels;
        sh = dm.heightPixels;

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(
                dp(100), dp(100), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.x = sw / 3;
        lp.y = sh / 3;

        spider = new SpiderView();
        wm.addView(spider, lp);

        spider.setOnTouchListener(new View.OnTouchListener() {
            float tx, ty;
            int sx, sy;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dragging = true;
                        sx = lp.x;
                        sy = lp.y;
                        tx = e.getRawX();
                        ty = e.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        lp.x = sx + (int)(e.getRawX() - tx);
                        lp.y = sy + (int)(e.getRawY() - ty);

                        lp.x = Math.max(0, Math.min(
                                lp.x, sw - spider.getWidth()));
                        lp.y = Math.max(0, Math.min(
                                lp.y, sh - spider.getHeight()));

                        wm.updateViewLayout(spider, lp);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        dragging = false;
                        directionTime = System.currentTimeMillis();
                        return true;
                }
                return false;
            }
        });

        handler.post(frame);
    }

    private Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (spider == null) return;

            long now = System.currentTimeMillis();
            float dt = lastTime == 0 ? 0.016f :
                    Math.min(0.04f, (now-lastTime)/1000f);
            lastTime = now;

            if (!dragging) {
                if (now > pauseUntil) {
                    walking = true;

                    if (now - directionTime > 3000) {
                        double a = random.nextDouble()
                                * Math.PI * 2;
                        float speed = 1.0f
                                + random.nextFloat() * 1.3f;

                        vx = (float)Math.cos(a) * speed;
                        vy = (float)Math.sin(a) * speed;

                        directionTime = now;

                        if (random.nextFloat() < 0.15f) {
                            pauseUntil = now + 900;
                        }
                    }

                    if (now < pauseUntil) walking = false;

                    lp.x += (int)(vx * 60 * dt);
                    lp.y += (int)(vy * 60 * dt);

                    if (lp.x < 0 || lp.x > sw-dp(100)) {
                        vx = -vx;
                        lp.x = Math.max(0,
                                Math.min(lp.x, sw-dp(100)));
                    }

                    if (lp.y < 0 || lp.y > sh-dp(100)) {
                        vy = -vy;
                        lp.y = Math.max(0,
                                Math.min(lp.y, sh-dp(100)));
                    }

                    try {
                        wm.updateViewLayout(spider, lp);
                    } catch (Exception ignored) { }
                } else {
                    walking = false;
                }
            }

            spider.invalidate();
            handler.postDelayed(this, 16);
        }
    };

    private class SpiderView extends View {

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float s;

        SpiderView() {
            super(SpiderService.this);
            s = getResources().getDisplayMetrics().density;
        }

        private void line(Canvas c, float x1, float y1,
                          float x2, float y2, int color,
                          float width) {
            p.setColor(color);
            p.setStrokeWidth(width*s);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStyle(Paint.Style.STROKE);
            c.drawLine(x1,y1,x2,y2,p);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);

            float cx = getWidth()/2f;
            float cy = getHeight()/2f;

            if (walking) phase += 0.14f;

            float bob = walking
                    ? (float)Math.sin(phase*2)*2*s : 0;

            c.save();
            c.translate(0,bob);

            int black = Color.rgb(35,29,42);
            int red = Color.rgb(210,42,55);

            // Eight articulated legs
            for (int side=-1; side<=1; side+=2) {
                for (int i=0; i<4; i++) {

                    float baseY = cy + (i-1.5f)*5*s;

                    float swing = walking
                            ? (float)Math.sin(phase
                            + i*Math.PI/2
                            + (side==1 ? Math.PI : 0))*7*s
                            : 0;

                    float kneeX = cx + side*22*s;
                    float kneeY = baseY - 8*s + swing;
                    float footX = cx + side*32*s;
                    float footY = baseY - 14*s + swing;

                    // Upper leg
                    line(c,cx+side*10*s,baseY,
                            kneeX,kneeY,black,3);

                    // Lower leg
                    line(c,cx+side*10*s,baseY,
                            kneeX,kneeY,red,1.2f);

                    line(c,cx+side*22*s,kneeY,
                            footX,footY,black,2.5f);

                    // Joint
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(red);
                    c.drawCircle(kneeX,kneeY,2.5f*s,p);
                    c.drawCircle(footX,footY,1.8f*s,p);
                }
            }

            // Rear abdomen with soft 3D shading
            p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(
                    cx-5*s,cy+12*s,25*s,
                    new int[]{
                        Color.rgb(105,90,112),
                        black,
                        Color.rgb(15,12,20)
                    },
                    null,Shader.TileMode.CLAMP
            ));

            c.drawOval(cx-17*s,cy-1*s,
                    cx+17*s,cy+28*s,p);
            p.setShader(null);

            // Red abdomen marking
            p.setColor(red);
            c.drawOval(cx-5*s,cy+8*s,
                    cx+5*s,cy+17*s,p);

            // Head
            p.setShader(new RadialGradient(
                    cx-5*s,cy-14*s,20*s,
                    new int[]{
                        Color.rgb(120,110,130),
                        black,
                        Color.BLACK
                    },
                    null,Shader.TileMode.CLAMP
            ));

            c.drawCircle(cx,cy-11*s,14*s,p);
            p.setShader(null);

            // Big glossy eyes
            for (int side=-1;side<=1;side+=2) {
                float ex = cx+side*6*s;
                float ey = cy-13*s;

                p.setColor(Color.rgb(105,30,42));
                c.drawCircle(ex,ey,6.5f*s,p);

                p.setColor(Color.rgb(15,18,28));
                c.drawCircle(ex,ey,5*s,p);

                p.setColor(Color.WHITE);
                c.drawCircle(ex-side*1.5f*s,
                        ey-2*s,1.8f*s,p);
            }

            // Tiny mouth
            p.setColor(Color.rgb(230,180,180));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.2f*s);
            c.drawArc(cx-3*s,cy-4*s,
                    cx+3*s,cy+2*s,15,150,
                    false,p);

            c.restore();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(frame);

        if (spider != null && wm != null) {
            try {
                wm.removeView(spider);
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
                
