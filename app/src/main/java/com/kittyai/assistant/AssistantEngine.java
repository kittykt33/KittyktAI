package com.kittyai.assistant;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.provider.AlarmClock;
import android.provider.ContactsContract;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

public class AssistantEngine {

    private final Context c;
    private final MemoryStore memory;

    public interface Callback {
        void reply(String text);
    }

    public AssistantEngine(Context c, MemoryStore m) {
        this.c = c;
        this.memory = m;
    }

    public String answer(String raw) {
        String q = raw == null ? "" : raw.trim();
        String s = q.toLowerCase(Locale.ROOT);

        if (q.isEmpty()) return "என்ன உதவி வேண்டும்?";

        // Greeting
        if (s.contains("வணக்கம்") || s.contains("hello") || s.equals("hi"))
            return "வணக்கம்! சொல்லுங்கள். என்ன உதவி வேண்டும்?";

        // Time
        if (s.contains("நேரம்") || s.contains("time")) {
            return "இப்போது " +
                new SimpleDateFormat("h:mm a", Locale.getDefault())
                .format(new Date());
        }

        // Date
        if (s.contains("தேதி") || s.contains("date") || s.contains("இன்று என்ன கிழமை")) {
            return new SimpleDateFormat("EEEE, dd MMMM yyyy",
                new Locale("ta", "LK")).format(new Date());
        }

        // Calculator
        if (isCalculation(s)) {
            String result = calculate(q);
            if (result != null) return "பதில்: " + result;
        }

        // Save memory
        if (s.startsWith("நினைவில் வை") ||
            s.startsWith("remember ") ||
            s.startsWith("ஞாபகம் வை")) {

            String value = q.replaceFirst("(?i)^நினைவில் வை", "")
                .replaceFirst("(?i)^remember", "")
                .replaceFirst("^ஞாபகம் வை", "").trim();

            if (value.isEmpty()) return "எதை நினைவில் வைக்க வேண்டும்?";

            memory.put("note_" + System.currentTimeMillis(), value);
            return "சரி. இதை நினைவில் வைத்துக்கொண்டேன்: " + value;
        }

        // Show memories
        if (s.contains("என் நினைவுகள்") ||
            s.contains("நினைவுகளை காட்டு") ||
            s.contains("show memory")) {

            StringBuilder b = new StringBuilder("உங்கள் நினைவுகள்:\n");
            for (Map.Entry<String, ?> e : memory.all().entrySet()) {
                b.append("• ").append(e.getValue()).append("\n");
            }
            return b.toString();
        }

        // Alarm
        if (s.contains("அலாரம்") || s.contains("alarm")) {
            return setAlarm(q);
        }

        // Contact calling
        if (s.contains("call") || s.contains("கால்") ||
            s.contains("அழை") || s.contains("போன் பண்ணு")) {
            return callContact(q);
        }

        // Web search
        if (s.startsWith("தேடு ") || s.startsWith("search ") ||
            s.contains("google")) {
            String term = q.replaceFirst("(?i)^தேடு", "")
                .replaceFirst("(?i)^search", "").trim();

            try {
                Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
                i.putExtra("query", term.isEmpty() ? q : term);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return "இணையத் தேடலைத் திறக்கிறேன்.";
            } catch (Exception e) {
                return "இணையத் தேடலைத் திறக்க முடியவில்லை.";
            }
        }

        // Torch ON
        if (s.contains("flashlight on") || s.contains("torch on") ||
            s.contains("டார்ச் ஆன்") || s.contains("டார்ச் போடு")) {
            return torch(true);
        }

        // Torch OFF
        if (s.contains("flashlight off") || s.contains("torch off") ||
            s.contains("டார்ச் ஆஃப்") || s.contains("டார்ச் அணை")) {
            return torch(false);
        }

        // Volume
        if (s.contains("volume up") || s.contains("சத்தம் கூட்டு")) {
            AudioManager a = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
            a.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
            return "சத்தத்தை அதிகரித்தேன்.";
        }

        if (s.contains("volume down") || s.contains("சத்தம் குறை")) {
            AudioManager a = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
            a.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
            return "சத்தத்தைக் குறைத்தேன்.";
        }

        // Home
        if (s.equals("home") || s.contains("முகப்புக்குச் செல்")) {
            return KittyAccessibilityService.perform("home")
                ? "Home திரைக்குச் செல்கிறேன்."
                : "Phone Control Accessibility-ஐ Settings-ல் Enable செய்ய வேண்டும்.";
        }

        // Back
        if (s.equals("back") || s.contains("பின்னால் செல்")) {
            return KittyAccessibilityService.perform("back")
                ? "பின்னால் செல்கிறேன்."
                : "Accessibility அனுமதி தேவை.";
        }

        // Notifications
        if (s.contains("notifications") || s.contains("அறிவிப்புகள் காட்டு")) {
            return KittyAccessibilityService.perform("notifications")
                ? "அறிவிப்புகளைத் திறக்கிறேன்."
                : "Accessibility அனுமதி தேவை.";
        }

        // Settings
        if (s.contains("settings") || s.contains("அமைப்புகள்")) {
            try {
                Intent i = new Intent(Settings.ACTION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return "Settings திறக்கிறேன்.";
            } catch (Exception e) {
                return "Settings திறக்க முடியவில்லை.";
            }
        }

        return "இந்தக் கட்டளையை இன்னும் கற்றுக்கொடுக்க வேண்டும். நீங்கள் சொன்னது: " + q;
    }

    // ---------------- CALCULATOR ----------------

    private boolean isCalculation(String s) {
        return s.matches(".*[0-9].*") ||
            s.contains("கூட்டி") || s.contains("கூட்டு") ||
            s.contains("கழி") || s.contains("பெருக்கு") ||
            s.contains("வகுத்து") || s.contains("multiply") ||
            s.contains("plus") || s.contains("minus") ||
            s.contains("times") || s.contains("divided");
    }

    private String calculate(String input) {
        String x = input.toLowerCase(Locale.ROOT);

        String[][] words = {
            {"பதினைஞ்சு","15"}, {"பதினைந்து","15"},
            {"இருபது","20"}, {"முப்பது","30"},
            {"நாற்பது","40"}, {"ஐம்பது","50"},
            {"அறுபது","60"}, {"எழுபது","70"},
            {"எண்பது","80"}, {"தொண்ணூறு","90"},
            {"நூறு","100"}, {"பத்து","10"},
            {"ஒன்பது","9"}, {"எட்டு","8"},
            {"ஏழு","7"}, {"ஆறு","6"},
            {"ஐந்து","5"}, {"நான்கு","4"},
            {"மூன்று","3"}, {"இரண்டு","2"},
            {"ஒன்று","1"}, {"ஒரு","1"},
            {"zero","0"}, {"one","1"}, {"two","2"},
            {"three","3"}, {"four","4"}, {"five","5"},
            {"six","6"}, {"seven","7"}, {"eight","8"},
            {"nine","9"}, {"ten","10"}, {"fifteen","15"},
            {"twenty","20"}, {"thirty","30"}, {"forty","40"}
        };

        for (String[] w : words) {
            x = x.replace(w[0], " " + w[1] + " ");
        }

        x = x.replace("கூட்டி", "+")
             .replace("கூட்டு", "+")
             .replace("சேர்", "+")
             .replace("plus", "+")
             .replace("add", "+")
             .replace("கழித்து", "-")
             .replace("கழி", "-")
             .replace("minus", "-")
             .replace("subtract", "-")
             .replace("பெருக்கு", "*")
             .replace("பெருக்கி", "*")
             .replace("multiply", "*")
             .replace("times", "*")
             .replace("வகுத்து", "/")
             .replace("வகு", "/")
             .replace("divide", "/")
             .replace(",", "+");

        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)|([+*/-])").matcher(x);
        ArrayList<String> tokens = new ArrayList<>();

        while (m.find()) tokens.add(m.group());

        if (tokens.size() < 3) return null;

        try {
            double result = Double.parseDouble(tokens.get(0));

            for (int i = 1; i + 1 < tokens.size(); i += 2) {
                String op = tokens.get(i);
                double n = Double.parseDouble(tokens.get(i + 1));

                if (op.equals("+")) result += n;
                else if (op.equals("-")) result -= n;
                else if (op.equals("*")) result *= n;
                else if (op.equals("/")) {
                    if (n == 0) return "பூஜ்ஜியத்தால் வகுக்க முடியாது.";
                    result /= n;
                }
            }

            if (result == Math.floor(result))
                return String.valueOf((long)result);

            return String.valueOf(result);

        } catch (Exception e) {
            return null;
        }
    }

