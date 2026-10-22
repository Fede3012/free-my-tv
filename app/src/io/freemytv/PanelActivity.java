package io.freemytv;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The remote's settings button. The stock home screen answers it with a panel, but in Free My TV mode that home screen
 * is off. This one sends each entry straight to the TV settings page the stock panel opens (picture, sound...), and
 * falls back to the main settings if the TV refuses.
 */
public class PanelActivity extends Activity {
    private static final String TV_SETTINGS = "com.android.tv.settings";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.init(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Free My TV");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, pad / 2);
        box.addView(title);

        Button first = entry(box, Ui.tr("Imagem", "Picture", "Imagen"), tvSettings("android.settings.DISPLAY_SETTINGS", ".partnercustomizer.picture.PictureActivity"));
        entry(box, Ui.tr("Som", "Sound", "Sonido"), tvSettings("com.android.tv.action.VIEW_SOUND_SETTINGS", ".device.sound.SoundActivity"));
        entry(box, "Wi-Fi", new Intent(Settings.ACTION_WIFI_SETTINGS));
        entry(box, "Bluetooth", new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        entry(box, Ui.tr("Todas as configurações", "All settings", "Todos los ajustes"), new Intent(Settings.ACTION_SETTINGS));

        Button original = button(box, Ui.tr("Voltar ao modo Original", "Back to Original mode", "Volver al modo Original"));
        original.setOnClickListener(v -> {
            HomeMode.request(this, HomeMode.ORIGINAL);
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });

        setContentView(box);
        first.requestFocus();
    }

    private static Intent tvSettings(String action, String activity) {
        return new Intent(action).setComponent(new ComponentName(TV_SETTINGS, TV_SETTINGS + activity));
    }

    private Button entry(LinearLayout box, String text, Intent intent) {
        Button b = button(box, text);
        b.setOnClickListener(v -> open(intent));
        return b;
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

    private Button button(LinearLayout box, String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(20);
        b.setFocusable(true);
        b.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(b);
        return b;
    }
}
