package org.fedorahosted.freeotp.main.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public final class SharingSettings {
    public static final String AUTO_SHARE="autoShareEnabled";
    public static final String AUTO_SHARE_ACCOUNT="autoShareAccount";
    public static SharedPreferences automationPreferences(Context context) { return context.getSharedPreferences("settings",Context.MODE_PRIVATE); }
    public static boolean generationEnabled(Context context) { return automationPreferences(context).getBoolean("autoGenerateStartup",false); }
    public static boolean autoShareEnabled(Context context) { return automationPreferences(context).getBoolean(AUTO_SHARE,false); }
    private SharingSettings() {}
    public static SharedPreferences preferences(Context context) { return context.getSharedPreferences("share_transports",Context.MODE_PRIVATE); }
    public static boolean clipboardEnabled(Context context) { return preferences(context).getBoolean("clipboard",true); }
    public static boolean jellingEnabled(Context context) { return preferences(context).getBoolean("jelling",true); }
    public static boolean keyboardEnabled(Context context) { return Build.VERSION.SDK_INT>=28 && preferences(context).getBoolean("keyboard",true); }
}
