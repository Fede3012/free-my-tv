package io.freemytv;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The remote's settings button. The stock home screen answers it with a panel, but in Free My TV mode that home screen
 * is off. This one sits on the right, in the look of the settings (white focused row, Inter), and sends each
 * entry straight to the TV settings page the stock panel opens (picture, sound...), falling back to the main settings
 * if the TV refuses.
 */
public class PanelActivity extends Activity {
    private static final String TV_SETTINGS = "com.android.tv.settings";
    private static final int INK = 0xFF1C1C1E;

    private float k;          // 1 at 1080p
    private LinearLayout list;
    private View first;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        k = getResources().getDisplayMetrics().heightPixels / 1080f;

        FrameLayout root = new FrameLayout(this);
        GradientDrawable scrim = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {0x00000000, 0xB3000000});
        root.setBackground(scrim);
        root.setOnClickListener(v -> finish());

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xF21C1C1E);
        panel.setPadding(px(44), px(56), px(44), px(44));
        panel.setClickable(true);
        root.addView(panel, new FrameLayout.LayoutParams(px(620), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END));

        TextView title = label("Free My TV", Ui.displaySemibold, 44, 0xFFFFFFFF);
        title.setPadding(px(30), 0, 0, px(36));
        panel.addView(title);

        list = panel;
        row(Ui.tr("Imagem", "Picture", "Imagen"), tvSettings("android.settings.DISPLAY_SETTINGS", ".partnercustomizer.picture.PictureActivity"));
        row(Ui.tr("Som", "Sound", "Sonido"), tvSettings("com.android.tv.action.VIEW_SOUND_SETTINGS", ".device.sound.SoundActivity"));
        row("Wi-Fi", new Intent(Settings.ACTION_WIFI_SETTINGS));
        row("Bluetooth", new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        row(Ui.tr("Todas as configurações", "All settings", "Todos los ajustes"), new Intent(Settings.ACTION_SETTINGS));

        View space = new View(this);
        panel.addView(space, new LinearLayout.LayoutParams(1, 0, 1f));

        TextView original = row(Ui.tr("Voltar ao modo Original", "Back to Original mode", "Volver al modo Original"), null);
        original.setOnClickListener(v -> {
            HomeMode.request(this, HomeMode.ORIGINAL);
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });

        setContentView(root);
        first.requestFocus();
    }

    private int px(float v) {
        return Math.round(v * k);
    }

    private static Intent tvSettings(String action, String activity) {
        return new Intent(action).setComponent(new ComponentName(TV_SETTINGS, TV_SETTINGS + activity));
    }

    private TextView label(String text, android.graphics.Typeface face, float size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTypeface(face);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * k);
        t.setTextColor(color);
        t.setSingleLine();
        return t;
    }

    /** A list row: no background; focused, white with dark text and a small lift (the Apple TV focus). */
    private TextView row(String text, Intent intent) {
        TextView t = label(text, Ui.medium, 30, 0xF2FFFFFF);
        t.setTextColor(new ColorStateList(new int[][] {{android.R.attr.state_focused}, {}}, new int[] {INK, 0xF2FFFFFF}));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(px(30), 0, px(30), 0);
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0xFFF5F5F7);
        focused.setCornerRadius(px(18));
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[] {android.R.attr.state_focused}, focused);
        t.setBackground(bg);
        t.setFocusable(true);
        t.setClickable(true);
        t.setOnFocusChangeListener((v, has) -> v.animate().scaleX(has ? 1.03f : 1f).scaleY(has ? 1.03f : 1f).setDuration(120).start());
        if (intent != null) t.setOnClickListener(v -> open(intent));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(84));
        lp.bottomMargin = px(10);
        list.addView(t, lp);
        if (first == null) first = t;
        return t;
    }

    private void open(Intent intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            AppLog.w("panel: " + intent.getAction() + " refused (" + e.getClass().getSimpleName() + "), opening settings");
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        finish();
    }
}
