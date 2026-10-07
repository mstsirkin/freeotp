package org.fedorahosted.freeotp.main.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public final class SharingSettings {
    public static SharedPreferences automationPreferences(Context context) { return context.getSharedPreferences("settings",Context.MODE_PRIVATE); }
    public static ShareRoute destination(Context context, String uuid) {
        return destination(automationPreferences(context), uuid);
    }
    public static ShareRoute destination(SharedPreferences preferences, String uuid) {
        try {
            ShareRoute route = ShareRoute.valueOf(preferences.getString("accountDestination:" + uuid, "NONE"));
            return route == ShareRoute.CHOOSER ? ShareRoute.NONE : route;
        } catch (IllegalArgumentException e) { return ShareRoute.NONE; }
    }
    public static boolean enabled(Context context, ShareRoute route) {
        switch (route) {
            case CLIPBOARD: return clipboardEnabled(context);
            case JELLING: return jellingEnabled(context);
            case KEYBOARD: return keyboardEnabled(context);
            default: return false;
        }
    }
    private SharingSettings() {}
    public static SharedPreferences preferences(Context context) { return context.getSharedPreferences("share_transports",Context.MODE_PRIVATE); }
    public static boolean clipboardEnabled(Context context) { return preferences(context).getBoolean("clipboard",true); }
    public static boolean jellingEnabled(Context context) { return preferences(context).getBoolean("jelling",true); }
    public static boolean keyboardEnabled(Context context) { return Build.VERSION.SDK_INT>=28 && preferences(context).getBoolean("keyboard",true); }
}
