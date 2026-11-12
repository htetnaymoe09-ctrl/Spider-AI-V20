
package com.spiderai.v20;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 80, 40, 40);

        TextView title = new TextView(this);
        title.setText("Spider AI Version 20.0");
        title.setTextSize(25);
        layout.addView(title);

        TextView status = new TextView(this);
        status.setText("Spider AI is ready!");
        status.setTextSize(18);
        layout.addView(status);

        Button start = new Button(this);
        start.setText("Start Spider");
        layout.addView(start);

        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            } else {
                status.setText("Spider permission ready!");
            }
        });

        setContentView(layout);
    }
}
