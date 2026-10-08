package org.fedorahosted.freeotp.shortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.main.share.ShareRoute;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Private shortcut configurations. Launcher intents contain only an opaque configuration ID. */
public final class AccountShortcuts {
    private static final Gson GSON = new Gson();
    private static final String SCHEME = "freeotp-shortcut";

    private AccountShortcuts() {}

    public static final class Config {
        public String account;
        public ShareRoute destination;
        // Null means resolve the current default at invocation time.
        public String computer;

        public Config(String account, ShareRoute destination, String computer) {
            this.account = account;
            this.destination = destination;
            this.computer = computer;
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("account_shortcuts", Context.MODE_PRIVATE);
    }

    public static String save(Context context, Config config) {
        String id = UUID.randomUUID().toString();
        if (!preferences(context).edit().putString(id, GSON.toJson(config)).commit())
            throw new IllegalStateException("Unable to save shortcut");
        return id;
    }

    public static Intent intent(Context context, String id) {
        return new Intent(context, ShortcutActivity.class).setAction(Intent.ACTION_VIEW)
                .setData(new Uri.Builder().scheme(SCHEME).authority("send").appendPath(id).build());
    }

    public static Config load(Context context, Intent intent) {
        Uri uri = intent.getData();
        if (!Intent.ACTION_VIEW.equals(intent.getAction()) || uri == null
                || !SCHEME.equals(uri.getScheme()) || !"send".equals(uri.getAuthority())
                || uri.getPathSegments().size() != 1) return null;
        try {
            Config config = GSON.fromJson(preferences(context).getString(uri.getLastPathSegment(), ""), Config.class);
            if (config == null || config.account == null || config.destination == null
                    || config.destination == ShareRoute.NONE || config.destination == ShareRoute.CHOOSER)
                return null;
            return config;
        } catch (JsonParseException e) { return null; }
    }

    public static void remove(Context context, String id) {
        preferences(context).edit().remove(id).apply();
    }

    public static void removeAccount(Context context, String account) {
        List<String> ids = new ArrayList<>();
        SharedPreferences prefs = preferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        for (String id : prefs.getAll().keySet()) {
            Config config = load(context, intent(context, id));
            if (config != null && account.equals(config.account)) {
                ids.add(id);
                editor.remove(id);
            }
        }
        editor.apply();
        if (Build.VERSION.SDK_INT >= 26 && !ids.isEmpty()) {
            ShortcutManager manager = context.getSystemService(ShortcutManager.class);
            if (manager != null) manager.disableShortcuts(ids, context.getString(R.string.shortcut_account_missing));
        }
    }
}
