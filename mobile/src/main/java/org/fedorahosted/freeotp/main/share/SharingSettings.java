package org.fedorahosted.freeotp.main.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public final class SharingSettings {
    private SharingSettings() {}
    public static SharedPreferences preferences(Context context) { return context.getSharedPreferences("share_transports",Context.MODE_PRIVATE); }
    public static boolean jellingEnabled(Context context) { return preferences(context).getBoolean("jelling",true); }
    public static boolean keyboardEnabled(Context context) { return Build.VERSION.SDK_INT>=28 && preferences(context).getBoolean("keyboard",true); }
}
