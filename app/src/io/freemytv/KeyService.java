package io.freemytv;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Prototype: the remote's settings button. It arrives as KEYCODE_NOTIFICATION, and SystemUI hands it only to
 * launcherx, which Free My TV mode turns off. Accessibility sees keys before the system does, so this tries to
 * catch it first and opens the TV's settings instead.
 */
public class KeyService extends AccessibilityService {
    @Override protected void onServiceConnected() {
        AppLog.w("KeyService connected");
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_NOTIFICATION) return false;
        AppLog.w("KeyService saw the settings button, action " + event.getAction());
        if (event.getAction() == KeyEvent.ACTION_UP) {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        return true;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override public void onInterrupt() {}
}
