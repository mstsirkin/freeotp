package org.fedorahosted.freeotp.main.share;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.keyboard.KeyboardActivity;

public final class SharingSettingsActivity extends Activity {
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(20),dp(20),dp(20));
        body.setOnApplyWindowInsetsListener((view,insets)->{ body.setPadding(dp(20),dp(20)+insets.getSystemWindowInsetTop(),dp(20),dp(20)+insets.getSystemWindowInsetBottom()); return insets; });
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        Button back=new Button(this); back.setText("‹"); back.setContentDescription(getString(R.string.share_settings_back)); back.setOnClickListener(view->finish()); header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView title=new TextView(this); title.setText(R.string.share_settings_title); title.setTextSize(22); header.addView(title); body.addView(header);
        Switch clipboard=new Switch(this); clipboard.setText(R.string.share_settings_clipboard); clipboard.setTextSize(17); clipboard.setPadding(0,dp(16),0,dp(16)); clipboard.setChecked(SharingSettings.clipboardEnabled(this)); body.addView(clipboard);
        clipboard.setOnCheckedChangeListener((button,enabled)->SharingSettings.preferences(this).edit().putBoolean("clipboard",enabled).apply());
        Switch jelling=new Switch(this); jelling.setText(R.string.share_settings_jelling); jelling.setTextSize(17); jelling.setPadding(0,dp(16),0,dp(16)); jelling.setChecked(SharingSettings.jellingEnabled(this)); body.addView(jelling);
        Switch keyboard=new Switch(this); keyboard.setText(R.string.share_settings_keyboard); keyboard.setTextSize(17); keyboard.setPadding(0,dp(16),0,dp(16)); keyboard.setChecked(SharingSettings.keyboardEnabled(this)); keyboard.setEnabled(Build.VERSION.SDK_INT>=28); body.addView(keyboard);
        Button manage=new Button(this); manage.setText(R.string.share_settings_manage); manage.setAllCaps(false); manage.setEnabled(SharingSettings.keyboardEnabled(this)); manage.setOnClickListener(view->startActivity(new Intent(this,KeyboardActivity.class))); body.addView(manage);
        jelling.setOnCheckedChangeListener((button,enabled)->SharingSettings.preferences(this).edit().putBoolean("jelling",enabled).apply());
        keyboard.setOnCheckedChangeListener((button,enabled)->{ SharingSettings.preferences(this).edit().putBoolean("keyboard",enabled).apply(); manage.setEnabled(enabled); });
        if(Build.VERSION.SDK_INT<28) { TextView note=new TextView(this); note.setText(R.string.share_keyboard_unavailable); body.addView(note); }
        ScrollView scroll=new ScrollView(this); scroll.addView(body); setContentView(scroll);
    }
}
