package org.fedorahosted.freeotp.shortcuts;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.fedorahosted.freeotp.Code;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.main.Activity;
import org.fedorahosted.freeotp.main.Adapter;
import org.fedorahosted.freeotp.main.share.ShareRoute;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyStore;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AccountShortcutsTest {
    private Context context;
    private Adapter accounts;
    private String account;
    private int automaticShares;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String file : new String[]{"tokens", "tokenStore", "settings", "account_shortcuts", "share_transports"})
            context.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear().commit();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                accounts = new Adapter(context, selected -> {}) {
                    @Override public void onCodeGenerated(String uuid, Code code) { automaticShares++; }
                };
                android.util.Pair<javax.crypto.SecretKey, Token> token = Token.parseUnsafe(Uri.parse(
                        "otpauth://hotp/Work:test?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&counter=0&issuer=Work"));
                accounts.add(token.first, token.second);
                account = accounts.uuidAt(0);
            } catch (Exception e) { throw new AssertionError(e); }
        });
    }

    @After public void tearDown() throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> accounts.close());
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (account != null) store.deleteEntry(account);
    }

    private long counter() { return accounts.getTokenInfo(0).getCounter(); }
    private Intent shortcut(ShareRoute route) {
        return AccountShortcuts.intent(context, AccountShortcuts.save(context,
                new AccountShortcuts.Config(account, route, null)));
    }

    @Test public void intentContainsOnlyOpaqueIdAndKeepsIndependentDestinations() {
        String fixed = "AA:BB:CC:DD:EE:FF";
        String id = AccountShortcuts.save(context, new AccountShortcuts.Config(account, ShareRoute.KEYBOARD, fixed));
        Intent intent = AccountShortcuts.intent(context, id);
        assertNull(intent.getExtras());
        assertFalse(intent.toUri(0).contains(account));
        assertFalse(intent.toUri(0).contains(fixed));
        AccountShortcuts.Config config = AccountShortcuts.load(context, intent);
        assertEquals(account, config.account);
        assertEquals(ShareRoute.KEYBOARD, config.destination);
        assertEquals(fixed, config.computer);
        assertNull(AccountShortcuts.load(context, shortcut(ShareRoute.KEYBOARD)).computer);
        assertEquals(0, counter());
    }

    @Test public void unknownOrMalformedIntentsCannotChooseAnAccount() {
        Intent valid = shortcut(ShareRoute.CLIPBOARD);
        assertNull(AccountShortcuts.load(context, new Intent(valid).setAction(Intent.ACTION_SEND)));
        assertNull(AccountShortcuts.load(context, new Intent(valid).setData(Uri.parse("freeotp-shortcut://send/missing"))));
        assertNull(AccountShortcuts.load(context, new Intent(valid).setData(Uri.parse("https://send/" + account))));
        try (ActivityScenario<ShortcutActivity> scenario = ActivityScenario.launch(
                new Intent(valid).setData(Uri.parse("freeotp-shortcut://send/missing")))) {
            assertEquals(0, counter());
        }
    }

    @Test public void shortcutCodeDoesNotAlsoTriggerAccountAutomaticSharing() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putString("accountDestination:" + account, ShareRoute.JELLING.name()).commit();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try {
                assertNotEquals("ERROR", accounts.getCodeForShortcut(0).getCode());
                assertEquals(0, automaticShares);
                assertNotEquals("ERROR", accounts.getCode(0).getCode());
                assertEquals(1, automaticShares);
            } catch (Exception e) { throw new AssertionError(e); }
        });
        assertEquals(2, counter());
    }

    @Test public void clipboardInvocationGeneratesExactlyOneHotpCode() {
        try (ActivityScenario<ShortcutActivity> scenario = ActivityScenario.launch(shortcut(ShareRoute.CLIPBOARD))) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(1, counter());
        }
    }

    @Test public void disabledTransportDoesNotConsumeHotpEvenAfterRecreation() {
        context.getSharedPreferences("share_transports", Context.MODE_PRIVATE).edit().putBoolean("clipboard", false).commit();
        try (ActivityScenario<ShortcutActivity> scenario = ActivityScenario.launch(shortcut(ShareRoute.CLIPBOARD))) {
            assertEquals(0, counter());
            scenario.recreate();
            assertEquals(0, counter());
        }
    }

    @Test public void recreatingJellingSessionDoesNotGenerateAgain() {
        for (String permission : new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN})
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(context.getPackageName(), permission);
        android.bluetooth.BluetoothManager manager = context.getSystemService(android.bluetooth.BluetoothManager.class);
        org.junit.Assume.assumeTrue("Jelling requires an enabled Bluetooth adapter", manager != null
                && manager.getAdapter() != null && manager.getAdapter().isEnabled());
        try (ActivityScenario<ShortcutActivity> scenario = ActivityScenario.launch(shortcut(ShareRoute.JELLING))) {
            assertEquals(1, counter());
            scenario.recreate();
            assertEquals(1, counter());
        }
    }

    @Test public void deletingAccountInvalidatesItsShortcuts() {
        Intent intent = shortcut(ShareRoute.CLIPBOARD);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            try { accounts.delete(0); } catch (Exception e) { throw new AssertionError(e); }
        });
        assertNull(AccountShortcuts.load(context, intent));
    }

    @Test public void openingAppRetiresStartupSelectionWithoutGenerating() {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
                .putBoolean("startupAccount:" + account, true).putBoolean("autoGenerateStartup", true).commit();
        try (ActivityScenario<Activity> scenario = ActivityScenario.launch(Activity.class)) {
            scenario.onActivity(activity -> assertNotNull(activity.findViewById(R.id.create_shortcut)));
            assertEquals(0, counter());
            assertFalse(context.getSharedPreferences("settings", Context.MODE_PRIVATE).contains("startupAccount:" + account));
        }
    }

    @Test public void shortcutDialogRetainsCustomizationWithoutGenerating() {
        try (ActivityScenario<Activity> scenario = ActivityScenario.launch(Activity.class)) {
            scenario.onActivity(activity -> activity.findViewById(R.id.create_shortcut).performClick());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                ShortcutDialog dialog = (ShortcutDialog) activity.getSupportFragmentManager().findFragmentByTag("create_shortcut");
                android.view.View root = dialog.requireDialog().getWindow().getDecorView();
                ((EditText) root.findViewWithTag("shortcut_name")).setText("Work laptop");
                ((Spinner) root.findViewWithTag("shortcut_destination")).setSelection(1);
                ((Spinner) root.findViewWithTag("shortcut_color")).setSelection(2);
                ((Spinner) root.findViewWithTag("shortcut_symbol")).setSelection(5);
                ((EditText) root.findViewWithTag("shortcut_emoji")).setText("★");
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                ShortcutDialog dialog = (ShortcutDialog) activity.getSupportFragmentManager().findFragmentByTag("create_shortcut");
                android.view.View root = dialog.requireDialog().getWindow().getDecorView();
                assertEquals("Work laptop", ((EditText) root.findViewWithTag("shortcut_name")).getText().toString());
                assertEquals(1, ((Spinner) root.findViewWithTag("shortcut_destination")).getSelectedItemPosition());
                assertEquals(2, ((Spinner) root.findViewWithTag("shortcut_color")).getSelectedItemPosition());
                assertEquals(5, ((Spinner) root.findViewWithTag("shortcut_symbol")).getSelectedItemPosition());
                assertEquals("★", ((EditText) root.findViewWithTag("shortcut_emoji")).getText().toString());
            });
            assertEquals(0, counter());
        }
    }
}
