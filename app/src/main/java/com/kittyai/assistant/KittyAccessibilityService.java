package com.kittyai.assistant;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

/** Optional user-enabled accessibility bridge. It only exposes safe navigation actions. */
public class KittyAccessibilityService extends AccessibilityService {
    private static volatile KittyAccessibilityService instance;
    @Override public void onServiceConnected() { super.onServiceConnected(); instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() { if (instance == this) instance = null; super.onDestroy(); }
    public static boolean perform(String action) {
        KittyAccessibilityService s = instance;
        if (s == null) return false;
        if ("back".equals(action)) return s.performGlobalAction(GLOBAL_ACTION_BACK);
        if ("home".equals(action)) return s.performGlobalAction(GLOBAL_ACTION_HOME);
        if ("notifications".equals(action)) return s.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        if ("recent".equals(action)) return s.performGlobalAction(GLOBAL_ACTION_RECENTS);
        return false;
    }
}
