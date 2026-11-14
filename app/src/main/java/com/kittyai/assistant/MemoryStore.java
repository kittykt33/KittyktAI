package com.kittyai.assistant;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.util.Map;

/** Local encrypted key/value memory. Falls back to private app preferences if crypto init fails. */
public class MemoryStore {
    private final SharedPreferences prefs;
    public MemoryStore(Context c) {
        SharedPreferences p;
        try {
            MasterKey key = new MasterKey.Builder(c).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            p = EncryptedSharedPreferences.create(c, "kitty_secure_memory", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception e) { p = c.getSharedPreferences("kitty_memory_private", Context.MODE_PRIVATE); }
        prefs = p;
    }
    public void put(String k,String v){ prefs.edit().putString(k,v).apply(); }
    public String get(String k){ return prefs.getString(k, ""); }
    public Map<String,?> all(){ return prefs.getAll(); }
    public void delete(String k){ prefs.edit().remove(k).apply(); }
    public void clear(){ prefs.edit().clear().apply(); }
}
