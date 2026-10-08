package org.fedorahosted.freeotp.shortcuts;

import android.Manifest;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.UserNotAuthenticatedException;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.fedorahosted.freeotp.Code;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.keyboard.KeyboardActivity;
import org.fedorahosted.freeotp.main.Adapter;
import org.fedorahosted.freeotp.main.share.ShareActions;
import org.fedorahosted.freeotp.main.share.ShareFragment;
import org.fedorahosted.freeotp.main.share.ShareRoute;
import org.fedorahosted.freeotp.main.share.SharingSettings;

import java.io.IOException;
import java.security.GeneralSecurityException;

/** A launcher shortcut generates once per invocation, never again on activity recreation. */
public class ShortcutActivity extends AppCompatActivity {
    private AccountShortcuts.Config config;
    private Adapter accounts;
    private TextView status;
    private Button manage;
    private boolean dispatched, authenticationAttempted;
    private String computer;

    private final ActivityResultLauncher<Intent> authentication = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) runShortcut();
                else { error(R.string.shortcut_auth_cancelled); }
            });
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) runShortcut(); else error(R.string.shortcut_bluetooth_permission);
            });
    private final ActivityResultLauncher<Intent> enableBluetooth = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) runShortcut(); else error(R.string.shortcut_bluetooth_off);
            });
    private final ActivityResultLauncher<Intent> keyboard = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> finish());
    private final ActivityResultLauncher<Intent> manageDevices = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> runShortcut());

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        body.setPadding(padding, padding, padding, padding);
        body.setOnApplyWindowInsetsListener((view, insets) -> {
            body.setPadding(padding, padding + insets.getSystemWindowInsetTop(), padding,
                    padding + insets.getSystemWindowInsetBottom());
            return insets;
        });
        status = new TextView(this);
        status.setText(R.string.shortcut_sending);
        body.addView(status);
        manage = new Button(this);
        manage.setText(R.string.share_settings_manage);
        manage.setVisibility(View.GONE);
        manage.setOnClickListener(v -> manageDevices.launch(new Intent(this, KeyboardActivity.class)));
        body.addView(manage);
        Button close = new Button(this);
        close.setText(R.string.close);
        close.setOnClickListener(v -> finish());
        body.addView(close);
        setContentView(body);

        config = AccountShortcuts.load(this, getIntent());
        if (config == null) { error(R.string.shortcut_account_missing); return; }
        try {
            accounts = new Adapter(this, selected -> {});
        } catch (GeneralSecurityException | IOException e) { error(R.string.main_error_message); return; }
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override public void onFragmentDetached(@NonNull FragmentManager fm, @NonNull Fragment fragment) {
                if (fragment instanceof ShareFragment && !isChangingConfigurations()) finish();
            }
        }, false);
        if (savedInstanceState != null) {
            dispatched = savedInstanceState.getBoolean("dispatched");
            authenticationAttempted = savedInstanceState.getBoolean("authenticationAttempted");
            // Activity result launchers restore pending authentication/permission/send results.
            // If there is no pending result, let the user invoke the shortcut again explicitly.
            status.setText(savedInstanceState.getCharSequence("status", getString(R.string.shortcut_not_repeated)));
            manage.setVisibility(savedInstanceState.getBoolean("manage") ? View.VISIBLE : View.GONE);
        } else runShortcut();
    }

    private void error(int message) { status.setText(message); }

    private void runShortcut() {
        if (dispatched || config == null || accounts == null) return;
        manage.setVisibility(View.GONE);
        int position = accounts.positionOf(config.account);
        if (position < 0) { error(R.string.shortcut_account_missing); return; }
        if (!SharingSettings.enabled(this, config.destination)) { error(R.string.share_destination_disabled); return; }
        if (config.destination == ShareRoute.KEYBOARD && !prepareKeyboard()) return;
        Token token = accounts.getTokenInfo(position);
        try {
            Code code = accounts.getCodeForShortcut(position);
            dispatched = true;
            if ("ERROR".equals(code.getCode()) || !code.isValid()) { error(R.string.main_error_message); return; }
            status.setText(R.string.shortcut_not_repeated);
            if (config.destination == ShareRoute.KEYBOARD) {
                keyboard.launch(new Intent(this, KeyboardActivity.class).setAction(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_TEXT, code.getCode()).putExtra("computer", computer)
                        .putExtra(KeyboardActivity.EXTRA_FIXED_DESTINATION, config.computer != null));
            } else {
                ShareActions.shareTo(this, code.getCode(), config.destination);
                if (config.destination == ShareRoute.CLIPBOARD) finish();
            }
        } catch (UserNotAuthenticatedException e) {
            if (authenticationAttempted) { error(R.string.shortcut_auth_failed); return; }
            KeyguardManager keyguard = getSystemService(KeyguardManager.class);
            Intent intent = keyguard == null ? null : keyguard.createConfirmDeviceCredentialIntent(token.getIssuer(), token.getLabel());
            if (intent == null) { error(R.string.shortcut_auth_failed); return; }
            authenticationAttempted = true;
            authentication.launch(intent);
        } catch (KeyPermanentlyInvalidatedException e) {
            error(R.string.main_invalidated_message);
        }
    }

    private boolean prepareKeyboard() {
        if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.BLUETOOTH_CONNECT);
            return false;
        }
        BluetoothManager manager = getSystemService(BluetoothManager.class);
        BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null) { error(R.string.shortcut_no_bluetooth); return false; }
        try {
            if (!adapter.isEnabled()) {
                enableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                return false;
            }
            computer = config.computer == null
                    ? getSharedPreferences("keyboard_destinations", MODE_PRIVATE).getString("default", null) : config.computer;
            if (computer == null) {
                error(R.string.shortcut_choose_default);
                manage.setVisibility(View.VISIBLE);
                return false;
            }
            if (!BluetoothAdapter.checkBluetoothAddress(computer)) { error(R.string.shortcut_device_missing); return false; }
            BluetoothDevice device = adapter.getRemoteDevice(computer);
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) { error(R.string.shortcut_device_missing); return false; }
            return true;
        } catch (SecurityException e) { error(R.string.shortcut_bluetooth_permission); return false; }
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("dispatched", dispatched);
        state.putBoolean("authenticationAttempted", authenticationAttempted);
        state.putCharSequence("status", status.getText());
        state.putBoolean("manage", manage.getVisibility() == View.VISIBLE);
    }

    @Override protected void onDestroy() {
        if (accounts != null) accounts.close();
        super.onDestroy();
    }
}
