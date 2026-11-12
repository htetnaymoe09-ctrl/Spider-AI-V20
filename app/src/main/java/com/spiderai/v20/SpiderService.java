
package com.spiderai.v20;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.view.MotionEvent;

public class SpiderService extends Service {

    private WindowManager manager;
    private TextView spider;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        manager = (WindowManager)
            getSystemService(WINDOW_SERVICE);

        spider = new TextView(this);
        spider.setText("🕷️");
        spider.setTextSize(45);

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                130,
                130,
                WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams
                    .FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 300;

        spider.setOnTouchListener(new android.view.View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(
                android.view.View v,
                MotionEvent event
            ) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
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
                        manager.updateViewLayout(spider, params);
                        return true;
                }
                return false;
            }
        });

        manager.addView(spider, params);
    }

    @Override
    public void onDestroy() {
        if (spider != null && manager != null) {
            manager.removeView(spider);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
