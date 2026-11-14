package com.kittyai.assistant;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Offline-first command router. Online generative AI requires a configured compatible endpoint/key. */
public class AssistantEngine {
    public interface Callback { void reply(String text); }
    private final Context c; private final MemoryStore memory;
    public AssistantEngine(Context c, MemoryStore m){this.c=c;this.memory=m;}
    public String answer(String raw) {
        String q=raw==null?"":raw.trim(); String s=q.toLowerCase(Locale.ROOT);
        if(q.isEmpty()) return "என்ன உதவி வேண்டும்?";
        if(s.contains("hey kitty")) return "சொல்லுங்கள், கேட்கிறேன்.";
        if(s.contains("வணக்கம்")||s.contains("hello")||s.contains("hi")) return "வணக்கம்! நான் Kitty. உங்களுக்கு என்ன உதவி வேண்டும்?";
        if(s.contains("நேரம்")||s.contains("time")) return "இப்போது " + new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        if(s.contains("தேதி")||s.contains("date")) return new SimpleDateFormat("dd MMMM yyyy", new Locale("ta","LK")).format(new Date());
        if(s.startsWith("நினைவில் வை")||s.startsWith("remember ")) {
            String value=q.replaceFirst("(?i)^நினைவில் வை","").replaceFirst("(?i)^remember","").trim();
            if(value.isEmpty()) return "எதை நினைவில் வைக்க வேண்டும்?";
            String key="note_"+System.currentTimeMillis(); memory.put(key,value); return "சரி. இதை இந்த போனில் பாதுகாப்பாகச் சேமித்தேன்: "+value;
        }
        if(s.contains("என் நினைவுகள்")||s.contains("show memory")||s.contains("நினைவுகளை காட்டு")) {
            StringBuilder b=new StringBuilder("உங்கள் சேமித்த நினைவுகள்:\n");
            for(java.util.Map.Entry<String,?> e:memory.all().entrySet()) b.append("• ").append(e.getValue()).append("\n");
            return b.toString();
        }
        if(s.startsWith("தேடு ")||s.startsWith("search ")||s.contains("google")) {
            String term=q.replaceFirst("(?i)^தேடு","").replaceFirst("(?i)^search","").trim();
            try { Intent i=new Intent(Intent.ACTION_WEB_SEARCH); i.putExtra("query",term.isEmpty()?q:term); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); return "இணையத் தேடலைத் திறக்கிறேன்."; }
            catch(Exception e){ return "இந்த போனில் இணையத் தேடல் செயலி கிடைக்கவில்லை."; }
        }
        if(s.contains("flashlight on")||s.contains("torch on")||s.contains("டார்ச் ஆன்")||s.contains("டார்ச் போடு")) return torch(true);
        if(s.contains("flashlight off")||s.contains("torch off")||s.contains("டார்ச் ஆஃப்")||s.contains("டார்ச் அணை")) return torch(false);
        if(s.contains("volume up")||s.contains("சத்தம் கூட்டு")) { AudioManager a=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE); a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI); return "சத்தத்தை அதிகரித்தேன்."; }
        if(s.contains("volume down")||s.contains("சத்தம் குறை")) { AudioManager a=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE); a.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI); return "சத்தத்தைக் குறைத்தேன்."; }
        if(s.equals("home")||s.contains("முகப்புக்குச் செல்")) return KittyAccessibilityService.perform("home") ? "Home திரைக்குச் செல்கிறேன்." : "Phone Control Accessibility-ஐ Settings-ல் enable செய்ய வேண்டும்.";
        if(s.equals("back")||s.contains("பின்னால் செல்")) return KittyAccessibilityService.perform("back") ? "பின்னால் செல்கிறேன்." : "Phone Control Accessibility-ஐ Settings-ல் enable செய்ய வேண்டும்.";
        if(s.contains("notifications")||s.contains("அறிவிப்புகள் காட்டு")) return KittyAccessibilityService.perform("notifications") ? "அறிவிப்புகளைத் திறக்கிறேன்." : "Phone Control Accessibility-ஐ Settings-ல் enable செய்ய வேண்டும்.";
        if(s.contains("settings")||s.contains("அமைப்புகள்")) { try{ Intent i=new Intent(Settings.ACTION_SETTINGS); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); return "Phone Settings திறக்கிறேன்.";}catch(Exception e){} }
        return "இந்த கட்டளை Offline அடிப்படை உதவியாளருக்குப் புரியவில்லை. Online AI இணைப்பு அமைக்கப்பட்டால் விரிவான பதில் கிடைக்கும். நீங்கள் சொன்னது: "+q;
    }
    private String torch(boolean on) {
        try {
            CameraManager cm=(CameraManager)c.getSystemService(Context.CAMERA_SERVICE);
            String[] ids=cm.getCameraIdList();
            for(String id:ids) { android.hardware.camera2.CameraCharacteristics ch=cm.getCameraCharacteristics(id);
                Boolean flash=ch.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing=ch.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                if(Boolean.TRUE.equals(flash) && facing!=null && facing==android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) { cm.setTorchMode(id,on); return on?"டார்ச் இயக்கப்பட்டது.":"டார்ச் அணைக்கப்பட்டது."; }
            }
            return "இந்த போனில் Flashlight கிடைக்கவில்லை.";
        } catch(SecurityException e) { return "Camera அனுமதி தேவை. Kitty AI App Info-வில் Camera அனுமதி கொடு."; }
          catch(Exception e) { return "டார்ச் கட்டுப்பாட்டில் பிழை: "+e.getMessage(); }
    }
}
