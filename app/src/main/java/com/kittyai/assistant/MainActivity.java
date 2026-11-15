package com.kittyai.assistant;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.widget.*;
import android.view.*;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.*;
import java.util.concurrent.Executor;

public class MainActivity extends androidx.fragment.app.FragmentActivity {

    private LinearLayout messages;
    private EditText input;
    private TextToSpeech tts;
    private SpeechRecognizer recognizer;
    private MemoryStore memory;
    private AssistantEngine engine;

    private static final int REQ = 44;
    private String pendingCommand = "";
    private boolean unlocked = false;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        memory = new MemoryStore(this);
        engine = new AssistantEngine(this, memory);

        buildUi();

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("ta", "LK"));
                if (r == TextToSpeech.LANG_MISSING_DATA ||
                    r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(new Locale("ta", "IN"));
                }
            }
        });

        requestPermissions(new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS
        }, REQ);

        add("Kitty", "வணக்கம்! நான் Kitty. உங்கள் கட்டளைகளைக் கேட்கத் தயாராக இருக்கிறேன்.");
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(1);
        page.setPadding(22, 28, 22, 18);
        page.setBackgroundColor(0xff10131b);

        TextView title = new TextView(this);
        title.setText("🐱 KITTY AI");
        title.setTextColor(-1);
        title.setTextSize(25);
        title.setPadding(0, 0, 0, 14);
        page.addView(title);

        ScrollView scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(1);
        scroll.addView(messages);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout row = new LinearLayout(this);
        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("தமிழில் அல்லது English-ல் எழுதுங்கள்");
        input.setTextColor(-1);
        input.setHintTextColor(0xffaab0c0);
        row.addView(input, new LinearLayout.LayoutParams(0, -2, 1));

        Button send = btn("அனுப்பு");
        send.setOnClickListener(v -> sendText());
        row.addView(send);
        page.addView(row);

        LinearLayout actions = new LinearLayout(this);

        Button mic = btn("🎙 பேசு");
        mic.setOnClickListener(v -> listen());
        actions.addView(mic, new LinearLayout.LayoutParams(0, -2, 1));

        Button wake = btn("Hey Kitty ON");
        wake.setOnClickListener(v -> toggleWake());
        actions.addView(wake, new LinearLayout.LayoutParams(0, -2, 1));

        Button lock = btn("பாதுகாப்பு");
        lock.setOnClickListener(v -> authenticate());
        actions.addView(lock, new LinearLayout.LayoutParams(0, -2, 1));

        page.addView(actions);

        LinearLayout tools = new LinearLayout(this);

        Button mem = btn("நினைவுகள்");
        mem.setOnClickListener(v -> reply(engine.answer("என் நினைவுகள் காட்டு")));
        tools.addView(mem, new LinearLayout.LayoutParams(0, -2, 1));

        Button clear = btn("நினைவுகளை அழி");
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("நினைவுகளை அழிக்கவா?")
            .setMessage("சேமித்த அனைத்து நினைவுகளும் அழிக்கப்படும்.")
            .setNegativeButton("வேண்டாம்", null)
            .setPositiveButton("அழி", (d, w) -> {
                memory.clear();
                add("Kitty", "நினைவுகள் அழிக்கப்பட்டன.");
            }).show());
        tools.addView(clear, new LinearLayout.LayoutParams(0, -2, 1));

        Button phone = btn("Phone Control");
        phone.setOnClickListener(v -> {
            try {
                startActivity(new Intent(
                    android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } catch (Exception e) {
                add("Kitty", "Accessibility Settings திறக்க முடியவில்லை.");
            }
        });
        tools.addView(phone, new LinearLayout.LayoutParams(0, -2, 1));

        page.addView(tools);
        setContentView(page);
    }

    private Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        return b;
    }

    private void sendText() {
        String q = input.getText().toString().trim();
        input.setText("");
        if (q.isEmpty()) return;

        add("நீங்கள்", q);

        String low = q.toLowerCase(Locale.ROOT);

        if ((low.contains("டார்ச்") || low.contains("torch") ||
             low.contains("flashlight")) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            pendingCommand = q;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ + 1);
            return;
        }

        if ((low.contains("call") || low.contains("கால்") ||
             low.contains("அழை") || low.contains("போன் பண்ணு")) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            pendingCommand = q;
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ + 2);
            return;
        }

        reply(engine.answer(q));
    }

    private void reply(String answer) {
        add("Kitty", answer);
        speak(answer);
    }

    private void add(String who, String text) {
        TextView t = new TextView(this);
        t.setText(who + "\n" + text);
        t.setTextColor(who.equals("Kitty") ? 0xffb9d7ff : 0xffffffff);
        t.setTextSize(16);
        t.setPadding(12, 12, 12, 12);
        messages.addView(t);
    }

    private void speak(String s) {
        if (tts != null)
            tts.speak(s, TextToSpeech.QUEUE_FLUSH, null, "kitty_reply");
    }

    private void listen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ);
            return;
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            add("Kitty", "இந்த போனில் Speech Recognition சேவை கிடைக்கவில்லை.");
            return;
        }

        try {
            if (recognizer != null) recognizer.destroy();

            recognizer = SpeechRecognizer.createSpeechRecognizer(this);

            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle p) {
                    add("Kitty", "கேட்கிறேன்...");
                }
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float r) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() {}

                public void onError(int e) {
                    add("Kitty", "குரலைப் புரிந்துகொள்ள முடியவில்லை. மீண்டும் முயற்சி செய்.");
                }

                public void onResults(Bundle b) {
                    ArrayList<String> x =
                        b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (x != null && !x.isEmpty()) {
                        input.setText(x.get(0));
                        sendText();
                    }
                }

                public void onPartialResults(Bundle b) {}
                public void onEvent(int e, Bundle b) {}
            });

            Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-LK");
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ta-LK");
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

            recognizer.startListening(i);

        } catch (Exception e) {
            add("Kitty", "Voice தொடங்க முடியவில்லை: " + e.getMessage());
        }
    }

    private void toggleWake() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ);
            return;
        }

        try {
            Intent i = new Intent(this, WakeWordService.class);

            if (WakeWordService.running) {
                stopService(i);
                add("Kitty", "Hey Kitty சேவை நிறுத்தப்பட்டது.");
            } else {
                if (Build.VERSION.SDK_INT >= 26)
                    startForegroundService(i);
                else
                    startService(i);

                add("Kitty", "Hey Kitty சேவையைத் தொடங்குகிறேன்.");
            }
        } catch (Exception e) {
            add("Kitty", "Wake Word தொடங்க முடியவில்லை: " + e.getMessage());
        }
    }

    private void authenticate() {
        Executor ex = ContextCompat.getMainExecutor(this);

        BiometricPrompt prompt = new BiometricPrompt(this, ex,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(
                    BiometricPrompt.AuthenticationResult r) {
                    unlocked = true;
                    add("Kitty", "அடையாளம் உறுதிப்படுத்தப்பட்டது.");
                }

                @Override
                public void onAuthenticationError(int c, CharSequence s) {
                    add("Kitty", "அங்கீகாரம் ரத்து செய்யப்பட்டது: " + s);
                }
            });

        BiometricPrompt.PromptInfo info =
            new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Kitty AI பாதுகாப்பு")
                .setSubtitle("Fingerprint அல்லது சாதன PIN மூலம் உறுதிப்படுத்தவும்")
                .setDeviceCredentialAllowed(true)
                .build();

        if (BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK |
            BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            == BiometricManager.BIOMETRIC_SUCCESS) {
            prompt.authenticate(info);
        } else {
            add("Kitty", "இந்த சாதனத்தில் PIN/Fingerprint அமைக்கப்படவில்லை.");
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if ((requestCode == REQ + 1 || requestCode == REQ + 2) &&
            grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
            !pendingCommand.isEmpty()) {

            String q = pendingCommand;
            pendingCommand = "";
            reply(engine.answer(q));

        } else if ((requestCode == REQ + 1 || requestCode == REQ + 2) &&
                   !pendingCommand.isEmpty()) {
            pendingCommand = "";
            add("Kitty", "தேவையான அனுமதி வழங்கப்படவில்லை.");
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        if (recognizer != null) recognizer.destroy();
        super.onDestroy();
    }
}
