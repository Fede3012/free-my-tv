package io.freemytv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The remote's settings button. The stock home screen answers it (a floating panel), but in Free My TV mode that
 * home screen is off, so the button would do nothing. This small panel takes its place: the TV's settings, or back
 * to the original mode.
 */
public class PanelActivity extends Activity {
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

        Button tv = button(Ui.tr("Configurações da TV", "TV settings", "Ajustes de la TV"));
        tv.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
        box.addView(tv);

        Button original = button(Ui.tr("Voltar ao modo Original", "Back to Original mode", "Volver al modo Original"));
        original.setOnClickListener(v -> {
            HomeMode.request(this, HomeMode.ORIGINAL);
            startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            finish();
        });
        box.addView(original);

        setContentView(box);
        tv.requestFocus();
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(20);
        b.setFocusable(true);
        b.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return b;
    }
}