    // ---------------- ALARM ----------------

    private String setAlarm(String q) {
        Matcher m = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?").matcher(q);

        if (!m.find()) return "எத்தனை மணிக்கு அலாரம் வைக்க வேண்டும்?";

        try {
            int hour = Integer.parseInt(m.group(1));
            int minute = m.group(2) == null ? 0 :
                Integer.parseInt(m.group(2));

            if (q.contains("மாலை") || q.contains("இரவு") ||
                q.toLowerCase(Locale.ROOT).contains("pm")) {
                if (hour < 12) hour += 12;
            } else if (q.contains("காலை") ||
                       q.toLowerCase(Locale.ROOT).contains("am")) {
                if (hour == 12) hour = 0;
            }

            if (hour > 23 || minute > 59)
                return "சரியான நேரத்தைக் கூறுங்கள்.";

            Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
            i.putExtra(AlarmClock.EXTRA_HOUR, hour);
            i.putExtra(AlarmClock.EXTRA_MINUTES, minute);
            i.putExtra(AlarmClock.EXTRA_MESSAGE, "Kitty AI Alarm");
            i.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);

            return "Clock App திறக்கிறேன். அலாரத்தை உறுதிப்படுத்துங்கள்.";

        } catch (Exception e) {
            return "அலாரம் அமைக்க முடியவில்லை. Clock App இருக்கிறதா என்று பாருங்கள்.";
        }
    }

    // ---------------- CONTACT CALL ----------------

    private String callContact(String q) {
        if (ContextCompat.checkSelfPermission(c,
            Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return "Contacts அனுமதி தேவை. App Settings-ல் Contacts அனுமதி கொடுங்கள்.";
        }

        String name = q.replaceAll("(?i)call|கால்|அழை|போன் பண்ணு|க்கு", " ")
            .replace("அம்மாவை", "அம்மா")
            .replace("அப்பாவை", "அப்பா")
            .trim();

        if (name.isEmpty()) return "யாருக்கு Call செய்ய வேண்டும்?";

        try {
            android.database.Cursor cursor = c.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[] {
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null, null, null
            );

            if (cursor == null) return "Contacts திறக்க முடியவில்லை.";

            String number = null;
            int matches = 0;

            while (cursor.moveToNext()) {
                String contactName = cursor.getString(0);
                String contactNumber = cursor.getString(1);

                if (contactName != null &&
                    contactName.toLowerCase(Locale.ROOT)
                    .contains(name.toLowerCase(Locale.ROOT))) {
                    number = contactNumber;
                    matches++;
                }
            }
            cursor.close();

            if (matches == 0) return name + " என்ற பெயரில் Contact கிடைக்கவில்லை.";
            if (matches > 1) return "இந்தப் பெயரில் ஒன்றுக்கு மேற்பட்ட Contacts உள்ளன. முழுப் பெயரைச் சொல்லுங்கள்.";

            Intent i = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + Uri.encode(number)));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);

            return name + " Contact-ஐ Dialer-ல் திறந்துவிட்டேன். Call பொத்தானை அழுத்துங்கள்.";

        } catch (Exception e) {
            return "Contact தேடும்போது பிழை ஏற்பட்டது.";
        }
    }

    // ---------------- TORCH ----------------

    private String torch(boolean on) {
        try {
            CameraManager cm =
                (CameraManager)c.getSystemService(Context.CAMERA_SERVICE);

            for (String id : cm.getCameraIdList()) {
                android.hardware.camera2.CameraCharacteristics ch =
                    cm.getCameraCharacteristics(id);

                Boolean flash = ch.get(
                    android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);

                Integer facing = ch.get(
                    android.hardware.camera2.CameraCharacteristics.LENS_FACING);

                if (Boolean.TRUE.equals(flash) && facing != null &&
                    facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                    cm.setTorchMode(id, on);
                    return on ? "டார்ச் இயக்கப்பட்டது." : "டார்ச் அணைக்கப்பட்டது.";
                }
            }

            return "Flashlight கிடைக்கவில்லை.";

        } catch (Exception e) {
            return "டார்ச் இயக்க முடியவில்லை. Camera அனுமதியைச் சரிபார்க்கவும்.";
        }
    }
}
