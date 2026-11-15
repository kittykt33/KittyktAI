package com.kittyai.assistant;

import android.app.*;
import android.content.*;
import android.os.*;
import android.speech.*;
import java.util.*;

public class WakeWordService extends Service {

    public static volatile boolean running = false;

    private SpeechRecognizer sr;
    private Handler handler;
    private boolean english = false;
    private boolean listening = false;
    private boolean openingApp = false;

    private static final String CHANNEL = "kitty_wake";
    private static final int NOTIFICATION_ID = 71;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        createChannel();

        Notification notification = new Notification.Builder(this, CHANNEL)
            .setContentTitle("Kitty AI")
            .setContentText("Hey Kitty - குரல் கட்டளைக்காகக் காத்திருக்கிறேன்")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build();

        startForeground(NOTIFICATION_ID, notification);
        running = true;

        handler.postDelayed(this::startListen, 500);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL,
                "Kitty Wake Word",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

            manager.createNotificationChannel(channel);
        }
    }

    private void startListen() {
        if (!running || openingApp || listening) return;

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            scheduleRetry(3000);
            return;
        }

        try {
            if (sr != null) {
                sr.destroy();
                sr = null;
            }

            sr = SpeechRecognizer.createSpeechRecognizer(this);

            sr.setRecognitionListener(new RecognitionListener() {

                @Override
                public void onReadyForSpeech(Bundle params) {
                    listening = true;
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    listening = false;
                }

                @Override
                public void onError(int error) {
                    listening = false;
                    english = !english;
                    scheduleRetry(1500);
                }

                @Override
                public void onResults(Bundle results) {
                    listening = false;

                    ArrayList<String> matches =
                        results.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        );

                    if (matches != null) {
                        for (String phrase : matches) {
                            String value = phrase.toLowerCase(Locale.ROOT);

                            if (value.contains("hey kitty") ||
                                value.contains("ஹே கிட்டி") ||
                                value.contains("ஹேய் கிட்டி") ||
                                value.contains("ஹே கிட்டி")) {

                                openKitty();
                                return;
                            }
                        }
                    }

                    english = !english;
                    scheduleRetry(500);
                }

                @Override
                public void onPartialResults(Bundle results) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            Intent intent = new Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                english ? "en-US" : "ta-LK"
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            );

            intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
            );

            sr.startListening(intent);

        } catch (Exception e) {
            listening = false;
            scheduleRetry(2000);
        }
    }

    private void scheduleRetry(long delay) {
        if (handler != null && running && !openingApp) {
            handler.removeCallbacksAndMessages(null);
            handler.postDelayed(this::startListen, delay);
        }
    }

    private void openKitty() {
        if (openingApp) return;

        openingApp = true;
        listening = false;

        try {
            if (sr != null) {
                sr.cancel();
                sr.destroy();
                sr = null;
            }

            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            );

            intent.putExtra("kitty_wake_triggered", true);
            startActivity(intent);

            stopSelf();

        } catch (Exception e) {
            openingApp = false;
            scheduleRetry(2000);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        listening = false;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (sr != null) {
            sr.cancel();
            sr.destroy();
            sr = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
