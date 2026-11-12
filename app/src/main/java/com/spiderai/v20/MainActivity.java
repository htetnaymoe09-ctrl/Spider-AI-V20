package com.spiderai.v20;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.provider.MediaStore;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import java.util.ArrayList;
import java.util.Locale;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class MainActivity extends Activity {

    private TextView status;
    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private TextToSpeech tts;
private SharedPreferences routerPrefs;

private static final String PREFS_FILE_NAME = "SpiderAISettings";
private static final String OPENROUTER_KEY_NAME = "openrouter_api_key";

private static final String OPENROUTER_URL =
        "https://openrouter.ai/api/v1/chat/completions";

private static final String GEMINI_MODEL =
        "google/gemini-2.5-flash";
    private static final int AUDIO_REQUEST = 101;
    private static final int VOICE_REQUEST = 102;

    private int dp(float v) {
        return (int)(v * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable bg(int color, int radius) {

      GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
routerPrefs = getSharedPreferences(PREFS_FILE_NAME, MODE_PRIVATE);
        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                int lang = tts.setLanguage(
                        new Locale("my", "MM"));

                if (lang == TextToSpeech.LANG_MISSING_DATA
                        || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault());
                }
            }
        });

        createInterface();
    }

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(12));
        root.setBackgroundColor(Color.rgb(16, 18, 30));

        TextView title = new TextView(this);
        title.setText("🕷 SPIDER AI");
        title.setTextSize(27);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("Your personal assistant is ready");
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(8), 0, dp(12));
        root.addView(status);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);

        Button start = new Button(this);
        start.setText("Start Spider");
        controls.addView(start,
                new LinearLayout.LayoutParams(0,
                        dp(48), 1));

        Button stop = new Button(this);
        stop.setText("Stop Spider");
        controls.addView(stop,
                new LinearLayout.LayoutParams(0,
                        dp(48), 1));

        root.addView(controls);

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);

        Button chat = new Button(this);
        chat.setText("💬 Chat AI");
        chat.setAllCaps(false);
        modes.addView(chat,
                new LinearLayout.LayoutParams(0,
                        dp(50), 1));

        Button voice = new Button(this);
        voice.setText("🎙 Voice AI");
        voice.setAllCaps(false);
        modes.addView(voice,
                new LinearLayout.LayoutParams(0,
                        dp(50), 1));

        root.addView(modes);

        scroll = new ScrollView(this);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(12), 0, dp(12));

        scroll.addView(messages);

        root.addView(scroll,
                new LinearLayout.LayoutParams(
                        -1, 0, 1));

        addMessage("Hello! I am Spider AI. "
                + "How can I help you?", false);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setHint("Type your command...");
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(bg(
                Color.rgb(45, 48, 65), 20));

        bottom.addView(input,
                new LinearLayout.LayoutParams(0,
                        dp(52), 1));

        Button mic = new Button(this);
        mic.setText("🎙");
        bottom.addView(mic);

        Button send = new Button(this);
        send.setText("Send");
        send.setAllCaps(false);
        bottom.addView(send);

        root.addView(bottom);

        setContentView(root);

        start.setOnClickListener(v -> startSpider());

        stop.setOnClickListener(v -> {
            stopService(new Intent(
                    this, SpiderService.class));
            status.setText("Spider stopped");
        });

        chat.setOnClickListener(v -> {
            status.setText("Chat mode ready");
            input.requestFocus();
        });

        voice.setOnClickListener(v -> startVoiceInput());

        mic.setOnClickListener(v -> startVoiceInput());

        send.setOnClickListener(v ->
                sendMessage(input.getText().toString()));
    }

    private void addMessage(String text, boolean user) {

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(16);
        bubble.setTextColor(Color.WHITE);
        bubble.setPadding(dp(14), dp(11),
                dp(14), dp(11));
        bubble.setMaxWidth(dp(280));

        bubble.setBackground(bg(
                user ? Color.rgb(35, 105, 190)
                     : Color.rgb(48, 51, 68), 18));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -2, -2);

        params.gravity = user
                ? Gravity.RIGHT : Gravity.LEFT;

        params.setMargins(0, dp(5), 0, dp(5));

        messages.addView(bubble, params);

        scroll.post(() ->
                scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private void sendMessage(String message) {

        message = message.trim();

        if (message.isEmpty()) return;

        input.setText("");
        addMessage(message, true);

        String reply = handleCommand(message);

        if (reply == null) {
            reply = "Chat AI server is not connected yet. "
                    + "The voice and basic app command "
                    + "system is being prepared.";
        }

        addMessage(reply, false);
        status.setText("Command processed");

        if (tts != null) {
            tts.speak(reply, TextToSpeech.QUEUE_FLUSH,
                    null, "spider_reply");
        }
    }

    private String handleCommand(String message) {

        String q = message.toLowerCase(Locale.ROOT);

        if (q.contains("setting") &&
                (q.contains("open") || q.contains("ဖွင့်"))) {

            startActivity(new Intent(
                    Settings.ACTION_SETTINGS));

            return "Opening phone settings.";
        }

        if (q.contains("youtube") &&
                (q.contains("open") || q.contains("ဖွင့်"))) {

            return openApp("com.google.android.youtube",
                    "YouTube", "https://youtube.com");
        }

        if ((q.contains("chrome") ||
                q.contains("browser")) &&
                (q.contains("open") || q.contains("ဖွင့်"))) {

            return openApp("com.android.chrome",
                    "Browser", "https://google.com");
        }

        if (q.contains("camera") &&
                (q.contains("open") || q.contains("ဖွင့်"))) {

            Intent camera = new Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE);

            if (camera.resolveActivity(
                    getPackageManager()) != null) {
                startActivity(camera);
                return "Opening camera.";
            }

            return "Camera is not available.";
        }

        return null;
    }

    private String openApp(String packageName,
                           String name, String url) {

        Intent launch = getPackageManager()
                .getLaunchIntentForPackage(packageName);

        if (launch != null) {
            startActivity(launch);
            return "Opening " + name + ".";
        }

        try {
            startActivity(new Intent(
                    Intent.ACTION_VIEW, Uri.parse(url)));
            return "Opening " + name + " in browser.";
        } catch (Exception e) {
            return name + " is not available.";
        }
    }

    private void startVoiceInput() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                        Manifest.permission.RECORD_AUDIO
                    }, AUDIO_REQUEST);

            return;
        }

        try {
            Intent voice = new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            voice.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            voice.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    "my-MM");

            voice.putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak to Spider AI");

            voice.putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS, 1);

            status.setText("Listening...");

            startActivityForResult(voice, VOICE_REQUEST);

        } catch (Exception e) {
            status.setText("Voice recognition unavailable");
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
            int resultCode, Intent data) {

        super.onActivityResult(
                requestCode, resultCode, data);

        if (requestCode == VOICE_REQUEST
                && resultCode == RESULT_OK
                && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS);

            if (results != null && !results.isEmpty()) {
                sendMessage(results.get(0));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == AUDIO_REQUEST
                && grantResults.length > 0
                && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {

            startVoiceInput();
        } else if (requestCode == AUDIO_REQUEST) {
            status.setText("Microphone permission required");
        }
    }

    private void startSpider() {

        if (!Settings.canDrawOverlays(this)) {

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));

            startActivity(intent);
            status.setText("Allow display over other apps");

        } else {

            startService(new Intent(
                    this, SpiderService.class));

            status.setText("Spider started!");
        }
    }

    @Override
    protected void onDestroy() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }
                    }
